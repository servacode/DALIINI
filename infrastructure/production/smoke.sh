#!/usr/bin/env bash
# Boots the production stack as the server runs it, with throwaway values, and asks each of its
# names through Caddy (DECISION-080). CI runs it on every pull request; it needs Docker, curl and
# openssl, and ports 80 and 443 free.
#
#   infrastructure/production/smoke.sh                 # build every image and boot
#   SMOKE_OVERRIDE=extra.yml infrastructure/production/smoke.sh
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
  -e "s|^FCM_SERVICE_ACCOUNT_JSON=.*|FCM_SERVICE_ACCOUNT_JSON=e30=|" "$work/smoke.env"
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
for name in "$domain" "www.$domain" "api.$domain" "admin.$domain"; do
  resolve+=(--resolve "$name:443:127.0.0.1" --resolve "$name:80:127.0.0.1")
done
ask() { curl -sk --noproxy '*' "${resolve[@]}" "$@"; }

# Up to ten minutes for migrations, the API and the two Next servers.
for _ in $(seq 1 120); do
  if [ "$(ask -o /dev/null -w '%{http_code}' "https://api.$domain/health/ready/")" = 200 ] &&
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
