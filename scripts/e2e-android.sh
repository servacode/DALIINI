#!/usr/bin/env bash
# Drive the Android data layer, and the Android -> Admin -> Android hand-off, against a real stack.
#
# Nothing is mocked. This resets a PostGIS database, applies migrations (which seed the launch
# baseline), creates the Admin and mobile fixtures, starts MinIO with a public-media bucket that
# anyone may read and a private-evidence bucket that nobody may, and Django with its WebSocket
# endpoint. Then:
#   1. the connected suite in apps/android/jvm-verification;
#   2. the hand-off: the owner submits with evidence (JVM), an operator opens the evidence and
#      approves in the real Admin (Playwright against `next start`), and the facility appears in
#      public discovery with its photo (JVM);
#   3. storage isolation: a public photo is readable without credentials, private evidence is
#      not.
#
# What this does not do is build or run the Android app: that needs Google Maven, which does not
# answer this network. See apps/android/jvm-verification/README.md.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/apps/backend"
ADMIN="$ROOT/apps/admin"
HARNESS="$ROOT/apps/android/jvm-verification"
API_PORT="${E2E_API_PORT:-8000}"
ADMIN_PORT="${E2E_ADMIN_PORT:-3000}"
MINIO_PORT="${E2E_MINIO_PORT:-9000}"
PG="${E2E_PG_CONTAINER:-p10pg}"
REDIS_HOST="${E2E_REDIS_HOST:-p10redis}"
NETWORK="${E2E_NETWORK:-p10net}"
IMAGE="${E2E_BACKEND_IMAGE:-directory-v3-p2dev:local}"
MINIO_IMAGE="${E2E_MINIO_IMAGE:-minio/minio:RELEASE.2025-09-07T16-13-09Z}"
MC_IMAGE="${E2E_MC_IMAGE:-minio/mc:RELEASE.2025-08-13T08-35-41Z}"
GRADLEW="$ROOT/apps/android/gradlew"
# Development-only credentials, the same ones infrastructure/docker/compose.yml uses.
S3_USER="directory-dev"
S3_PASSWORD="directory-dev-only-change-me"
PUBLIC_MEDIA="http://127.0.0.1:$MINIO_PORT/directory-public"

stop_admin() {
  for pid in $(netstat -ano 2>/dev/null | grep ":$ADMIN_PORT" | grep LISTENING | awk '{print $5}' | sort -u); do
    taskkill /PID "$pid" /F /T >/dev/null 2>&1 || kill -9 "$pid" >/dev/null 2>&1 || true
  done
}

cleanup() {
  docker rm -f e2e-api e2e-minio >/dev/null 2>&1 || true
  stop_admin
}
# E2E_KEEP_STACK=1 leaves Django and MinIO running after the suite, for investigating a failure.
if [ "${E2E_KEEP_STACK:-0}" != "1" ]; then trap cleanup EXIT; fi
cleanup

mc() {
  docker run --rm --network "$NETWORK" --entrypoint sh "$MC_IMAGE" -c \
    "mc alias set e2e http://e2e-minio:9000 $S3_USER $S3_PASSWORD >/dev/null 2>&1 && $*"
}

failures=0
step() { # name, command...
  local name="$1"; shift
  if "$@"; then echo "PASS  $name"; else echo "FAIL  $name"; failures=$((failures + 1)); fi
}

echo "== 1. Reset the database and object storage =="
# Redis holds the login and OTP throttle history; a previous run would count against this one.
docker exec "$REDIS_HOST" redis-cli FLUSHALL >/dev/null
docker exec "$PG" psql -U directory -d postgres \
  -c "DROP DATABASE IF EXISTS directory WITH (FORCE);" \
  -c "CREATE DATABASE directory OWNER directory;" >/dev/null

docker run -d --name e2e-minio --network "$NETWORK" -p "$MINIO_PORT:9000" \
  -e MINIO_ROOT_USER="$S3_USER" -e MINIO_ROOT_PASSWORD="$S3_PASSWORD" \
  "$MINIO_IMAGE" server /data >/dev/null || { echo "FAIL: minio"; exit 1; }
ready=0
for _ in $(seq 1 30); do
  # Public media is readable by anyone; private evidence keeps the default: no anonymous access.
  if mc "mc mb --ignore-existing e2e/directory-public e2e/directory-private >/dev/null \
         && mc anonymous set download e2e/directory-public >/dev/null"; then
    ready=1
    echo "minio ready: directory-public anonymous download, directory-private private"
    break
  fi
  sleep 1
done
[ "$ready" = 1 ] || { echo "FAIL: minio buckets"; exit 1; }

MOUNTS=()
for entry in "$BACKEND"/*; do
  name="$(basename "$entry")"
  case "$name" in .venv|.pytest_cache|.ruff_cache) continue ;; esac
  MOUNTS+=(-v "$entry:/app/$name:ro")
done
ENVIRONMENT=(
  -e UV_NO_SYNC=1
  -e DATABASE_URL="postgresql://directory:directory@$PG:5432/directory"
  -e REDIS_URL="redis://$REDIS_HOST:6379/0"
  -e S3_ENDPOINT_URL="http://e2e-minio:9000"
  -e S3_REGION="us-east-1"
  -e S3_ACCESS_KEY_ID="$S3_USER"
  -e S3_SECRET_ACCESS_KEY="$S3_PASSWORD"
  -e S3_PUBLIC_BUCKET="directory-public"
  -e S3_PRIVATE_BUCKET="directory-private"
  -e S3_PUBLIC_MEDIA_BASE_URL="$PUBLIC_MEDIA"
)

# The Admin fixtures first: they clear every `e2e-` facility, the mobile ones included.
docker run --rm --network "$NETWORK" "${MOUNTS[@]}" "${ENVIRONMENT[@]}" \
  "$IMAGE" sh -c "uv run python manage.py migrate --noinput >/dev/null \
    && uv run python manage.py seed_e2e_fixtures \
    && uv run python manage.py seed_e2e_mobile_fixtures" \
  || { echo "FAIL: fixtures"; exit 1; }

echo
echo "== 2. Start Django (HTTP and WebSocket) =="
docker run -d --name e2e-api --network "$NETWORK" -p "$API_PORT:8000" "${MOUNTS[@]}" "${ENVIRONMENT[@]}" \
  -e ALLOWED_HOSTS="localhost,127.0.0.1,e2e-api" \
  "$IMAGE" sh -c "uv run python manage.py runserver 0.0.0.0:8000 --noreload" >/dev/null
ready=0
for _ in $(seq 1 40); do
  if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$API_PORT/api/v1/public/provinces/")" = "200" ]; then
    ready=1
    echo "django ready on $API_PORT"
    break
  fi
  sleep 1
done
[ "$ready" = 1 ] || { echo "FAIL: django did not start"; docker logs e2e-api 2>&1 | tail -20; exit 1; }

connected() { # extra gradle args
  (cd "$HARNESS" && DIRECTORY_API_BASE_URL="http://127.0.0.1:$API_PORT/" E2E_API_CONTAINER="e2e-api" \
    "$GRADLEW" -p "$HARNESS" --console=plain connectedCheck "$@")
}

handoff() { # phase
  HANDOFF_PHASE="$1" connected --tests '*HandoffConnectedTest*'
}

echo
echo "== 3. Connected suite =="
step "connected suite" connected

echo
echo "== 4. Hand-off: owner submits with evidence (JVM) =="
step "handoff submit" handoff submit

echo
echo "== 5. Hand-off: an operator opens the evidence and approves (Admin, production build) =="
cd "$ADMIN"
export ADMIN_API_ORIGIN="http://127.0.0.1:$API_PORT"
export ADMIN_PUBLIC_ORIGIN="http://localhost:$ADMIN_PORT"
stop_admin
./node_modules/.bin/next build >/dev/null 2>&1 || { echo "FAIL: next build"; exit 1; }
./node_modules/.bin/next start -p "$ADMIN_PORT" > "$ADMIN/.e2e-admin.log" 2>&1 &
for _ in $(seq 1 40); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$ADMIN_PORT/login")" = "200" ] && break
  sleep 1
done
step "handoff admin review" env E2E_HANDOFF=1 E2E_API_ORIGIN="http://127.0.0.1:$API_PORT" \
  ADMIN_E2E_URL="http://localhost:$ADMIN_PORT" ./node_modules/.bin/playwright test tests/e2e/handoff.spec.ts
cd "$ROOT"

echo
echo "== 6. Hand-off: the facility is public, with its photo (JVM) =="
step "handoff public" handoff public

echo
echo "== 7. Storage isolation =="
public_key="$(mc "mc ls --recursive e2e/directory-public" | awk '{print $NF}' | grep '\.jpg$' | head -1)"
private_key="$(mc "mc ls --recursive e2e/directory-private" | awk '{print $NF}' | grep '\.jpg$' | head -1)"
public_status="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$MINIO_PORT/directory-public/$public_key")"
private_status="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$MINIO_PORT/directory-private/$private_key")"
echo "public object without credentials: $public_status; private object without credentials: $private_status"
step "public media readable anonymously" test -n "$public_key" -a "$public_status" = "200"
step "private evidence refused anonymously" test -n "$private_key" -a "$private_status" = "403"

echo
echo "failures=$failures"
exit $((failures > 0))
