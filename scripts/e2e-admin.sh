#!/usr/bin/env bash
# Run the Admin golden path against a real stack.
#
# Nothing is mocked. This resets a PostGIS database, applies migrations (which seed the
# launch baseline), creates the deterministic test operators, starts Django, builds the
# Admin for production and serves it with `next start`, then runs Playwright against that.
#
# `next dev` is deliberately not used: the production build is what ships, and it is the one
# whose server-only boundaries, bundling and instrumentation are worth testing.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADMIN="$ROOT/apps/admin"
BACKEND="$ROOT/apps/backend"
API_PORT="${E2E_API_PORT:-8000}"
ADMIN_PORT="${E2E_ADMIN_PORT:-3000}"
PG="${E2E_PG_CONTAINER:-p10pg}"
REDIS_HOST="${E2E_REDIS_HOST:-p10redis}"
NETWORK="${E2E_NETWORK:-p10net}"
IMAGE="${E2E_BACKEND_IMAGE:-directory-v3-p2dev:local}"

cleanup() {
  docker rm -f e2e-api >/dev/null 2>&1 || true
  for pid in $(netstat -ano 2>/dev/null | grep ":$ADMIN_PORT" | grep LISTENING | awk '{print $5}' | sort -u); do
    taskkill //PID "$pid" //F >/dev/null 2>&1 || kill -9 "$pid" >/dev/null 2>&1 || true
  done
}
trap cleanup EXIT

echo "== 1. Reset the database and apply migrations =="
docker exec "$PG" psql -U directory -d postgres \
  -c "DROP DATABASE IF EXISTS directory WITH (FORCE);" \
  -c "CREATE DATABASE directory OWNER directory;" >/dev/null

MOUNTS=()
for entry in "$BACKEND"/*; do
  name="$(basename "$entry")"
  case "$name" in .venv|.pytest_cache|.ruff_cache) continue ;; esac
  MOUNTS+=(-v "$entry:/app/$name:ro")
done

docker run --rm --network "$NETWORK" "${MOUNTS[@]}" \
  -e UV_NO_SYNC=1 \
  -e DATABASE_URL="postgresql://directory:directory@$PG:5432/directory" \
  -e REDIS_URL="redis://$REDIS_HOST:6379/0" \
  "$IMAGE" sh -c "uv run python manage.py migrate --noinput >/dev/null && uv run python manage.py seed_e2e_fixtures" \
  || { echo "FAIL: fixtures"; exit 1; }

echo
echo "== 2. Start Django =="
cleanup
docker run -d --name e2e-api --network "$NETWORK" -p "$API_PORT:8000" "${MOUNTS[@]}" \
  -e UV_NO_SYNC=1 \
  -e ALLOWED_HOSTS="localhost,127.0.0.1,e2e-api" \
  -e DATABASE_URL="postgresql://directory:directory@$PG:5432/directory" \
  -e REDIS_URL="redis://$REDIS_HOST:6379/0" \
  "$IMAGE" sh -c "uv run python manage.py runserver 0.0.0.0:8000 --noreload" >/dev/null

for _ in $(seq 1 40); do
  if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$API_PORT/api/v1/public/provinces/")" = "200" ]; then
    echo "django ready on $API_PORT"
    break
  fi
  sleep 1
done

echo
echo "== 3. Build and serve the Admin, production mode =="
cd "$ADMIN"
export ADMIN_API_ORIGIN="http://127.0.0.1:$API_PORT"
export ADMIN_PUBLIC_ORIGIN="http://localhost:$ADMIN_PORT"
./node_modules/.bin/next build >/dev/null 2>&1 || { echo "FAIL: next build"; exit 1; }
./node_modules/.bin/next start -p "$ADMIN_PORT" > "$ADMIN/.e2e-admin.log" 2>&1 &

for _ in $(seq 1 40); do
  if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$ADMIN_PORT/login")" = "200" ]; then
    echo "admin ready on $ADMIN_PORT"
    break
  fi
  sleep 1
done

echo
echo "== 4. Playwright =="
E2E_API_ORIGIN="http://127.0.0.1:$API_PORT" \
ADMIN_E2E_URL="http://localhost:$ADMIN_PORT" \
  ./node_modules/.bin/playwright test "$@"
status=$?

echo
echo "playwright exit=$status"
exit $status
