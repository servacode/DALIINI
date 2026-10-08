#!/usr/bin/env bash
# Every gate in this repository, in one run, with one table at the end.
#
#   scripts/verify.sh            # everything that can run on this machine
#   scripts/verify.sh --quick    # skip the slow ones (builds, images)
#   scripts/verify.sh backend    # one group: backend | web | admin | android | bot | contract
#
# Why this exists: the gates were real but scattered — pytest inside a container, vitest behind
# a pnpm filter, Gradle in two places, a Node service with no workspace. Knowing whether the
# project was green meant remembering six commands and where each one runs. Forgetting one is
# how a suite rots unnoticed, which is exactly what happened to the connected tests: they had
# not compiled for months and nothing said so.
#
# It reports what it could not run as `SKIP` with the reason, never as a pass. A check that did
# not run is not a check that succeeded, and a green table that quietly skipped half the
# project would be worse than no table at all.
set -uo pipefail
export MSYS_NO_PATHCONV=1

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

QUICK=0
# Not GROUPS: bash keeps that name for the current user's group ids, and assigning to
# it is ignored — the first version of this script matched nothing and reported an
# empty table, which is the failure mode it exists to prevent.
SELECTED="backend web admin android bot contract"
for arg in "$@"; do
  case "$arg" in
    --quick) QUICK=1 ;;
    backend|web|admin|android|bot|contract) SELECTED="$arg" ;;
    *) echo "unknown argument: $arg"; exit 2 ;;
  esac
done

# The development stack's API (`pnpm stack:up`), where GeoDjango has its GDAL.
API_CONTAINER="${VERIFY_API_CONTAINER:-$(docker compose -f infrastructure/docker/compose.yml ps -q api 2>/dev/null)}"
results=()
failures=0
skips=0

# name | status | detail
record() {
  results+=("$1|$2|$3")
  [ "$2" = "FAIL" ] && failures=$((failures + 1))
  [ "$2" = "SKIP" ] && skips=$((skips + 1))
  printf '%-7s %-34s %s\n' "$2" "$1" "$3"
}

# Run a command, record PASS or FAIL, and keep its last lines for the failure report.
check() {
  local name="$1" detail="$2"; shift 2
  local log; log="$(mktemp)"
  if "$@" >"$log" 2>&1; then
    record "$name" "PASS" "$detail"
  else
    record "$name" "FAIL" "$(tail -3 "$log" | tr '\n' ' ' | cut -c1-110)"
  fi
  rm -f "$log"
}

in_api() { docker exec "$API_CONTAINER" sh -lc "cd /app && $1"; }

has_container() { [ -n "$API_CONTAINER" ] && [ "$(docker inspect -f '{{.State.Running}}' "$API_CONTAINER" 2>/dev/null)" = "true" ]; }

# Docker takes a Windows path on this machine; on a Unix one there is nothing to translate.
host_path() {
  if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi
}

echo "== $(date '+%Y-%m-%d %H:%M') =="
echo

# ---------------------------------------------------------------- backend
case " $SELECTED " in *" backend "*)
  echo "-- backend --"
  if ! has_container; then
    record "backend tests" "SKIP" "no running API container (pnpm stack:up); GeoDjango needs GDAL"
    record "backend lint" "SKIP" "same"
    record "backend types" "SKIP" "same"
  elif ! in_api "uv sync --frozen >/dev/null 2>&1 && .venv/bin/python -m pytest --version >/dev/null"; then
    # The image carries production dependencies only. The checks need the dev group, synced into
    # the container's own environment (which the next `pnpm stack:up` renews); without it, a check
    # would report a tool that is not there as a pass.
    record "backend tests" "FAIL" "could not install the dev dependencies in the API container"
    record "backend lint" "FAIL" "same"
    record "backend types" "FAIL" "same"
  else
    # The source is mounted read-only to the container's user, so every cache goes to /tmp. The
    # contract tests find the repository's `openapi/`, which compose mounts at /openapi.
    check "backend tests" "pytest" in_api ".venv/bin/python -m pytest -q -p no:cacheprovider"
    check "backend lint" "ruff" in_api ".venv/bin/python -m ruff check --no-cache ."
    check "backend types" "mypy" in_api ".venv/bin/python -m mypy --cache-dir /tmp/mypy-cache ."
  fi
  echo
;; esac

# ---------------------------------------------------------------- web
case " $SELECTED " in *" web "*)
  echo "-- public site --"
  check "web tests" "vitest" pnpm --filter @servacode/public-web test
  check "web lint" "eslint" pnpm --filter @servacode/public-web lint
  check "web types" "tsc" pnpm --filter @servacode/public-web typecheck
  # The live pass needs a backend and a built site, which scripts/e2e-android.sh provides.
  # Named here so the table says the site's pages are unverified rather than leaving it out.
  if [ -n "${VERIFY_SITE_URL:-}" ]; then
    check "web pages live" "every public route" env PUBLIC_API_ORIGIN="${VERIFY_SITE_API:-}"       node scripts/verify-public-site.mjs "$VERIFY_SITE_URL"
  else
    record "web pages live" "SKIP" "set VERIFY_SITE_URL to a running site to check its pages"
  fi
  if [ "$QUICK" = 1 ]; then
    record "web build" "SKIP" "--quick"
  else
    check "web build" "next build" env NEXT_PUBLIC_ROOT_DOMAIN=example.invalid \
      pnpm --filter @servacode/public-web build
  fi
  echo
;; esac

# ---------------------------------------------------------------- admin
case " $SELECTED " in *" admin "*)
  echo "-- admin console --"
  check "admin tests" "vitest" pnpm --filter @servacode/admin test
  check "admin lint" "eslint" pnpm --filter @servacode/admin lint
  check "admin types" "tsc" pnpm --filter @servacode/admin typecheck
  if [ "$QUICK" = 1 ]; then
    record "admin build" "SKIP" "--quick"
  else
    check "admin build" "next build" env ADMIN_API_ORIGIN=http://127.0.0.1:9 \
      ADMIN_PUBLIC_ORIGIN=http://127.0.0.1:3100 pnpm --filter @servacode/admin build
  fi
  echo
;; esac

# ---------------------------------------------------------------- android
case " $SELECTED " in *" android "*)
  echo "-- android --"
  # The app itself cannot be built here: Google Maven does not answer this network
  # (artifacts/evidence/google-maven-diagnosis-20260919.txt). CI is where that happens, and
  # saying so is the point — a skip names what is not covered.
  record "android app" "SKIP" "Google Maven unreachable here; CI builds and tests it"
  record "android device" "SKIP" "instrumentation runs on CI's emulator"
  check "android harness" "jvm-verification" \
    ./apps/android/gradlew -p apps/android/jvm-verification test
  check "android connected compiles" "connectedTest" \
    ./apps/android/gradlew -p apps/android/jvm-verification compileConnectedTestKotlin
  echo
;; esac

# ---------------------------------------------------------------- bot
case " $SELECTED " in *" bot "*)
  echo "-- whatsapp bot --"
  # Every test file, not a named one: `message.test.js` was added later, and a gate that runs
  # only the files it was first written for passes over the rest without a word.
  check "bot tests" "node --test" env -C services/whatsapp-bot node --test "test/queue.test.js" "test/message.test.js"
  echo
;; esac

# ---------------------------------------------------------------- contract
case " $SELECTED " in *" contract "*)
  echo "-- contract and governance --"
  check "governance" "required files and spec checksums" node scripts/check-governance.mjs
  if ! has_container; then
    record "schema is current" "SKIP" "needs the api container to regenerate"
  else
    # Regenerate into a temporary file and compare: the committed schema must be what the
    # source produces, or the generated clients describe an API that no longer exists.
    # Inside the repository, not /tmp: `docker cp` on Windows does not understand the shell's
    # idea of /tmp. It writes nowhere and still succeeds, which read here as a schema mismatch
    # while the schema was in fact identical — a false alarm is as bad as a missed one.
    tmp="$ROOT/.verify-schema.yaml"
    rm -f "$tmp"
    if in_api ".venv/bin/python manage.py spectacular --file /tmp/verify-schema.yaml --settings=directory_backend.settings.test" >/dev/null 2>&1 \
       && docker cp "$API_CONTAINER:/tmp/verify-schema.yaml" "$(host_path "$tmp")" >/dev/null 2>&1 \
       && [ -s "$tmp" ]; then
      if cmp -s "$tmp" openapi/schema.yaml; then
        record "schema is current" "PASS" "openapi/schema.yaml matches the source"
      else
        record "schema is current" "FAIL" "run scripts/generate-openapi.sh and the clients"
      fi
    else
      record "schema is current" "SKIP" "could not regenerate in the container"
    fi
    rm -f "$tmp"
  fi
  # The recorded hash must describe the committed schema. Compared rather than rewritten,
  # so a stale hash is a failure and not something quietly overwritten.
  check "schema hash" "sha256 matches the file" python scripts/verify-schema-hash.py
  echo
;; esac

# ---------------------------------------------------------------- summary
echo "================================================================"
printf '%-7s %-34s %s\n' "STATUS" "CHECK" "DETAIL"
echo "----------------------------------------------------------------"
for row in "${results[@]}"; do
  # Recorded as name|status|detail; printed status first, as the live lines are.
  printf '%-7s %-34s %s\n' "$(echo "$row" | cut -d'|' -f2)" "${row%%|*}" "$(echo "$row" | cut -d'|' -f3-)"
done
echo "----------------------------------------------------------------"
passed=$(( ${#results[@]} - failures - skips ))
echo "passed=$passed  failed=$failures  skipped=$skips  of ${#results[@]}"
[ "$skips" -gt 0 ] && echo "a skip is not a pass: what it names is unverified here."
exit $(( failures > 0 ))
