#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT/apps/backend"
OUT="$ROOT/openapi/schema.yaml"
HASH="$ROOT/openapi/schema.sha256"
mkdir -p "$ROOT/openapi"
cd "$BACKEND"
uv run python manage.py spectacular --file "$OUT" --settings=directory_backend.settings.test
cd "$ROOT"
python scripts/write-openapi-hash.py "$OUT" "$HASH"
