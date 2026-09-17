#!/usr/bin/env bash
set -euo pipefail
: "${STAGING_API_ORIGIN:?STAGING_API_ORIGIN is required}"
origin="${STAGING_API_ORIGIN%/}"
python - "$origin" <<'PY2'
import json, sys, urllib.request, urllib.error
origin=sys.argv[1]
for path, expected in [("/health/live/", "ok"), ("/health/ready/", "ready")]:
    req=urllib.request.Request(origin+path, headers={"User-Agent":"directory-v3-staging-smoke/1"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            body=json.load(r)
            if r.status != 200 or body.get("status") != expected:
                raise SystemExit(f"{path} failed: status={r.status} body={body}")
    except urllib.error.URLError as exc:
        raise SystemExit(f"{path} unavailable: {exc}") from exc
print("staging health smoke PASS")
PY2
