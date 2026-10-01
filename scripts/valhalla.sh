#!/usr/bin/env bash
# Valhalla: the app's own routing engine, on this machine, over Syria's OSM extract only.
#
#   scripts/valhalla.sh build    download the extract and build the routing tiles (slow, once)
#   scripts/valhalla.sh up       start the service on http://localhost:8002
#   scripts/valhalla.sh status   whether it answers, and for each of the three travel modes
#   scripts/valhalla.sh down     stop it
#
# A phone reaches it through `adb reverse tcp:8002 tcp:8002`, the same way it reaches Django on
# 8000 and MinIO on 9000. The Android build is pointed at it by DIRECTORY_ROUTING_BASE_URL.
#
# It is a service of its own on purpose: nothing in local-api, the Admin, the map style or the
# search depends on it, so a routing engine that is down costs the app its routes and nothing
# else. It is started and stopped separately from scripts/local-stack.sh for the same reason.
#
# The tiles are large and live in .local-stack/valhalla, which is git-ignored and sits on the
# drive the repository is on — deliberately not in a Docker volume, because Docker keeps its own
# disk on a drive with little room left. No credential is involved anywhere here: the data is
# public OpenStreetMap and the service has no authentication of its own.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${VALHALLA_IMAGE:-ghcr.io/valhalla/valhalla:3.9.0}"
NAME="${VALHALLA_CONTAINER:-local-valhalla}"
NETWORK="${LOCAL_NETWORK:-p10net}"
PORT="${VALHALLA_PORT:-8002}"
DATA="$ROOT/.local-stack/valhalla"
EXTRACT_URL="${VALHALLA_EXTRACT_URL:-https://download.geofabrik.de/asia/syria-latest.osm.pbf}"
EXTRACT="syria-latest.osm.pbf"
CONFIG="/data/valhalla.json"

# Damascus, near the Umayyad Square, and a point a few streets away: enough to prove each
# costing answers rather than to prove any particular road is right.
PROBE_FROM_LAT=33.5138
PROBE_FROM_LON=36.2765
PROBE_TO_LAT=33.5020
PROBE_TO_LON=36.2910

in_valhalla() { # command run inside the image with the data directory mounted
  docker run --rm -v "$DATA:/data" "$IMAGE" sh -c "$1"
}

build() {
  mkdir -p "$DATA"

  echo "== Image =="
  docker image inspect "$IMAGE" >/dev/null 2>&1 \
    || docker pull "$IMAGE" \
    || { echo "FAIL: cannot pull $IMAGE"; exit 1; }
  echo "$IMAGE present"

  echo "== Extract =="
  if [ -s "$DATA/$EXTRACT" ]; then
    echo "$EXTRACT already downloaded ($(du -h "$DATA/$EXTRACT" | cut -f1))"
  else
    # Written from inside the directory on purpose. MSYS_NO_PATHCONV is on for Docker's sake,
    # which also stops a /d/... path being translated for the Windows curl that answers here;
    # a relative name is understood by every curl this script might meet.
    (cd "$DATA" && curl -fL --retry 3 -C - -o "$EXTRACT" "$EXTRACT_URL") \
      || { echo "FAIL: cannot download the extract"; exit 1; }
    echo "$EXTRACT downloaded ($(du -h "$DATA/$EXTRACT" | cut -f1))"
  fi

  echo "== Configuration =="
  in_valhalla "valhalla_build_config \
      --mjolnir-tile-dir /data/tiles \
      --mjolnir-timezone /data/timezones.sqlite \
      --mjolnir-admin /data/admins.sqlite \
      > $CONFIG" \
    || { echo "FAIL: cannot write the configuration"; exit 1; }
  echo "configuration written"

  # Timezones and administrative boundaries are what let the engine reason about local time and
  # which side of the road a country drives on. Neither is required for a route to be found, so
  # a failure here is reported and the build goes on rather than leaving no engine at all.
  echo "== Timezones =="
  in_valhalla "valhalla_build_timezones > /data/timezones.sqlite" \
    && echo "timezones built" \
    || echo "WARN: timezones not built; routes are still found, local-time reasoning is not"

  echo "== Administrative boundaries =="
  in_valhalla "valhalla_build_admins -c $CONFIG /data/$EXTRACT" \
    && echo "admins built" \
    || echo "WARN: admins not built; routes are still found, country rules are defaults"

  echo "== Tiles =="
  in_valhalla "valhalla_build_tiles -c $CONFIG /data/$EXTRACT" \
    || { echo "FAIL: tiles"; exit 1; }
  echo "tiles built ($(du -sh "$DATA/tiles" 2>/dev/null | cut -f1)) in $DATA/tiles"
}

up() {
  [ -d "$DATA/tiles" ] || { echo "FAIL: no tiles; run scripts/valhalla.sh build first"; exit 1; }
  docker rm -f "$NAME" >/dev/null 2>&1
  docker run -d --name "$NAME" --network "$NETWORK" -p "$PORT:8002" \
    -v "$DATA:/data" "$IMAGE" valhalla_service "$CONFIG" 1 >/dev/null \
    || { echo "FAIL: cannot start $NAME"; exit 1; }
  for _ in $(seq 1 60); do
    if [ "$(probe auto)" = "200" ]; then
      echo "valhalla ready on http://localhost:$PORT"
      echo "a phone needs: adb reverse tcp:$PORT tcp:$PORT"
      return 0
    fi
    sleep 1
  done
  echo "FAIL: valhalla did not become ready"
  docker logs "$NAME" 2>&1 | tail -20
  exit 1
}

probe() { # costing -> HTTP status of a short route request
  curl -s -o /dev/null -w '%{http_code}' --max-time 10 \
    "http://localhost:$PORT/route" --data-raw \
    "{\"locations\":[{\"lat\":$PROBE_FROM_LAT,\"lon\":$PROBE_FROM_LON},{\"lat\":$PROBE_TO_LAT,\"lon\":$PROBE_TO_LON}],\"costing\":\"$1\"}"
}

status() {
  if [ "$(docker inspect -f '{{.State.Running}}' "$NAME" 2>/dev/null)" != "true" ]; then
    echo "valhalla: not running"
    return 1
  fi
  for costing in pedestrian motorcycle auto; do
    printf '  %-12s -> %s\n' "$costing" "$(probe "$costing")"
  done
}

down() {
  docker rm -f "$NAME" >/dev/null 2>&1 && echo "valhalla stopped" || echo "valhalla was not running"
}

case "${1:-}" in
  build) build ;;
  up) up ;;
  status) status ;;
  down) down ;;
  *) echo "usage: scripts/valhalla.sh {build|up|status|down}"; exit 2 ;;
esac
