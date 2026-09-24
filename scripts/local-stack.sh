#!/usr/bin/env bash
# The whole platform on this machine, for a session with a real phone. Nothing is mocked and no
# test runs: PostGIS, Redis, MinIO, Django (HTTP and WebSocket) and the Admin's production
# build, with the migrations (which seed the launch baseline) and the e2e fixtures.
#
#   scripts/local-stack.sh up       reset and start everything; the Admin stays in the
#                                   foreground and Ctrl+C stops it
#   scripts/local-stack.sh status   health of each service
#   scripts/local-stack.sh down     stop Django, MinIO and the Admin (PostGIS and Redis stay)
#
# A phone on USB reaches it through `adb reverse tcp:8000 tcp:8000` and
# `adb reverse tcp:9000 tcp:9000` (docs/runbooks/android-local-networking.md), so public media
# is addressed as http://localhost:9000/directory-public on the phone and on this machine
# alike. Private evidence has no public address. No production secret is involved: the
# storage credentials are the development ones compose.yml uses, and the accounts are the
# fixtures in seed_e2e_fixtures and seed_e2e_mobile_fixtures.
#
# The stack has its own database (directory_device), Redis database (1) and container names, so
# the e2e runners cannot reset it. It shares ports 8000, 9000 and 3000 with them: stop one
# before starting the other.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/apps/backend"
ADMIN="$ROOT/apps/admin"
PG="${LOCAL_PG_CONTAINER:-p10pg}"
REDIS="${LOCAL_REDIS_CONTAINER:-p10redis}"
NETWORK="${LOCAL_NETWORK:-p10net}"
IMAGE="${LOCAL_BACKEND_IMAGE:-directory-v3-p2dev:local}"
MINIO_IMAGE="minio/minio:RELEASE.2025-09-07T16-13-09Z"
MC_IMAGE="minio/mc:RELEASE.2025-08-13T08-35-41Z"
DB="directory_device"
API="local-api"
MINIO="local-minio"
S3_USER="directory-dev"
S3_PASSWORD="directory-dev-only-change-me"
PUBLIC_MEDIA="http://localhost:9000/directory-public"
STATE="$ROOT/.local-stack"

code() { curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$1"; }

wait_for() { # url, expected code, seconds
  for _ in $(seq 1 "$3"); do
    [ "$(code "$1")" = "$2" ] && return 0
    sleep 1
  done
  return 1
}

mc() {
  docker run --rm --network "$NETWORK" --entrypoint sh "$MC_IMAGE" -c \
    "mc alias set local http://$MINIO:9000 $S3_USER $S3_PASSWORD >/dev/null 2>&1 && $*"
}

# Anonymous reads of objects only. MinIO's canned "download" policy also grants ListBucket,
# which would let anyone enumerate every public key, a draft facility's photo included (INT-081).
PUBLIC_READ='{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::directory-public/*"]}]}'
public_read_only() { # alias
  docker run --rm --network "$NETWORK" -e POLICY="$PUBLIC_READ" --entrypoint sh "$MC_IMAGE" -c \
    "mc alias set $1 http://$2:9000 $S3_USER $S3_PASSWORD >/dev/null 2>&1 \
     && printf '%s' \"\$POLICY\" > /tmp/public-read.json \
     && mc anonymous set-json /tmp/public-read.json $1/directory-public >/dev/null"
}

stop_admin() {
  for pid in $(netstat -ano 2>/dev/null | grep ':3000 ' | grep LISTENING | awk '{print $5}' | sort -u); do
    taskkill /PID "$pid" /F /T >/dev/null 2>&1 || kill -9 "$pid" >/dev/null 2>&1 || true
  done
}

status() {
  echo "api   live:   $(code http://localhost:8000/health/live/)"
  echo "api   ready:  $(curl -s --max-time 5 http://localhost:8000/health/ready/ || echo unreachable)"
  echo "minio live:   $(code http://localhost:9000/minio/health/live)"
  echo "minio public bucket listing, anonymous: $(code "$PUBLIC_MEDIA/")"
  echo "minio private bucket, anonymous: $(code http://localhost:9000/directory-private/)"
  if [ -f "$STATE/photo-url" ]; then
    echo "public photo, anonymous:         $(code "$(cat "$STATE/photo-url")")"
  fi
  echo "admin login:  $(code http://localhost:3000/login)"
}

down() {
  docker rm -f "$API" "$MINIO" >/dev/null 2>&1 || true
  stop_admin
  echo "stopped: $API, $MINIO, admin"
}

up() {
  mkdir -p "$STATE"
  down >/dev/null

  echo "== PostGIS and Redis =="
  for container in "$PG" "$REDIS"; do
    if [ "$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null)" != "true" ]; then
      docker start "$container" >/dev/null || { echo "FAIL: container $container does not exist"; exit 1; }
    fi
  done
  docker exec "$PG" psql -U directory -d postgres -q \
    -c "DROP DATABASE IF EXISTS $DB WITH (FORCE);" -c "CREATE DATABASE $DB OWNER directory;" \
    || { echo "FAIL: database"; exit 1; }
  docker exec "$REDIS" redis-cli -n 1 FLUSHDB >/dev/null
  echo "database $DB reset; redis db 1 flushed"

  echo "== MinIO =="
  docker run -d --name "$MINIO" --network "$NETWORK" -p 9000:9000 \
    -e MINIO_ROOT_USER="$S3_USER" -e MINIO_ROOT_PASSWORD="$S3_PASSWORD" \
    "$MINIO_IMAGE" server /data >/dev/null || { echo "FAIL: minio"; exit 1; }
  ready=0
  for _ in $(seq 1 30); do
    # Public media is readable by anyone; private evidence keeps the default: no anonymous access.
    if mc "mc mb --ignore-existing local/directory-public local/directory-private >/dev/null" \
        && public_read_only local "$MINIO"; then
      ready=1; break
    fi
    sleep 1
  done
  [ "$ready" = 1 ] || { echo "FAIL: minio buckets"; exit 1; }
  echo "buckets: directory-public (anonymous object reads, no listing), directory-private (private)"

  MOUNTS=()
  for entry in "$BACKEND"/*; do
    name="$(basename "$entry")"
    case "$name" in .venv|.pytest_cache|.ruff_cache) continue ;; esac
    MOUNTS+=(-v "$entry:/app/$name:ro")
  done
  ENVIRONMENT=(
    -e UV_NO_SYNC=1
    -e DATABASE_URL="postgresql://directory:directory@$PG:5432/$DB"
    -e REDIS_URL="redis://$REDIS:6379/1"
    -e S3_ENDPOINT_URL="http://$MINIO:9000"
    -e S3_REGION="us-east-1"
    -e S3_ACCESS_KEY_ID="$S3_USER"
    -e S3_SECRET_ACCESS_KEY="$S3_PASSWORD"
    -e S3_PUBLIC_BUCKET="directory-public"
    -e S3_PRIVATE_BUCKET="directory-private"
    -e S3_PUBLIC_MEDIA_BASE_URL="$PUBLIC_MEDIA"
    -e ALLOWED_HOSTS="localhost,127.0.0.1,$API"
  )

  echo "== Migrations, launch baseline and fixtures =="
  docker run --rm --network "$NETWORK" "${MOUNTS[@]}" "${ENVIRONMENT[@]}" "$IMAGE" sh -c \
    "uv run python manage.py migrate --noinput >/dev/null \
     && uv run python manage.py seed_e2e_fixtures \
     && uv run python manage.py seed_e2e_mobile_fixtures \
     && uv run python manage.py seed_local_directory" \
    || { echo "FAIL: migrations or fixtures"; exit 1; }

  echo "== Django (HTTP and WebSocket) =="
  docker run -d --name "$API" --network "$NETWORK" -p 8000:8000 "${MOUNTS[@]}" "${ENVIRONMENT[@]}" \
    "$IMAGE" sh -c "uv run python manage.py runserver 0.0.0.0:8000 --noreload" >/dev/null
  wait_for http://localhost:8000/health/ready/ 200 60 \
    || { echo "FAIL: django not ready"; docker logs "$API" 2>&1 | tail -20; exit 1; }
  echo "django ready on http://localhost:8000"

  echo "== A public photo, uploaded through the owner API =="
  # The fixtures carry no images. The mobile owner's public pharmacy gets one over HTTP, the
  # way the app sends it, so the phone has a real object in MinIO to load. The account comes
  # from the fixture command itself; neither its password nor the token is printed.
  docker exec -i "$API" uv run python - > "$STATE/photo-url" <<'PY' || { echo "FAIL: photo upload"; exit 1; }
import io
import json
import os
import urllib.request
import uuid

import django
from PIL import Image, ImageDraw

# Only to read the fixture account; every call below goes over HTTP to the running server.
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "directory_backend.settings.development")
django.setup()
from core.management.commands.seed_e2e_mobile_fixtures import MOBILE_OWNER  # noqa: E402

BASE = "http://localhost:8000/api/v1"


def call(path, body=None, token=None, content_type="application/json"):
    request = urllib.request.Request(BASE + path, data=body, method="POST" if body else "GET")
    if body:
        request.add_header("Content-Type", content_type)
    if token:
        request.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(request) as response:
        return json.load(response)


login = json.dumps({"phone": MOBILE_OWNER["phone"], "password": MOBILE_OWNER["password"],
                    "deviceName": "local-stack"}).encode()
token = call("/auth/login/", login)["accessToken"]
facility = next(i["id"] for i in call("/owner/facilities/", token=token)["items"] if "للبث" in i["nameAr"])

image = Image.new("RGB", (960, 640), (18, 94, 84))
draw = ImageDraw.Draw(image)
draw.rectangle((80, 80, 880, 560), outline=(240, 240, 240), width=12)
draw.line((480, 180, 480, 460), fill=(240, 240, 240), width=40)
draw.line((340, 320, 620, 320), fill=(240, 240, 240), width=40)
jpeg = io.BytesIO()
image.save(jpeg, "JPEG", quality=88)

boundary = uuid.uuid4().hex
body = (
    f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"shopfront.jpg\"\r\n"
    "Content-Type: image/jpeg\r\n\r\n"
).encode() + jpeg.getvalue() + f"\r\n--{boundary}--\r\n".encode()
uploaded = call(f"/owner/facilities/{facility}/images/", body, token,
                f"multipart/form-data; boundary={boundary}")
print(uploaded["url"])
PY
  echo "photo: $(cat "$STATE/photo-url")"

  echo "== Admin (production build) =="
  cd "$ADMIN"
  export ADMIN_API_ORIGIN="http://127.0.0.1:8000"
  export ADMIN_PUBLIC_ORIGIN="http://localhost:3000"
  ./node_modules/.bin/next build > "$STATE/admin-build.log" 2>&1 || { echo "FAIL: next build, see $STATE/admin-build.log"; exit 1; }
  ./node_modules/.bin/next start -p 3000 > "$STATE/admin.log" 2>&1 &
  admin=$!
  wait_for http://localhost:3000/login 200 60 || { echo "FAIL: admin did not start"; exit 1; }

  echo
  echo "== Status =="
  status
  echo
  echo "LOCAL STACK READY. Admin runs in the foreground; Ctrl+C stops it, scripts/local-stack.sh down stops the rest."
  wait "$admin"
}

case "${1:-}" in
  up) up ;;
  status) status ;;
  down) down ;;
  *) echo "usage: scripts/local-stack.sh up|status|down"; exit 2 ;;
esac
