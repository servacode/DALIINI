#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA="$ROOT/openapi/schema.yaml"
GENERATOR_VERSION="7.15.0"
if [[ ! -f "$SCHEMA" ]]; then
  echo "Missing generated OpenAPI schema: $SCHEMA" >&2
  exit 2
fi
if ! command -v openapi-generator-cli >/dev/null 2>&1; then
  echo "openapi-generator-cli $GENERATOR_VERSION is required in PATH" >&2
  exit 3
fi
actual="$(openapi-generator-cli version 2>/dev/null || true)"
if [[ "$actual" != "$GENERATOR_VERSION" ]]; then
  echo "Expected openapi-generator-cli $GENERATOR_VERSION, got: $actual" >&2
  exit 4
fi
rm -rf "$ROOT/packages/api-typescript/generated" "$ROOT/packages/api-kotlin/generated" "$ROOT/packages/api-swift/generated"
openapi-generator-cli generate -i "$SCHEMA" -g typescript-fetch -c "$ROOT/openapi/config/typescript.json" -o "$ROOT/packages/api-typescript/generated"
openapi-generator-cli generate -i "$SCHEMA" -g kotlin -c "$ROOT/openapi/config/kotlin.json" -o "$ROOT/packages/api-kotlin/generated"
openapi-generator-cli generate -i "$SCHEMA" -g swift5 -c "$ROOT/openapi/config/swift.json" -o "$ROOT/packages/api-swift/generated"
