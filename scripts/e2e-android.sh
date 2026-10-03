#!/usr/bin/env bash
# Drive the Android data layer, and the Android -> console -> Android hand-off, against a real stack.
#
# Nothing is mocked. This starts the suites' own compose stack (scripts/lib/stack.sh) from empty
# volumes: PostGIS with the migrations and so the launch baseline, Redis, the object store with a
# public-media bucket anyone may read and a private-evidence bucket nobody may, and the API with
# its WebSocket endpoint. It adds the console and mobile fixtures. Then:
#   1. the connected suite in apps/android/jvm-verification;
#   2. the hand-off: the owner submits with evidence (JVM), an operator opens the evidence and
#      approves in the real console (Playwright against `next start`), and the facility appears
#      in public discovery with its photo (JVM);
#   3. storage isolation: a public photo is readable without credentials, private evidence is
#      not, and the public bucket cannot be listed.
#
# What this does not do is build or run the Android app itself; the Android Build Verification
# workflow does. See apps/android/jvm-verification/README.md.
#
# E2E_KEEP_STACK=1 leaves everything running afterwards, for investigating a failure or for
# scripts/verify-all.sh to build the website against the data this run created.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$ROOT/scripts/lib/stack.sh"
ADMIN="$ROOT/apps/admin"
HARNESS="$ROOT/apps/android/jvm-verification"
GRADLEW="$ROOT/apps/android/gradlew"
API="http://127.0.0.1:$E2E_API_PORT"
MEDIA="http://127.0.0.1:$E2E_S3_PORT"

cleanup() {
  stop_port "$E2E_ADMIN_PORT"
  e2e_down
}
if [ "${E2E_KEEP_STACK:-0}" != "1" ]; then trap cleanup EXIT; fi

failures=0
step() { # name, command...
  local name="$1"; shift
  if "$@"; then echo "PASS  $name"; else echo "FAIL  $name"; failures=$((failures + 1)); fi
}

echo "== 1. A fresh stack, and the fixtures =="
e2e_up || exit 1
# The console fixtures first: they clear every `e2e-` facility, the mobile ones included.
e2e_manage seed_e2e_fixtures && e2e_manage seed_e2e_mobile_fixtures \
  || { echo "FAIL: fixtures"; exit 1; }
CONTAINER="$(e2e_api_container)"

connected() { # extra gradle args
  (cd "$HARNESS" && DIRECTORY_API_BASE_URL="$API/" E2E_API_CONTAINER="$CONTAINER" \
    "$GRADLEW" -p "$HARNESS" --console=plain connectedCheck "$@")
}

handoff() { # phase
  HANDOFF_PHASE="$1" connected --tests '*HandoffConnectedTest*'
}

review_in_console() {
  (cd "$ADMIN" && E2E_HANDOFF=1 E2E_API_ORIGIN="$API" E2E_API_CONTAINER="$CONTAINER" \
    ADMIN_E2E_URL="http://localhost:$E2E_ADMIN_PORT" \
    ./node_modules/.bin/playwright test tests/e2e/handoff.spec.ts)
}

echo
echo "== 2. Connected suite =="
step "connected suite" connected

echo
echo "== 3. Hand-off: owner submits with evidence (JVM) =="
step "handoff submit" handoff submit

echo
echo "== 4. Hand-off: an operator opens the evidence and approves (console, production build) =="
admin_serve "$API" "$E2E_ADMIN_PORT" "$ADMIN/.e2e-admin.log" || exit 1
step "handoff admin review" review_in_console

echo
echo "== 5. Hand-off: the facility is public, with its photo (JVM) =="
step "handoff public" handoff public

echo
echo "== 6. Storage isolation =="
# The keys come from the database, which is where the platform itself finds them.
keys="$(e2e_manage shell -c "
from facilities.models import FacilityImage, VerificationEvidence
print(FacilityImage.objects.values_list('storage_key', flat=True).first() or '')
print(VerificationEvidence.objects.values_list('storage_key', flat=True).first() or '')
" 2>/dev/null | tail -2)"
public_key="$(printf '%s\n' "$keys" | sed -n 1p)"
private_key="$(printf '%s\n' "$keys" | sed -n 2p)"
public_status="$(http_code "$MEDIA/directory-public/$public_key")"
private_status="$(http_code "$MEDIA/directory-private/$private_key")"
listing_status="$(http_code "$MEDIA/directory-public/")"
echo "public object: $public_status; private object: $private_status; public listing: $listing_status"
step "public media readable anonymously" test -n "$public_key" -a "$public_status" = "200"
step "private evidence refused anonymously" test -n "$private_key" -a "$private_status" = "403"
step "public keys cannot be listed anonymously" test "$listing_status" = "403"

echo
echo "failures=$failures"
exit $((failures > 0))
