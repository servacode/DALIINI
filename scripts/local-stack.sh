#!/usr/bin/env bash
# The whole platform on this machine, for a session with a real phone. Nothing is mocked: the
# development compose stack (PostGIS, Redis, the object store, the API with its WebSocket, the
# worker and beat), filled with the e2e and mobile fixtures and a local directory.
#
#   scripts/local-stack.sh up       build and start, migrate, seed; data is kept between runs
#   scripts/local-stack.sh status   health of each service
#   scripts/local-stack.sh down     stop everything (the data stays in its volumes)
#   scripts/local-stack.sh reset    stop and delete the data, for a clean start next time
#
# A phone on USB reaches it through `adb reverse tcp:8000 tcp:8000` and
# `adb reverse tcp:9000 tcp:9000` (scripts/android-link.sh, and
# docs/runbooks/android-local-networking.md), so public media is addressed as
# http://localhost:9000/directory-public on the phone and on this machine alike. Private
# evidence has no public address. No production secret is involved: the credentials are the
# development ones in infrastructure/docker/compose.yml, and the accounts are the fixtures in
# seed_e2e_fixtures and seed_e2e_mobile_fixtures.
#
# The console and the website run outside it: `pnpm dev:admin` (http://localhost:3000) and
# `pnpm dev:web` (http://localhost:3001).
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$ROOT/scripts/lib/stack.sh"
OSM="$ROOT/.local-stack/osm/boundaries.geojsonl"

dev() {
  docker compose -f "$COMPOSE_FILE_DEV" "$@"
}

manage() { # manage.py arguments...
  dev exec -T api uv run python manage.py "$@"
}

status() {
  echo "api   live:   $(http_code http://localhost:8000/health/live/)"
  echo "api   ready:  $(curl -s --max-time 5 --noproxy '*' http://localhost:8000/health/ready/ || echo unreachable)"
  echo "media public bucket listing, anonymous: $(http_code http://localhost:9000/directory-public/) (403 expected)"
  echo "media private bucket, anonymous:        $(http_code http://localhost:9000/directory-private/) (403 expected)"
  echo "console: $(http_code http://localhost:3000/login)  website: $(http_code http://localhost:3001/)  (000 until started)"
}

up() {
  echo "== The stack =="
  local build=(--build)
  [ "${STACK_NO_BUILD:-0}" = "1" ] && build=(--no-build)
  dev up -d "${build[@]}" --renew-anon-volumes >/dev/null || { echo "FAIL: docker compose up"; exit 1; }
  wait_for http://localhost:8000/health/ready/ 200 180 \
    || { echo "FAIL: the API did not become ready"; dev logs --tail 30 api; exit 1; }

  echo "== Fixtures and a local directory =="
  # The console fixtures first: they clear every `e2e-` facility, the mobile ones included.
  manage seed_e2e_fixtures && manage seed_e2e_mobile_fixtures && manage seed_local_directory \
    || { echo "FAIL: fixtures"; exit 1; }

  if [ -f "$OSM" ]; then
    echo "== Real boundaries, and the fixtures attached to them =="
    dev cp "$OSM" api:/tmp/boundaries.geojsonl >/dev/null \
      && manage import_osm_boundaries /tmp/boundaries.geojsonl --prune --relink \
      || { echo "FAIL: boundary import"; exit 1; }
  else
    echo "== No OpenStreetMap boundaries here; scripts/osm-boundaries.sh builds them =="
  fi

  echo "== A public photo, uploaded through the owner API =="
  # The fixtures carry no images. The mobile owner's public pharmacy gets one over HTTP, the way
  # the app sends it, so the phone has a real object to load. Once: a pharmacy that already has a
  # photo is left as it is. Neither the fixture password nor the token is printed.
  dev exec -T api uv run python - <<'PY' || { echo "FAIL: photo upload"; exit 1; }
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
images = call(f"/owner/facilities/{facility}/", token=token).get("images") or []
if images:
    print("photo: already there")
    raise SystemExit(0)

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
print("photo:", uploaded["url"])
PY

  echo
  echo "== Status =="
  status
  echo
  echo "LOCAL STACK READY. Next: pnpm dev:admin, and for a phone on USB, scripts/android-link.sh."
}

case "${1:-}" in
  up) up ;;
  status) status ;;
  down) dev down ;;
  reset) dev down -v ;;
  *) echo "usage: scripts/local-stack.sh up|status|down|reset"; exit 2 ;;
esac
