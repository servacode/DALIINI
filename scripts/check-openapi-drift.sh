#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
./scripts/generate-openapi.sh
git diff --exit-code -- openapi/schema.yaml openapi/schema.sha256
