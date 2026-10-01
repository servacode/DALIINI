#!/usr/bin/env bash
# The province's map, on this machine, for a development build to draw.
#
#   scripts/local-map.sh <archive.pmtiles>
#
# The cartography is RahalGo's own style — the same owner's work — and the geography under it
# is an OpenStreetMap extract of Syria built into vector tiles by that project's pipeline. What
# this script does is translate: a PMTiles archive becomes the plain z/x/y tiles every renderer
# understands, because MapLibre on Android speaks XYZ over HTTP and knows nothing of the
# pmtiles:// scheme that the archive's own style uses.
#
# The tiles go into the local media store beside the facility photographs, where `adb reverse
# tcp:9000` already makes them reachable from a phone. Nothing here touches staging or
# production: those read a style from their own map host.
#
# Attribution travels with the data. The style carries "© OpenMapTiles © OpenStreetMap
# contributors", which the ODbL requires and the renderer shows.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARCHIVE="${1:-}"
WORK="$ROOT/.local-stack/map"
# Python here is the Windows interpreter on a Git Bash shell: it cannot open `/d/…`, which is
# what this shell calls the same file, and MSYS_NO_PATHCONV above stops the shell translating.
# `cygpath` does it explicitly; on a Unix machine there is nothing to translate and no cygpath.
host_path() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi
}
BUCKET_PREFIX="map"

if [ -z "$ARCHIVE" ] || [ ! -f "$ARCHIVE" ]; then
  echo "usage: scripts/local-map.sh <archive.pmtiles>"
  echo
  echo "  A PMTiles v3 archive covering the province. The one used so far was built by the"
  echo "  RahalGo map pipeline (maps/config/build.env there: Geofabrik 'syria', z0-16)."
  exit 2
fi

echo "== unpacking $(basename "$ARCHIVE") =="
rm -rf "$WORK"
python "$(host_path "$ROOT/scripts/pmtiles-extract.py")" "$(host_path "$ARCHIVE")" "$(host_path "$WORK")" || exit 1

echo "== binding the style to tiles this machine serves =="
python - "$(host_path "$ROOT")" "$(host_path "$WORK")" <<'PY' || exit 1
import json, pathlib, sys
root, work = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
style = json.loads((root / "maps" / "raqqa.style.json").read_text(encoding="utf-8"))
(work / "style.json").write_text(json.dumps(style, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"  {len(style['layers'])} layers, source {style['sources']['base']['tiles'][0]}")
PY

echo "== uploading to the local media store =="
python - "$(host_path "$ROOT")" "$(host_path "$WORK")" "$BUCKET_PREFIX" <<'PY' || exit 1
import pathlib, re, sys
import boto3
from botocore.client import Config

root, work, prefix = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3]
stack = (root / "scripts" / "local-stack.sh").read_text(encoding="utf-8")
s3 = boto3.client(
    "s3", endpoint_url="http://127.0.0.1:9000",
    aws_access_key_id=re.search(r'S3_USER="([^"]+)"', stack).group(1),
    aws_secret_access_key=re.search(r'S3_PASSWORD="([^"]+)"', stack).group(1),
    config=Config(signature_version="s3v4"), region_name="us-east-1",
)
s3.put_object(Bucket="directory-public", Key=f"{prefix}/style.json",
              Body=(work / "style.json").read_bytes(), ContentType="application/json")
tiles = sorted((work / "tiles").rglob("*.pbf"))
for i, path in enumerate(tiles, 1):
    key = f"{prefix}/tiles/{path.relative_to(work / 'tiles').as_posix()}"
    # Stored gzipped and served gzipped: the renderer inflates it, the wire stays small.
    s3.put_object(Bucket="directory-public", Key=key, Body=path.read_bytes(),
                  ContentType="application/x-protobuf", ContentEncoding="gzip")
    if i % 500 == 0 or i == len(tiles):
        print(f"  {i}/{len(tiles)} tiles")
PY

echo
echo "style:  http://localhost:9000/directory-public/$BUCKET_PREFIX/style.json"
echo "a tile: http://localhost:9000/directory-public/$BUCKET_PREFIX/tiles/14/0/0.pbf"
echo "the app reads it through adb reverse tcp:9000 tcp:9000"
