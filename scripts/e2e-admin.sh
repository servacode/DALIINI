#!/usr/bin/env bash
# Run the console's golden path against a real stack.
#
#   scripts/e2e-admin.sh [playwright arguments]
#
# Nothing is mocked. This starts the suites' own compose stack (scripts/lib/stack.sh) from empty
# volumes, which applies the migrations and so the launch baseline, creates the test operators,
# builds the console for production and serves it with `next start`, then runs Playwright against
# that. The development stack is left alone.
#
# E2E_KEEP_STACK=1 leaves everything running afterwards, for investigating a failure.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$ROOT/scripts/lib/stack.sh"
ADMIN="$ROOT/apps/admin"

cleanup() {
  stop_port "$E2E_ADMIN_PORT"
  e2e_down
}
if [ "${E2E_KEEP_STACK:-0}" != "1" ]; then trap cleanup EXIT; fi

echo "== 1. A fresh stack, and the operators =="
e2e_up || exit 1
e2e_manage seed_e2e_fixtures || { echo "FAIL: fixtures"; exit 1; }

echo
echo "== 2. The console, production build =="
admin_serve "http://127.0.0.1:$E2E_API_PORT" "$E2E_ADMIN_PORT" "$ADMIN/.e2e-admin.log" || exit 1

echo
echo "== 3. Playwright =="
cd "$ADMIN" || exit 1
E2E_API_ORIGIN="http://127.0.0.1:$E2E_API_PORT" \
E2E_API_CONTAINER="$(e2e_api_container)" \
ADMIN_E2E_URL="http://localhost:$E2E_ADMIN_PORT" \
  ./node_modules/.bin/playwright test "$@"
status=$?

echo
echo "playwright exit=$status"
exit $status
