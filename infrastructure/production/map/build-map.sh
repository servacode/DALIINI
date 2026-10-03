#!/usr/bin/env bash
# The map and the routing engine's data, built from OpenStreetMap on the server (DECISION-082).
#
#   infrastructure/production/map/build-map.sh <env-file>
#       Build, install under $MAP_DIR (default /srv/daliini/map), restart Martin and Valhalla,
#       check them through Caddy, and go back to the previous build if the check fails.
#   infrastructure/production/map/build-map.sh --build-only <dir> <origin>
#       Build into <dir>, with the style bound to <origin>, and install nothing (CI).
#
# One extract of Syria (Geofabrik) feeds both:
# * Planetiler turns it into vector tiles in the OpenMapTiles schema the style reads, with Arabic
#   and English names, as one PMTiles archive that Martin serves;
# * Valhalla builds its routing graph from the same file, so a road on the map is a road the
#   app can be guided along.
# Run monthly by systemd/daliini-map.timer; the first build is part of setting a server up.
# Planetiler's other sources (coastlines, lakes, Natural Earth, about 1.3 GB) are kept in
# $MAP_DIR/sources and refreshed only when they change.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
production="$(cd "$here/.." && pwd)"
PLANETILER_IMAGE="${PLANETILER_IMAGE:-ghcr.io/onthegomap/planetiler:0.9.3}"
VALHALLA_IMAGE="${VALHALLA_IMAGE:-ghcr.io/valhalla/valhalla:3.9.0}"
EXTRACT_URL="${MAP_EXTRACT_URL:-https://download.geofabrik.de/asia/syria-latest.osm.pbf}"
HEAP="${MAP_BUILD_HEAP:-1500m}"
log() { printf '%s [map] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
as_me=(--user "$(id -u):$(id -g)")

build() { # <dir> <origin> <sources-cache>
  local out="$1" origin="$2" sources="$3"
  mkdir -p "$out/styles" "$out/valhalla" "$sources"

  log "downloading $EXTRACT_URL"
  curl -fsSL --retry 3 -o "$out/extract.osm.pbf" "$EXTRACT_URL"
  if curl -fsSL --retry 3 -o "$out/extract.md5" "$EXTRACT_URL.md5"; then
    (cd "$out" && sed "s| .*|  extract.osm.pbf|" extract.md5 | md5sum -c --quiet -)
    rm "$out/extract.md5"
  fi
  log "extract: $(du -h "$out/extract.osm.pbf" | cut -f1)"

  log "tiles (Planetiler)"
  docker run --rm "${as_me[@]}" -e JAVA_TOOL_OPTIONS="-Xmx$HEAP" \
    -v "$out:/data" -v "$sources:/data/sources" "$PLANETILER_IMAGE" \
    --osm_path=/data/extract.osm.pbf --output=/data/syria.pmtiles --force \
    --download --download_dir=/data/sources --tmpdir=/data/tmp \
    --languages=ar,en --transliterate=false
  # tmp/ and tile_weights.tsv.gz are Planetiler's working files; sources/ is only the mount point
  # Docker made for the cache.
  rm -rf "$out/tmp" "$out/tile_weights.tsv.gz"
  rmdir "$out/sources" 2>/dev/null || true
  log "tiles: $(du -h "$out/syria.pmtiles" | cut -f1)"

  log "routing graph (Valhalla)"
  docker run --rm "${as_me[@]}" -v "$out/valhalla:/data" \
    -v "$out/extract.osm.pbf:/extract.osm.pbf:ro" "$VALHALLA_IMAGE" bash -euc '
      valhalla_build_config --mjolnir-tile-dir /data/tiles --mjolnir-tile-extract /data/tiles.tar \
        --mjolnir-timezone /data/timezones.sqlite --mjolnir-admin /data/admins.sqlite \
        --logging-color false > /data/valhalla.json
      # Time zones and borders refine local-time and country rules; a route is found without them.
      valhalla_build_timezones > /data/timezones.sqlite || {
        echo "time zones not built"; rm -f /data/timezones.sqlite; }
      valhalla_build_admins -c /data/valhalla.json /extract.osm.pbf || echo "borders not built"
      valhalla_build_tiles -c /data/valhalla.json /extract.osm.pbf
      valhalla_build_extract -c /data/valhalla.json --overwrite
      rm -rf /data/tiles'
  log "routing graph: $(du -h "$out/valhalla/tiles.tar" | cut -f1)"

  python3 "$here/bind-style.py" "$origin" "$out/syria.pmtiles" "$out/styles/daliini.json"
  rm "$out/extract.osm.pbf"
  date -u +%Y-%m-%dT%H:%M:%SZ > "$out/built-at"
}

if [ "${1:-}" = "--build-only" ]; then
  out="${2:?usage: build-map.sh --build-only <dir> <origin>}"
  build "$out" "${3:?usage: build-map.sh --build-only <dir> <origin>}" "${MAP_SOURCES_DIR:-$out/../sources}"
  exit 0
fi

env_file="${1:?usage: build-map.sh <env-file> | --build-only <dir> <origin>}"
setting() { sed -n "s/^$1=//p" "$env_file" | tail -n 1; }
domain="$(setting ROOT_DOMAIN)"
map_dir="$(setting MAP_DIR)"
map_dir="${map_dir:-/srv/daliini/map}"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
release="$map_dir/releases/$stamp"
previous="$(readlink "$map_dir/current" 2>/dev/null || true)"
compose() { docker compose -f "$production/compose.yml" --env-file "$env_file" "$@"; }

build "$release" "https://maps.$domain" "$map_dir/sources"

switch_to() { # <release dir>
  ln -sfn "$1" "$map_dir/current.next" && mv -T "$map_dir/current.next" "$map_dir/current"
  compose up -d --force-recreate --no-deps martin valhalla >/dev/null
}

ask() { curl -fsS --max-time 10 --resolve "maps.$domain:443:127.0.0.1" "$@"; }
check() {
  for _ in $(seq 1 30); do
    if ask -o /dev/null "https://maps.$domain/style/daliini" &&
       ask "https://maps.$domain/routing/status" | grep -q '"tileset_last_modified":[1-9]'; then
      # Raqqa's centre, two streets apart: the graph answers for the city the platform began in.
      ask -o /dev/null "https://maps.$domain/routing/route" --data \
        '{"locations":[{"lat":35.9506,"lon":39.0089},{"lat":35.9530,"lon":39.0160}],"costing":"auto"}'
      return
    fi
    sleep 4
  done
  return 1
}

log "switching to $stamp (was ${previous:-none})"
switch_to "$release"
if ! check; then
  log "the new map does not answer through Caddy"
  if [ -n "$previous" ]; then
    switch_to "$previous"
    log "back on $(basename "$previous")"
  fi
  exit 1
fi
log "map ready: https://maps.$domain/style/daliini"

# Keep this build and the one before it.
for old in $(ls -1d "$map_dir"/releases/* | sort | head -n -2); do
  [ "$old" = "$previous" ] || rm -rf "$old"
done
