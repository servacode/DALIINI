# Implementation Plan

Updated: 2026-09-24 (complete product batch)

## Verification baseline

`RECEIPT-AUDIT-2026-09-17.md` is the accepted verification baseline. A gate is PASS only
with a real command exit code. Textual source assertions no longer qualify a gate on their
own, and a schema is not qualified by reading its YAML: a representative sample of
operations must be exercised against the running service and validated against the
document. A web screen is not qualified until it has run in a real browser — the Admin's
CSP fault (INT-045) was invisible to every HTTP-level check.

## In flight — complete product batch (2026-09-24)

One batch, no device dependency, ending in a single local-debug APK for the morning's review.

**A — Home, Map and motion.** The advertisement slider on Home (the `Advertisement` model,
its targeting and schedule already exist and the public endpoint already serves them), quick
filters for nearest / open now / on duty that use the directory query the backend already
takes, a map quick-filter bar and a vertical category rail fed by the province's real
taxonomy, an automatic location header that resolves the user's province and area instead of
making them choose, and one central navigation transition to replace the window animation
that shrinks a screen into a square.

**B — Map and navigation from RahalGo.** Audit `/d/RahalGo`'s mature map stack, classify
every part as REUSE_AS_IS / ADAPT / DO_NOT_REUSE, and bring across what serves this app:
route preview before live navigation, route progress, rerouting, and guidance. Directory's
own camera policy, persistence, facility focus and single map destination survive unchanged,
and the tests must keep INT-096's accumulation fixed.

**C — What the product was missing.** Favourites and a notification inbox, both end to end
from migration to Android; the account's own screens: edit profile, change password, account
verification as far as the specification defines it, and account deletion.

**D — Legal, help and settings.** Versioned legal and help content served from the backend
rather than frozen into the APK, a settings screen of only what exists, and the contact
channels that are actually configured.

**E — Qualification.** Backend tests, migrations, ruff, mypy, OpenAPI with zero errors and
zero warnings, regenerated TypeScript, Kotlin and Swift clients, Admin checks, Android unit
and JVM tests, lint, `assembleLocalDebug`, the qualifiers, and one APK signed by the stable
debug key `21e9ff5c…`.

Rules for the batch: the source of truth in `docs/spec` decides every product question; no
invented policy, no fake data, no workaround where the backend is the right place; the
verified functional baseline (sessions, province, permissions, ordering, distance,
pagination, media separation, camera, focus, evidence privacy, generated clients, error
envelope) does not regress; INT-089, INT-096, FCM delivery and `LAUNCH_POLICY_PENDING` keep
their current status until real device and provider evidence exists.

## Completed

**FIX-P0** — ASGI initialization order, Django messages without sessions, the sessions
migration app label, and the AGP 9 convention plugin plus source sets.

**P2 connected qualification** — migration convergence, both business invariants proven at
the database level, and the test suite running for the first time. `CONNECTED_VERIFIED`.

**P10 contracts recovery** — a real generated contract, three generated clients and a drift
gate that can fail. `CONNECTED_VERIFIED`.

**Contract alignment** — one error envelope, one cursor envelope, camelCase throughout,
`sequence` on the wire for business hours.

**Launch baseline** — fourteen provinces and the health taxonomy seeded by migration with
deterministic ids and non-destructive re-runs. P4 `CONNECTED_VERIFIED`.

**P11–P13 Admin data binding and golden path** — the Admin went from thirteen static pages
to a working console on the real backend. Sign-in, refresh, sign-out, `/admin/me/`,
permission-aware navigation, and every operational page bound through one BFF route and
the generated client. Cycle J runs from the Admin. 31 Playwright tests against a production
build, Django, PostGIS and Redis; 71 Vitest tests; 211 backend tests. P12 and P13
`CONNECTED_VERIFIED`.

**Android generated-client integration (§1–§48)** — the data layer runs on the generated
client, verified on the JVM against the real backend: 26 connected and 89 unit tests. The
app itself is not built. Fourteen defects were fixed on the way, nine of them in the contract
or the backend, and three more are recorded open (INT-048 to INT-064).

**Android golden path (§49–§89)** — the official Gradle wrapper; the three open Android
defects fixed in the backend and bound in the app (capabilities, push registration,
permanent public media); FCM behind build configuration. The Android -> Admin -> Android
hand-off, with real evidence in MinIO's private bucket opened by an operator in the
production Admin, runs green: 7/7 steps, 92 unit tests, 245 backend tests. It found INT-066,
INT-067 and INT-068. The Android build was attempted and fails for a diagnosed network
reason, NETWORK_ENVIRONMENT_FAILURE; P14 to P18 stay `SOURCE_IMPLEMENTED`.

## Next executable phase

The owner's first device smoke with the CI APK (run 35430793735): the app starts, reaches the
local backend over `adb reverse`, shows Raqqa and pharmacies, lists and opens a facility with
its photo, and signs in and out. Full Android Device QA follows only if that passes. The app
now builds on GitHub Actions (`artifacts/evidence/android-ci-build-20260919.txt`).

## Gate status

| Gate | Status |
|---|---|
| `P12 ADMIN FOUNDATION` | CONNECTED_VERIFIED |
| `P13 ADMIN GOLDEN PATH` | CONNECTED_VERIFIED — evidence streaming with real bytes now verified; not raised to CLOSED |
| Android Gradle build | NOT_VERIFIED — NETWORK_ENVIRONMENT_FAILURE |
| Android device | NOT_RUN |
| FCM delivery | EXTERNAL_NOT_VERIFIED |

Evidence streaming is verified end to end (2026-09-19). An owner's document was uploaded to
MinIO's private bucket, opened by an operator in the production Admin build (the owner's
JPEG, typed, `no-store`, audited, with no storage key anywhere) and approved. The same object
refuses an anonymous read.

## Deferred, recorded, not started

P23 to P26 iOS. `DutyShift` field restoration (INT-008). Category `description_ar/en`,
named in `06-DATA-MODEL.md` and absent from the model. `apps/web` HSTS (INT-024) and its
tsconfig rewrite (INT-023). LAUNCH_POLICY_PENDING — the pharmacy verification policy is a
production launch gate; the tool to configure it now exists. Every item carries an INT
identifier in `RECEIPT-AUDIT-2026-09-17.md`.

## Quality debt

`DECISIONS.md` carries the register. DEBT-001 ruff at 99, from 106 at intake. DEBT-002 mypy
at 645 in the backend container, against a 797 baseline measured the same way. DEBT-003 the
deferred `PermissionsMixin` evaluation. The standing rule is no new lint or type debt in a
touched file; every module added since P10 is strict-clean.

## Contract discipline

Django and DRF are the source of truth; `openapi/schema.yaml` is generated from them and
the three clients are generated from it. After any change that touches a request or
response, run `./scripts/generate-openapi.sh` and `./scripts/generate-api-clients.sh` and
commit the regenerated artefacts with the source change. No consumer may hand-write a
transport DTO. See `openapi/README.md` and DECISION-010 onwards.

## Running the Admin golden path

```bash
./scripts/e2e-admin.sh            # resets the test database, starts Django, builds and
                                  # serves the Admin, runs Playwright
cd apps/admin && pnpm test        # Vitest
```

The runner expects the `p10pg` PostGIS and `p10redis` containers on the `p10net` network.

## Parallel open gates

- P19 connected staging, waiting on EXT-004 and EXT-005.
- P20 connected end-to-end, security, load and restore.
- P22 signed Android AAB and Play tracks, waiting on EXT-002 and EXT-003.
