#!/usr/bin/env bash
# Boots the production stack as the server runs it, with throwaway values, and asks each of its
# names through Caddy (DECISION-080). CI runs it on every pull request; it needs Docker, curl and
# openssl, and ports 80 and 443 free.
#
#   infrastructure/production/smoke.sh                 # build every image and boot
#   SMOKE_OVERRIDE=extra.yml infrastructure/production/smoke.sh
#   SMOKE_LOAD=1 infrastructure/production/smoke.sh     # then the load test (DECISION-083)
#
# The domain is daliini.localhost: Caddy gives *.localhost names certificates from its own local
# authority, so the whole TLS path runs with no real domain and no ACME.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
work="$(mktemp -d)"
project="daliini-smoke"
domain="daliini.localhost"

random() { openssl rand -hex 32; }

# Every <...> in the example gets a value; secrets get fresh random ones.
sed -e "s|<ROOT_DOMAIN>|$domain|g" -e "s|<OPS_EMAIL>|ops@$domain|" -e "s|<GIT_SHA>|smoke|" \
  "$here/production.env.example" > "$work/smoke.env"
while IFS= read -r line; do
  key="${line%%=*}"
  case "$line" in
    *"=<random>") sed -i "s|^$key=.*|$key=$(random)|" "$work/smoke.env" ;;
  esac
done < "$work/smoke.env"
sed -i -e "s|<[A-Z_]*>|placeholder|g" \
  -e "s|^FCM_SERVICE_ACCOUNT_JSON=.*|FCM_SERVICE_ACCOUNT_JSON=e30=|" \
  -e "s|^MAP_DIR=.*|MAP_DIR=$work/map|" "$work/smoke.env"

# The map host with one tile of Raqqa instead of Syria (map/fixture.py), the style bound to this
# domain, and Valhalla's configuration without a graph: enough to prove each path through Caddy.
current="$work/map/current"
mkdir -p "$current/valhalla"
python3 "$here/map/fixture.py" "$current"
python3 "$here/map/bind-style.py" "https://maps.$domain" "$current/syria.pmtiles" "$current/styles/daliini.json"
docker run --rm ghcr.io/valhalla/valhalla:3.9.0 valhalla_build_config \
  --mjolnir-tile-extract /data/tiles.tar --logging-color false > "$current/valhalla/valhalla.json"
# Smoke only: the site and the console call the API through Caddy by its public name, and
# Caddy's local authority is not one Node trusts. On a real server the certificate is public.
cat > "$work/smoke.override.yml" <<'EOF'
services:
  web:
    environment:
      NODE_TLS_REJECT_UNAUTHORIZED: "0"
  admin:
    environment:
      NODE_TLS_REJECT_UNAUTHORIZED: "0"
EOF
# The load test comes from one address, which the per-address limits would throttle within
# seconds; in this throwaway stack, and only here, they are raised out of its way.
if [ "${SMOKE_LOAD:-}" = 1 ]; then
  cat >> "$work/smoke.override.yml" <<'EOF'
  api:
    environment:
      THROTTLE_ANON_DEFAULT: 1000000/minute
      THROTTLE_SEARCH: 1000000/minute
EOF
fi

compose() {
  docker compose -p "$project" -f "$here/compose.yml" -f "$work/smoke.override.yml" \
    ${SMOKE_OVERRIDE:+-f "$SMOKE_OVERRIDE"} --env-file "$work/smoke.env" "$@"
}

finish() {
  status=$?
  if [ "$status" -ne 0 ]; then
    compose ps -a || true
    compose logs --no-color --tail=60 || true
  fi
  compose down -v --remove-orphans >/dev/null 2>&1 || true
  rm -rf "$work"
  exit "$status"
}
trap finish EXIT

compose up -d --build

resolve=()
for name in "$domain" "www.$domain" "api.$domain" "admin.$domain" "maps.$domain"; do
  resolve+=(--resolve "$name:443:127.0.0.1" --resolve "$name:80:127.0.0.1")
done
ask() { curl -sk --noproxy '*' "${resolve[@]}" "$@"; }

# Up to ten minutes for migrations, the API and the two Next servers.
for _ in $(seq 1 120); do
  if [ "$(ask -o /dev/null -w '%{http_code}' "https://api.$domain/health/ready/")" = 200 ] &&
     [ "$(ask -o /dev/null -w '%{http_code}' "https://maps.$domain/health")" = 200 ] &&
     [ "$(ask -o /dev/null -w '%{http_code}' "https://$domain/")" = 200 ] &&
     [ "$(ask -o /dev/null -w '%{http_code}' "https://admin.$domain/login")" = 200 ]; then
    break
  fi
  sleep 5
done

failures=0
expect() {
  local url="$1" want="$2" got
  got="$(ask -o /dev/null -w '%{http_code}' "$url")"
  if [ "$got" = "$want" ]; then
    echo "ok    $want $url"
  else
    echo "FAIL  $got $url (expected $want)"
    failures=$((failures + 1))
  fi
}

expect "https://api.$domain/health/ready/" 200
expect "https://api.$domain/health/live/" 200
expect "https://$domain/" 200
expect "https://admin.$domain/login" 200
expect "https://www.$domain/duty" 301
expect "http://api.$domain/health/live/" 308

# The map host (DECISION-082): the style, a tile, Arabic glyphs in their joined forms, the icons,
# and the routing engine, which takes a route by POST and nothing by GET.
expect "https://maps.$domain/style/daliini" 200
expect "https://maps.$domain/syria/12/2491/1609" 200
expect "https://maps.$domain/font/IBM%20Plex%20Sans%20Arabic%20Regular/65024-65279" 200
expect "https://maps.$domain/sprite/daliini.json" 200
expect "https://maps.$domain/routing/status" 200
expect "https://maps.$domain/routing/route?json=%7B%7D" 404
expect "https://maps.$domain/routing/isochrone" 404
route="$(ask -o /dev/null -w '%{http_code}' -X POST --data '{"locations":[]}' "https://maps.$domain/routing/route")"
if [ "$route" = 400 ]; then
  echo "ok    400 POST https://maps.$domain/routing/route (reached Valhalla, which has no graph here)"
else
  echo "FAIL  $route POST https://maps.$domain/routing/route (expected Valhalla's 400)"; failures=$((failures + 1))
fi
if ask "https://maps.$domain/style/daliini" | grep -q "https://maps.$domain/syria/{z}/{x}/{y}"; then
  echo "ok    the style points at this host's tiles"
else
  echo "FAIL  the style does not point at this host's tiles"; failures=$((failures + 1))
fi
if ask -D - -o /dev/null "https://$domain/" | grep -i '^content-security-policy:' | grep -q "https://maps.$domain"; then
  echo "ok    the site's CSP lets the map host in"
else
  echo "FAIL  the site's CSP does not name https://maps.$domain"; failures=$((failures + 1))
fi

headers="$(ask -D - -o /dev/null "https://api.$domain/health/ready/")"
if grep -qi '^strict-transport-security: max-age=31536000' <<<"$headers"; then
  echo "ok    HSTS"
else
  echo "FAIL  no HSTS on the API"; failures=$((failures + 1))
fi
if grep -qi '^server:' <<<"$headers"; then
  echo "FAIL  the Server header is still sent"; failures=$((failures + 1))
else
  echo "ok    no Server header"
fi

# The site's server reads the API through Caddy: Raqqa's page carries its Arabic name only when
# that call succeeded (the province is seeded by a migration).
if ask "https://$domain/raqqa" | grep -q 'الرقة'; then
  echo "ok    the site read the API"
else
  echo "FAIL  the site could not read the API"; failures=$((failures + 1))
fi

if [ "$failures" -ne 0 ]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "production stack smoke PASS"

# The load test (DECISION-083): five thousand made-up facilities, then visitors through Caddy.
if [ "${SMOKE_LOAD:-}" = 1 ]; then
  compose exec -T api uv run python manage.py seed_load_directory --facilities 5000 --disposable
  docker run --rm --network host -v "$here/../load:/load:ro" grafana/k6:1.3.0 run --quiet \
    -e API="https://api.$domain" -e HOSTS="api.$domain=127.0.0.1" -e INSECURE=1 \
    -e VUS="${LOAD_VUS:-40}" -e HOLD="${LOAD_HOLD:-60s}" /load/public.js
  echo "load test PASS"
fi
