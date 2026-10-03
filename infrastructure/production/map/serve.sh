#!/usr/bin/env bash
# Martin and Valhalla on this machine, over a map directory, as the stack runs them (DECISION-082).
#
#   infrastructure/production/map/serve.sh <map-dir>     Martin on :3000, Valhalla on :8002
#   infrastructure/production/map/serve.sh stop
#
# For CI and for looking at a build before it goes on a server; the server runs the same images
# and arguments through compose.yml. <map-dir> is what build-map.sh --build-only or fixture.py
# wrote: syria.pmtiles, styles/, and valhalla/ when there is a routing graph.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../../.." && pwd)"
MARTIN_IMAGE="ghcr.io/maplibre/martin:1.16.1"
VALHALLA_IMAGE="ghcr.io/valhalla/valhalla:3.9.0"

docker rm -f daliini-map-martin daliini-map-valhalla >/dev/null 2>&1 || true
[ "${1:-}" = "stop" ] && exit 0

map="$(cd "${1:?usage: serve.sh <map-dir> | stop}" && pwd)"
docker run -d --name daliini-map-martin -p 3000:3000 \
  -v "$map:/map:ro" \
  -v "$repo/packages/design-tokens/fonts/android/font:/fonts:ro" \
  -v "$repo/maps/sprite/daliini:/sprites/daliini:ro" \
  "$MARTIN_IMAGE" /map/syria.pmtiles --font /fonts --sprite /sprites/daliini --style /map/styles >/dev/null
if [ -f "$map/valhalla/valhalla.json" ]; then
  docker run -d --name daliini-map-valhalla -p 8002:8002 -v "$map/valhalla:/data:ro" \
    "$VALHALLA_IMAGE" valhalla_service /data/valhalla.json 2 >/dev/null
fi

for _ in $(seq 1 60); do
  if curl -fs --noproxy '*' -o /dev/null http://127.0.0.1:3000/health; then
    echo "martin: http://127.0.0.1:3000/style/daliini"
    break
  fi
  sleep 1
done
curl -fs --noproxy '*' -o /dev/null http://127.0.0.1:3000/health || { docker logs daliini-map-martin; exit 1; }
if [ -f "$map/valhalla/valhalla.json" ]; then
  for _ in $(seq 1 60); do
    curl -fs --noproxy '*' -o /dev/null http://127.0.0.1:8002/status && { echo "valhalla: http://127.0.0.1:8002"; exit 0; }
    sleep 1
  done
  docker logs daliini-map-valhalla
  exit 1
fi
