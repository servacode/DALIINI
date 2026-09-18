#!/usr/bin/env bash
# Drive the Android data layer against a real stack.
#
# Nothing is mocked. This resets a PostGIS database, applies migrations (which seed the
# launch baseline), creates the mobile fixtures, starts MinIO with both buckets and Django with
# its WebSocket endpoint, then runs the connected suite in apps/android/jvm-verification: the
# generated Kotlin client, the adapters, the mappers, session coordination and the realtime
# stream, all as the app wires them.
#
# What this does not do is build or run the Android app: that needs Google Maven, which is
# not reachable from every network. See apps/android/jvm-verification/README.md.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/apps/backend"
HARNESS="$ROOT/apps/android/jvm-verification"
API_PORT="${E2E_API_PORT:-8000}"
PG="${E2E_PG_CONTAINER:-p10pg}"
REDIS_HOST="${E2E_REDIS_HOST:-p10redis}"
NETWORK="${E2E_NETWORK:-p10net}"
IMAGE="${E2E_BACKEND_IMAGE:-directory-v3-p2dev:local}"
MINIO_IMAGE="${E2E_MINIO_IMAGE:-minio/minio:RELEASE.2025-09-07T16-13-09Z}"
MC_IMAGE="${E2E_MC_IMAGE:-minio/mc:RELEASE.2025-08-13T08-35-41Z}"
GRADLE="${GRADLE:-gradle}"
# Development-only credentials, the same ones infrastructure/docker/compose.yml uses.
S3_USER="directory-dev"
S3_PASSWORD="directory-dev-only-change-me"

cleanup() {
  docker rm -f e2e-api e2e-minio >/dev/null 2>&1 || true
}
# E2E_KEEP_STACK=1 leaves Django and MinIO running after the suite, for investigating a failure.
if [ "${E2E_KEEP_STACK:-0}" != "1" ]; then trap cleanup EXIT; fi
cleanup

echo "== 1. Reset the database and object storage =="
# Redis holds the login and OTP throttle history; a previous run would count against this one.
docker exec "$REDIS_HOST" redis-cli FLUSHALL >/dev/null
docker exec "$PG" psql -U directory -d postgres \
  -c "DROP DATABASE IF EXISTS directory WITH (FORCE);" \
  -c "CREATE DATABASE directory OWNER directory;" >/dev/null

docker run -d --name e2e-minio --network "$NETWORK" \
  -e MINIO_ROOT_USER="$S3_USER" -e MINIO_ROOT_PASSWORD="$S3_PASSWORD" \
  "$MINIO_IMAGE" server /data >/dev/null || { echo "FAIL: minio"; exit 1; }
for _ in $(seq 1 30); do
  if docker run --rm --network "$NETWORK" --entrypoint sh "$MC_IMAGE" -c \
    "mc alias set e2e http://e2e-minio:9000 $S3_USER $S3_PASSWORD >/dev/null 2>&1 \
     && mc mb --ignore-existing e2e/directory-public e2e/directory-private >/dev/null"; then
    echo "minio ready with both buckets"
    break
  fi
  sleep 1
done

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
)

docker run --rm --network "$NETWORK" "${MOUNTS[@]}" "${ENVIRONMENT[@]}" \
  "$IMAGE" sh -c "uv run python manage.py migrate --noinput >/dev/null && uv run python manage.py seed_e2e_mobile_fixtures" \
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

echo
echo "== 3. Connected suite =="
cd "$HARNESS"
DIRECTORY_API_BASE_URL="http://127.0.0.1:$API_PORT/" \
E2E_API_CONTAINER="e2e-api" \
  "$GRADLE" --console=plain connectedCheck "$@"
status=$?

echo
echo "connected exit=$status"
exit $status
