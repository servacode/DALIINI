#!/usr/bin/env bash
# Everything, actually run: the static gates, then every live suite against real servers.
#
#   scripts/verify-all.sh
#
# `scripts/verify.sh` answers "does the code hold together" — tests, types, lint, builds. This
# answers the harder question: does the product work when a database, an object store, a
# backend, a console and a website are all running at once and a browser is driving them.
#
# Four stages, in the order that fails fastest:
#
#   1. the static gates, because there is no point starting servers for code that does not
#      compile;
#   2. the console against a real stack, in a real browser;
#   3. the whole cycle — register, browse, submit a facility with evidence, approve it in the
#      console, see it published, and prove the evidence stayed private;
#   4. the public site, built against that same backend and driven in a browser.
#
# Each stage brings up and tears down its own stack. That is slower than sharing one, and it is
# the point: a suite that only passes because a previous suite left something behind is a suite
# that will not pass on a fresh machine.
#
# The stacks are the suites' own compose project (scripts/lib/stack.sh, compose.e2e.yml), on
# ports that are not the defaults, so a development stack on this machine keeps running
# untouched.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$ROOT/scripts/lib/stack.sh"
cd "$ROOT"

API_PORT="$E2E_API_PORT"
WEB_PORT="$E2E_WEB_PORT"

stages=()
failures=0

record() {
  stages+=("$1|$2|$3")
  [ "$2" = "FAIL" ] && failures=$((failures + 1))
  printf '\n%s  %s — %s\n\n' "$2" "$1" "$3"
}

started="$(date +%s)"
echo "== verify-all  $(date '+%Y-%m-%d %H:%M') =="

# ------------------------------------------------------------------ 1. static
echo
echo "########## 1 / 4  the static gates ##########"
if bash scripts/verify.sh; then
  record "static gates" "PASS" "tests, types, lint and builds across every package"
else
  record "static gates" "FAIL" "see the table above"
fi

# ------------------------------------------------------------------ 2. console
echo
echo "########## 2 / 4  the console, in a browser ##########"
if bash scripts/e2e-admin.sh; then
  record "console e2e" "PASS" "every page opened, every golden path driven"
else
  record "console e2e" "FAIL" "see the output above"
fi

# ------------------------------------------------------------------ 3. the cycle
echo
echo "########## 3 / 4  the whole cycle ##########"
if E2E_KEEP_STACK=1 bash scripts/e2e-android.sh; then
  record "full cycle" "PASS" "register, submit with evidence, approve, publish, stay private"
else
  record "full cycle" "FAIL" "see the output above"
fi

# ------------------------------------------------------------------ 4. the site
# The stack from stage 3 is still up (E2E_KEEP_STACK), so the site is built against the same
# backend the cycle just filled with real data.
echo
echo "########## 4 / 4  the public site, against that backend ##########"
site_status="FAIL"
site_detail="the site did not start"
if curl -s -o /dev/null --max-time 5 "http://127.0.0.1:$API_PORT/api/v1/public/provinces/"; then
  (
    cd apps/web
    export PUBLIC_API_ORIGIN="http://127.0.0.1:$API_PORT"
    export NEXT_PUBLIC_API_ORIGIN="http://127.0.0.1:$API_PORT"
    export NEXT_PUBLIC_ROOT_DOMAIN="daliini.test"
    export SUPPORT_EMAIL="support@daliini.test"
    export PRIVACY_CONTACT_EMAIL="privacy@daliini.test"
    ./node_modules/.bin/next build >/dev/null 2>&1 || exit 1
    ./node_modules/.bin/next start -p "$WEB_PORT" > "$ROOT/.verify-web.log" 2>&1 &
    echo $! > "$ROOT/.verify-web.pid"
    for _ in $(seq 1 40); do
      [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$WEB_PORT/")" = "200" ] && exit 0
      sleep 1
    done
    exit 1
  )
  if [ $? -eq 0 ]; then
    pages=0
    browser=0
    PUBLIC_API_ORIGIN="http://127.0.0.1:$API_PORT" \
      node scripts/verify-public-site.mjs "http://127.0.0.1:$WEB_PORT" || pages=1
    (cd apps/web && WEB_E2E_URL="http://127.0.0.1:$WEB_PORT" \
      PUBLIC_API_ORIGIN="http://127.0.0.1:$API_PORT" ./node_modules/.bin/playwright test) || browser=1
    if [ "$pages" = 0 ] && [ "$browser" = 0 ]; then
      site_status="PASS"; site_detail="every route served, and the pages hydrate in a browser"
    elif [ "$pages" = 1 ]; then
      site_detail="a route failed to serve"
    else
      site_detail="a page did not behave in the browser"
    fi
  fi
else
  site_detail="the backend from stage 3 is not answering on $API_PORT"
fi
record "public site" "$site_status" "$site_detail"

# The stack was kept for stage 4; nothing is left running.
[ -f "$ROOT/.verify-web.pid" ] && kill "$(cat "$ROOT/.verify-web.pid")" 2>/dev/null
rm -f "$ROOT/.verify-web.pid"
stop_port "$E2E_ADMIN_PORT"
e2e_down

# ------------------------------------------------------------------ summary
elapsed=$(( $(date +%s) - started ))
echo
echo "================================================================"
printf '%-7s %-18s %s\n' "RESULT" "STAGE" "WHAT IT COVERED"
echo "----------------------------------------------------------------"
for row in "${stages[@]}"; do
  printf '%-7s %-18s %s\n' "$(echo "$row" | cut -d'|' -f2)" "${row%%|*}" "$(echo "$row" | cut -d'|' -f3-)"
done
echo "----------------------------------------------------------------"
printf 'stages passed=%s failed=%s   in %dm %ds\n' \
  "$(( ${#stages[@]} - failures ))" "$failures" "$((elapsed / 60))" "$((elapsed % 60))"
echo
echo "still outside any automated run, and so unverified:"
echo "  - the WhatsApp bot against a real paired account"
echo "  - the Android app itself (Google Maven does not answer this network; CI builds it)"
echo "  - anything that needs a phone in a person's hand"
exit $(( failures > 0 ))
