#!/usr/bin/env bash
# Load Syria's real boundaries and quarters into an environment's database (DECISION-124), from the
# OpenStreetMap extract the map build already downloaded: what scripts/osm-boundaries.sh and
# `import_osm_boundaries` do locally, done on the server.
#
#   infrastructure/production/import-boundaries.sh /srv/daliini/production.env
#
# Without it a position resolves only to the nearest active province; with it, to the city and the
# quarter as well. GDAL comes from the running API's own image. Re-running corrects the same rows
# (their keys come from the OpenStreetMap ids). The fixture-only --prune and --relink are not used:
# a real facility keeps the address its owner gave.
set -euo pipefail

env_file="${1:?usage: import-boundaries.sh <env-file>}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
setting() { sed -n "s/^$1=//p" "$env_file" | tail -n 1; }
project="$(setting COMPOSE_PROJECT_NAME)"
map_dir="$(setting MAP_DIR)"
map_dir="${map_dir:-/srv/daliini/map}"
extract="$map_dir/current/extract.osm.pbf"
log() { printf '%s [boundaries] %s\n' "$(date -u +%H:%M:%S)" "$*"; }
compose() { docker compose -p "${project:-daliini}" -f "$here/compose.yml" --env-file "$env_file" "$@"; }

image="$(docker inspect -f '{{.Config.Image}}' "$(compose ps -q api)")"
# Beside the map, not under /tmp: a private /tmp (systemd's PrivateTmp, a root-only mktemp) is a
# directory Docker's daemon cannot mount into the container, which then sees an empty /in.
work="$(mktemp -d "$map_dir/boundaries.XXXXXX")"
chmod 777 "$work"  # the image runs as an unprivileged user, and writes its result here
trap 'rm -rf "$work"' EXIT
# The map build removes its extract once the tiles and graph are made; fetch the same file then.
if [ ! -f "$extract" ]; then
  url="$(setting MAP_EXTRACT_URL)"
  url="${url:-https://download.geofabrik.de/asia/syria-latest.osm.pbf}"
  log "downloading $url"
  curl -fsSL --retry 3 -o "$work/extract.osm.pbf" "$url"
  extract="$work/extract.osm.pbf"
fi

log "reading $(du -h "$extract" | cut -f1) with $image"
# The container reads the extract from the work directory, under a name it knows.
[ "$extract" = "$work/extract.osm.pbf" ] || cp "$extract" "$work/extract.osm.pbf"
chmod 644 "$work/extract.osm.pbf"
docker run --rm -v "$work:/in" -v "$work:/out" --entrypoint sh "$image" -c '
  set -e
  ogr2ogr -f GeoJSONSeq /tmp/admin.geojsonl /in/extract.osm.pbf multipolygons \
    -where "boundary='"'"'administrative'"'"' AND admin_level IN ('"'"'4'"'"','"'"'5'"'"','"'"'10'"'"')"
  ogr2ogr -f GeoJSONSeq /tmp/places.geojsonl /in/extract.osm.pbf multipolygons \
    -where "place IN ('"'"'neighbourhood'"'"','"'"'suburb'"'"','"'"'quarter'"'"')"
  cat /tmp/admin.geojsonl /tmp/places.geojsonl > /out/boundaries.geojsonl
'
log "$(wc -l < "$work/boundaries.geojsonl") features"

compose cp "$work/boundaries.geojsonl" api:/tmp/boundaries.geojsonl
compose exec -T api uv run python manage.py import_osm_boundaries /tmp/boundaries.geojsonl
compose exec -T api rm -f /tmp/boundaries.geojsonl
log "done"
