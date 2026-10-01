#!/usr/bin/env bash
# The boundaries of Syria's places, out of an OpenStreetMap extract and into a file the platform
# can import.
#
#   scripts/osm-boundaries.sh [extract.osm.pbf]
#
# The extract defaults to the one Valhalla already built its routing graph from
# (`.local-stack/valhalla/syria-latest.osm.pbf`, a Geofabrik download), so a machine that has run
# the routing engine needs nothing new.
#
# What comes out is `.local-stack/osm/boundaries.geojsonl`: one feature per line, carrying only the
# administrative boundaries and the named quarters — about eight megabytes out of eighty. Then
# `manage.py import_osm_boundaries` reads it, which `scripts/local-stack.sh` does by itself.
#
# The filtering is GDAL's, because GDAL is what reads the `.pbf` format. It runs in a one-off
# container from the backend's own image — the same GDAL Django's GIS layer uses — so this needs no
# running stack and nothing installed on the machine but Docker.
#
# The data is © OpenStreetMap contributors, under the ODbL. Attribution travels with it: the app
# already shows it on the map, and anything derived from these boundaries carries the same licence.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXTRACT="${1:-$ROOT/.local-stack/valhalla/syria-latest.osm.pbf}"
IMAGE="${LOCAL_BACKEND_IMAGE:-directory-v3-p2dev:local}"
OUT_DIR="$ROOT/.local-stack/osm"
OUT="$OUT_DIR/boundaries.geojsonl"

# Docker on Windows is a Windows program: it cannot open `/d/…`, which is what this shell calls the
# same file. `cygpath` translates; on a Unix machine there is nothing to translate and no cygpath.
host_path() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi
}

if [ ! -f "$EXTRACT" ]; then
  echo "no extract at $EXTRACT"
  echo
  echo "  Download one from https://download.geofabrik.de/asia/syria.html and pass its path,"
  echo "  or run scripts/valhalla.sh first — it downloads the same file."
  exit 2
fi

if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
  echo "image '$IMAGE' is not built; run scripts/local-stack.sh up once"
  exit 2
fi

mkdir -p "$OUT_DIR"
EXTRACT_DIR="$(cd "$(dirname "$EXTRACT")" && pwd)"
EXTRACT_NAME="$(basename "$EXTRACT")"

# Two passes over the extract, because the two things wanted are tagged differently: a governorate
# or a district is an administrative boundary, and a quarter of Aleppo or Raqqa is a place polygon.
# Both land in one file, and the import decides what each feature is.
echo "== reading $EXTRACT_NAME =="
docker run --rm \
  -v "$(host_path "$EXTRACT_DIR"):/in:ro" \
  -v "$(host_path "$OUT_DIR"):/out" \
  --entrypoint sh "$IMAGE" -lc "
    set -e
    ogr2ogr -f GeoJSONSeq /tmp/admin.geojsonl \"/in/$EXTRACT_NAME\" multipolygons \
      -where \"boundary='administrative' AND admin_level IN ('4','5','10')\"
    ogr2ogr -f GeoJSONSeq /tmp/places.geojsonl \"/in/$EXTRACT_NAME\" multipolygons \
      -where \"place IN ('neighbourhood','suburb','quarter')\"
    cat /tmp/admin.geojsonl /tmp/places.geojsonl > /out/boundaries.geojsonl
  " || { echo "FAIL: GDAL could not read the extract"; exit 1; }

echo
echo "boundaries: $OUT ($(wc -l < "$OUT") features)"
echo "scripts/local-stack.sh up imports them; to do it by hand:"
echo "  docker exec local-api sh -lc 'cd /app && uv run python manage.py import_osm_boundaries /osm/boundaries.geojsonl'"
