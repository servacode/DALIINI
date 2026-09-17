#!/usr/bin/env bash
# Generate the TypeScript, Kotlin and Swift clients from the canonical OpenAPI document.
#
# The generator version is pinned. It runs from the official image by default so the
# result is reproducible on any machine and in CI without a local install; a local
# openapi-generator-cli of the same version is used instead when one is present.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA="$ROOT/openapi/schema.yaml"
GENERATOR_VERSION="7.15.0"
IMAGE="openapitools/openapi-generator-cli:v${GENERATOR_VERSION}"

if [[ ! -f "$SCHEMA" ]]; then
  echo "Missing generated OpenAPI schema: $SCHEMA" >&2
  echo "Run ./scripts/generate-openapi.sh first." >&2
  exit 2
fi

use_local=0
if command -v openapi-generator-cli >/dev/null 2>&1; then
  actual="$(openapi-generator-cli version 2>/dev/null || true)"
  if [[ "$actual" == "$GENERATOR_VERSION" ]]; then
    use_local=1
  else
    echo "Ignoring local openapi-generator-cli $actual; this project pins $GENERATOR_VERSION." >&2
  fi
fi

if [[ $use_local -eq 0 ]] && ! command -v docker >/dev/null 2>&1; then
  echo "Need either openapi-generator-cli $GENERATOR_VERSION in PATH or Docker." >&2
  exit 3
fi

generate() {
  local generator="$1" config="$2" out="$3"
  rm -rf "$ROOT/$out"
  mkdir -p "$ROOT/$out"
  if [[ $use_local -eq 1 ]]; then
    openapi-generator-cli generate \
      -i "$SCHEMA" -g "$generator" -c "$ROOT/$config" -o "$ROOT/$out"
  else
    MSYS_NO_PATHCONV=1 docker run --rm -v "$ROOT:/work" -w /work "$IMAGE" generate \
      -i "openapi/schema.yaml" -g "$generator" -c "$config" -o "$out"
  fi
}

generate typescript-fetch openapi/config/typescript.json packages/api-typescript/generated
generate kotlin           openapi/config/kotlin.json     packages/api-kotlin/generated
generate swift5           openapi/config/swift.json      packages/api-swift/generated

# Generators emit their own bookkeeping directory, whose FILES manifest changes on every
# run, and a helper script for pushing to GitHub. Neither belongs to the contract, and the
# manifest would make the drift gate fail spuriously.
for dir in packages/api-typescript packages/api-kotlin packages/api-swift; do
  rm -rf "$ROOT/$dir/generated/.openapi-generator"
  rm -f "$ROOT/$dir/generated/git_push.sh"
done
echo "Generated clients from $SCHEMA with openapi-generator $GENERATOR_VERSION."
