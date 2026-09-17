#!/usr/bin/env bash
# Fail when the committed contract no longer matches what Django generates.
#
# The previous version ran `git diff --exit-code` on the canonical files. That could not
# fail while those files were untracked, because `git diff` ignores untracked paths, so
# the gate reported success on a contract that did not exist. This version verifies that
# the files are tracked, regenerates them, and checks both the document and its hash.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

SCHEMA="openapi/schema.yaml"
HASH="openapi/schema.sha256"

for path in "$SCHEMA" "$HASH"; do
  if ! git ls-files --error-unmatch "$path" >/dev/null 2>&1; then
    echo "FAIL: $path is not tracked in Git; the drift gate cannot prove anything." >&2
    exit 2
  fi
done

before_schema="$(git hash-object "$SCHEMA")"
before_hash="$(git hash-object "$HASH")"

./scripts/generate-openapi.sh

after_schema="$(git hash-object "$SCHEMA")"
after_hash="$(git hash-object "$HASH")"

status=0
if [[ "$before_schema" != "$after_schema" ]]; then
  echo "FAIL: $SCHEMA drifted from the source." >&2
  git --no-pager diff --stat -- "$SCHEMA" >&2 || true
  status=1
fi
if [[ "$before_hash" != "$after_hash" ]]; then
  echo "FAIL: $HASH drifted from the source." >&2
  status=1
fi

# Independent check: the recorded digest must describe the document on disk.
recorded="$(cut -d' ' -f1 <"$HASH")"
actual="$(python -c "
import hashlib
import pathlib
print(hashlib.sha256(pathlib.Path('$SCHEMA').read_bytes()).hexdigest())
")"
if [[ "$recorded" != "$actual" ]]; then
  echo "FAIL: $HASH does not match $SCHEMA." >&2
  status=1
fi

if [[ $status -eq 0 ]]; then
  echo "PASS: committed OpenAPI contract matches the source (sha256 $actual)."
fi
exit $status
