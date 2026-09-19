# Evidence Ledger

Evidence entries record commands actually executed. A gate is never marked PASS from source inspection alone.

| Timestamp | Gate | Commit | Environment | Command/Check | Result | Artifact |
|---|---|---|---|---|---|---|
| 2026-09-17T08:28:00+03:00 | Intake integrity | UNBORN_HEAD | local container | `sha256sum -c SHA256SUMS.txt` on handoff | PASS (all listed files) | source console output |
| 2026-09-17T08:28:00+03:00 | P0 GOVERNANCE PASS | UNBORN_HEAD | local container | governance validator + JSON parse + whitespace + implementation framework/secret-file hygiene | PASS | `artifacts/evidence/p0-governance-20260917.txt` |
| 2026-09-17T08:28:00+03:00 | P1 DESIGN TOKENS PASS | 871d4c7 | local container | token validation + contrast + generated drift + governance regression + whitespace | PASS | `artifacts/evidence/p1-design-tokens-20260917.txt` |
| 2026-09-17T08:28:00+03:00 | P2 source qualification (not gate closure) | 9c4031c | local container | Python compile + TOML/source checks + whitespace; dependency install attempted | SOURCE PASS / CONNECTED NOT VERIFIED | `artifacts/evidence/p2-backend-source-20260917.txt` |

| 2026-09-17T08:59:49+03:00 | P3 source qualification (not gate closure) | f9408d8 | local container | governance + design drift + compileall + AST + line-length + secret-persistence invariants + phone executable smoke + YAML + whitespace | SOURCE PASS / RUNTIME NOT VERIFIED | `artifacts/evidence/p3-auth-rbac-source-20260917.txt` |

Future machine-readable evidence is stored under `artifacts/evidence/` with secrets redacted.
| 2026-09-17T16:33:17+03:00 | P13 Admin Operations source qualification (not golden-path closure) | ff60f96 | local container | 189-file backend AST + P13 line length + 11 source-contract checks + governance + design drift + Admin routes/imports/token refs + whitespace | SOURCE PASS / RUNTIME+PLAYWRIGHT NOT VERIFIED | `artifacts/evidence/p13-admin-operations-source-20260917.txt` |

| 2026-09-17T17:45:00+03:00 | P13 Admin Operations recovery qualification | 507814d | local container | 184 backend AST + 7 source-contract + Admin CSS/import + governance/design drift + whitespace | SOURCE PASS / RUNTIME+PLAYWRIGHT NOT VERIFIED | `artifacts/evidence/p13-admin-operations-recovery-20260917.txt` |
| 2026-09-17T17:58:00+03:00 | P14 Android Foundation source qualification | 4c0a693 | local container | 27 modules + SDK/security/Keystore/Mutex/RTL/architecture/hygiene + governance/design drift/line length/whitespace | SOURCE PASS / GRADLE+DEVICE NOT VERIFIED | `artifacts/evidence/p14-android-foundation-source-20260917.txt` |
| 2026-09-17T18:31:00+03:00 | P15 Android Public source qualification | 7178ba5 | local container | P14/P15 qualifiers + governance/design regression + token drift + whitespace | SOURCE PASS / GRADLE+DEVICE NOT VERIFIED | `artifacts/evidence/p15-android-public-source-20260917.txt` |
| 2026-09-17T18:44:51+03:00 | P16 Android Owner source qualification | 682218a | local container | backend owner qualifier + P14/P15/P16 qualifiers + governance/design/token drift + whitespace + executable DutyValidator smoke | SOURCE PASS / DJANGO+GRADLE+DEVICE NOT VERIFIED | `artifacts/evidence/p16-android-owner-source-20260917.txt` |
| 2026-09-17T19:35:00+03:00 | P19 staging source qualification (not runtime gate closure) | 1cc6cf1 | local container + connected Render/GitHub inventory | P19 source qualifier + shell syntax + backend compileall + secret-marker scan + governance/design regression + whitespace; external inventory verified V2-only Render resources and no V3 GitHub repo | SOURCE PASS / STAGING RUNTIME NOT RUN | `artifacts/evidence/p19-staging-source-20260917.txt` |


## P20 — Release quality source qualification
- Evidence: `artifacts/evidence/p20-release-quality-source-20260917.txt`
- Machine report: `artifacts/evidence/quality/p20-local.json`
- Strict fail-closed report: `artifacts/evidence/quality/p20-connected-required.json`
- Commit: `5e58355`
- Result: SOURCE_IMPLEMENTED / local-source-qualified; connected gate not passed.

| 2026-09-17T20:27:00+03:00 | P21 Play RC source qualification (not Play gate closure) | 9196f4d | local container | P21 qualifier + P14 Android source regression + governance + design validation/drift + whitespace | SOURCE PASS / AAB+DEVICE+STAGING+PLAY NOT VERIFIED | `artifacts/evidence/p21-play-rc-source-20260917.txt` |


## 2026-09-17 — Receipt audit accepted as the verification baseline

The full receipt and verification audit is recorded in `RECEIPT-AUDIT-2026-09-17.md` and was accepted by the project owner as the new verification baseline. From this point a gate is PASS only with a real command exit code; textual source assertions no longer qualify a gate on their own.

| Timestamp | Gate | Commit | Environment | Command/Check | Result | Artifact |
|---|---|---|---|---|---|---|
| 2026-09-17T21:00:00+03:00 | Receipt audit — package integrity | bc12f4d | Windows 11 host | `diff -rq` zip vs tree, `sha256sum -c PACKAGE-SHA256SUMS.txt`, `sha256sum -c docs/spec/SHA256SUMS.txt` | PASS — 582/582 files identical, 5/5 and 35/35 checksums OK | `RECEIPT-AUDIT-2026-09-17.md` |
| 2026-09-17T21:00:00+03:00 | Receipt audit — executed toolchain | bc12f4d | Docker + PostGIS 17.5, Node 24.17.0, Gradle 9.6.0 | ruff, mypy, pytest, Django check/migrate, Next build, Gradle assembleDebug, spectacular | FAIL — four P0 defects found that all prior textual gates had passed | `RECEIPT-AUDIT-2026-09-17.md` |

## 2026-09-17 — FIX-P0 runtime restoration

| Timestamp | Gate | Commit | Environment | Command/Check | Result | Artifact |
|---|---|---|---|---|---|---|
| 2026-09-17T22:15:00+03:00 | FIX-001 ASGI boot | FIX-P0 working tree | Docker image from unmodified `apps/backend/Dockerfile` | `daphne -b 0.0.0.0 -p 8000 directory_backend.asgi:application` | PASS — `Listening on TCP address 0.0.0.0:8000` | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | FIX-002 request path | FIX-P0 working tree | same | `GET /health/live/`, `/health/ready/`, `/api/v1/public/provinces/`, `/api/v1/public/facilities/`, `/api/v1/admin/dashboard/` | PASS — 200 / 200 / 200 / 400 / 403, no infrastructure 500 remains | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | FIX-003 migration graph | FIX-P0 working tree | PostgreSQL 17.5 + PostGIS | `manage.py check`, `manage.py showmigrations`, `manage.py migrate` | PASS — exit 0, all 30 migrations applied, graph valid | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | Migration drift | FIX-P0 working tree | PostgreSQL 17.5 + PostGIS | `manage.py makemigrations --check --dry-run` | FAIL — exit 1, drift in accounts, business_hours, content_services, facilities, notifications, pharmacy_duty (INT-009, out of scope) | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | Backend test suite | FIX-P0 working tree | PostgreSQL 17.5 + PostGIS | `uv run pytest` | FAIL — exit 2, collection aborts on duplicate test module basenames (INT-028, out of scope) | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | FIX-004 / FIX-004b Android build-logic | FIX-P0 working tree | Gradle 9.6.0, JDK 25, Android SDK 36 | `gradle :app:assembleDebug` | PARTIAL — `:build-logic:compileKotlin` and `:build-logic:jar` PASS, all 27 modules configured, then dependency resolution fails | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | `gradle :app:assembleDebug` | FIX-P0 working tree | Gradle 9.6.0, JDK 25, Android SDK 36 | `gradle :app:assembleDebug` | NOT_VERIFIED — environment limitation: Google Maven does not serve this machine; `androidx.annotation:annotation:1.0.0` and `com.android.tools.build:gradle:9.4.0` return 404 while Maven Central returns 200 | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | Post-fix regression set | FIX-P0 working tree | local host | governance, design-token validate, design-token drift, P19 staging qualifier, five Android source qualifiers | PASS — all exit 0 | `artifacts/evidence/fixp0-runtime-20260917.txt` |
| 2026-09-17T22:15:00+03:00 | Lint baseline | FIX-P0 working tree | local host | `uv run ruff check .` | 106 errors, identical to the pre-fix baseline — no regression introduced | `artifacts/evidence/fixp0-runtime-20260917.txt` |

`P2 BACKEND FOUNDATION CONNECTED PASS` is NOT claimed: migration drift and test collection remain open. Android remains below `BUILD_VERIFIED`; `DEVICE_VERIFIED` is not claimed.


## 2026-09-17 — P2 connected qualification

| Timestamp | Gate | Commit | Environment | Command/Check | Result | Artifact |
|---|---|---|---|---|---|---|
| 2026-09-17T23:55:00+03:00 | Migration drift analysis | 189a500 | PostgreSQL 17.5 + PostGIS | `makemigrations --dry-run --verbosity 3`, nothing written | 13 operations across 7 apps, one of them a destructive `RemoveConstraint` on a specification invariant | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Expression identity probe | P2 tree | Docker | deconstruct round-trip and model-vs-migration-state comparison | `TstzRange` round-trip STABLE; the drift came from a duplicate class declared in the migration, not from deconstruction | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Expected SQL review | P2 tree | PostgreSQL 17.5 + PostGIS | `sqlmigrate accounts 0005`, `sqlmigrate facilities 0004` | accounts renders `-- (no-op)` three times; facilities renders one `ALTER INDEX ... RENAME TO` | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | INT-009 migration drift | P2 tree | PostgreSQL 17.5 + PostGIS | `makemigrations --check --dry-run` | PASS — exit 0, and exit 0 again on a second pass; drift reduced from 13 operations to 4 justified ones | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Fresh database sequence | P2 tree | PostgreSQL 17.5 + PostGIS | `check`, `migrate`, `makemigrations --check`, `migrate`, `makemigrations --check` | PASS — all five exit 0 | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Existing database upgrade path | 189a500 → P2 tree | PostgreSQL 17.5 + PostGIS | seed with pre-batch code, upgrade, compare `pg_class.oid` and row counts | PASS — two migrations applied, row counts identical, every oid preserved, GiST constraint and the invariant index untouched | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Business invariants, connected | P2 tree | PostgreSQL 17.5 + PostGIS | five write attempts against the real database | PASS — duplicate SUBMITTED rejected, DRAFT accepted, different kind accepted, overlapping duty rejected, adjacent duty accepted | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Hardened invariant qualifier | P2 tree | local host | `scripts/qualify-owner-source.py`, plus a negative test | PASS — exit 0 declared, exit 1 when the constraint is removed from model state; the previous text-only check passed in both cases | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | INT-028 / INT-007 test suite | P2 tree | PostgreSQL 17.5 + PostGIS | `uv run pytest`, official command, no import-mode workaround | PASS — 49 passed, exit 0, from a run that previously collected nothing | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | INT-030 owner submit lock | P2 tree | PostgreSQL 17.5 + PostGIS | `pytest facilities/tests/test_owner_api.py` | FIXED — `FOR UPDATE cannot be applied to the nullable side of an outer join`; resolved with `select_for_update(of=("self",))` | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | `P2 BACKEND FOUNDATION CONNECTED PASS` | P2 tree | final image, unmodified Dockerfile CMD, no mounts | daphne boot plus six live endpoints | **PASS** — `/health/live/` 200, `/health/ready/` 200 with database and redis ok, `/public/provinces/` 200, `/public/facilities/` 400, `/admin/dashboard/` 403; the only 500 is INT-010 | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Post-batch regressions | P2 tree | local host | governance, design-token validate, design-token drift, P19 staging qualifier, hardened owner qualifier | PASS — all exit 0 | `artifacts/evidence/p2-connected-20260917.txt` |
| 2026-09-17T23:55:00+03:00 | Lint and type debt | P2 tree | local host | `uv run ruff check .` | 104 errors against a 106 baseline — debt reduced, none added; mypy untouched at 556 | DEBT-001, DEBT-002 in `DECISIONS.md` |

`P2 BACKEND FOUNDATION CONNECTED PASS` is achieved and P2 is recorded as `CONNECTED_VERIFIED`. That is not phase closure: connected Celery, S3 and Channels qualification remain, and security, load and restore belong to P20. INT-010 and INT-029 stay open by decision.


## 2026-09-18 — P10 OpenAPI contracts recovery

| Timestamp | Gate | Commit | Environment | Command/Check | Result | Artifact |
|---|---|---|---|---|---|---|
| 2026-09-18T02:40:00+03:00 | Schema baseline | 06fac0a | backend container | `manage.py spectacular` plus metrics | Recorded — 72 paths, 84 operations, **0** component schemas, **0** request bodies, **0** response schemas, no security scheme, 328 errors, 76 warnings | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | API inventory | 06fac0a | backend container | URL-conf introspection of every routed operation | 87 operations classified by domain, auth requirement and parameters | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | Schema generation | P10 tree | backend container | `manage.py spectacular` | **PASS** — exit 0 with **0 errors and 0 warnings**; 140 component schemas, 35 request bodies, 72 response schemas, 84 unique operation ids | `openapi/schema.yaml` |
| 2026-09-18T02:40:00+03:00 | Determinism | P10 tree | backend container | two independent generations, byte comparison | **PASS** — identical; sha256 `c739f4e7cc18655a2b6b3e4ab213527764a928bc4f5008f1d60661de7c08a4f4` | `openapi/schema.sha256` |
| 2026-09-18T02:40:00+03:00 | Authentication contract | P10 tree | backend container | schema inspection | **PASS** — `bearerAccessToken` http/bearer declared; 71 operations require it, 8 explicitly open, 5 declare no authentication classes | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | Contract tests | P10 tree | PostgreSQL 17.5 + PostGIS | `pytest core/tests/test_openapi_contract.py` | **PASS** — 12/12 | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | Runtime contract smoke | P10 tree | PostgreSQL 17.5 + PostGIS, Redis | 17 real requests, each body validated against the declared response schema | **PASS** — 17/17, covering auth, public list and detail, owner mutation, admin mutation, validation failure and permission failure | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | TypeScript client | P10 tree | Node 24, TypeScript 5.9 | `openapi-generator 7.15.0` then `tsc --noEmit` | **PASS** — 138 models, 23 API classes, compiles with exit 0 | `packages/api-typescript/generated` |
| 2026-09-18T02:40:00+03:00 | Kotlin client | P10 tree | Gradle wrapper 8.7 | `openapi-generator 7.15.0`, then `gradlew compileKotlin` | GENERATED — 335 sources. Compilation **NOT_VERIFIED**, `ENVIRONMENT_LIMITATION`: the Gradle distribution download from services.gradle.org resets, the same condition that blocks Google Maven | `packages/api-kotlin/generated` |
| 2026-09-18T02:40:00+03:00 | Swift client | P10 tree | — | `openapi-generator 7.15.0` | GENERATED — 172 sources, contract artefact only. No Swift toolchain on this host and iOS is not started | `packages/api-swift/generated` |
| 2026-09-18T02:40:00+03:00 | Drift gate | P10 tree | local host | `check-openapi-drift.sh` rewritten | **PASS** by construction — the previous gate diffed an untracked file and could never fail; the new one requires tracked files, compares blob hashes across regeneration and verifies the digest independently | `scripts/check-openapi-drift.sh` |
| 2026-09-18T02:40:00+03:00 | Backend suite | P10 tree | PostgreSQL 17.5 + PostGIS | `uv run pytest` | **PASS** — 61 passed, from 49 | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | Regressions | P10 tree | local host | governance, design-token validate and drift, P19 staging qualifier, hardened owner qualifier | **PASS** — all exit 0 | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | Lint debt | P10 tree | local host | `uv run ruff check .` | 101 errors against a 104 baseline — reduced, none added; every P10 file passes cleanly | DEBT-001 |
| 2026-09-18T02:40:00+03:00 | INT-033 authentication surface | P10 tree | PostgreSQL 17.5 + PostGIS | runtime smoke | **FIXED** — registration completion, login and password reset returned 500 through a camelCase/snake_case keyword mismatch | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | INT-031 province lookup | P10 tree | PostgreSQL 17.5 + PostGIS | runtime smoke | **FIXED** — three sites filtered `Province` on `is_active`; the field is `active` | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | INT-034 owner facility detail | P10 tree | PostgreSQL 17.5 + PostGIS | runtime smoke | **FIXED** — business hours ordered by a field that does not exist | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | INT-010 owner authentication | P10 tree | PostgreSQL 17.5 + PostGIS | contract test plus runtime smoke | **FIXED** — 403 instead of 500 for an anonymous caller | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T02:40:00+03:00 | INT-016 schema exposure | P10 tree | local host | settings review | **FIXED** — public in development, admin-only in staging, not routed in production | `artifacts/evidence/p10-contracts-20260918.txt` |
| 2026-09-18T04:30:00+03:00 | Backend suite | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS | `uv run pytest` | **PASS** — 133 passed, from 61 | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | New contract suites | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS | `pytest` on the four new modules in isolation | **PASS** — 72 passed | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | INT-038 error envelope | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS | 17 cases, including three through the real URL conf | **FIXED** — one `{code, message, details, requestId}` envelope for validation, authentication, permission, not found, method, media type, conflict, throttling and unexpected failure; `requestId` proven equal to the `X-Request-ID` header; a connection string, exception class and traceback proven absent from a 500 body | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | INT-035 cursor envelope | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS | 6 cases over an 11-row collection at page size 4 | **FIXED** — `{items, nextCursor, hasMore}`; first, middle and last page correct; the token contains no scheme, host, path or query parameter; a full walk returns all 11 rows exactly once in `(name_ar, id)` order; a mangled cursor answers 400 `VALIDATION_ERROR` with `details.cursor`, not 404 | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | INT-036 Admin casing | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS | 6 connected endpoint cases plus a structural sweep of every response serializer | **FIXED** — eight `values()` endpoints now answer in camelCase through serializers; no column renamed; a sweep of eight Admin routes finds no snake_case key outside the opaque `metadata`, `snapshot` and setting `value` payloads | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | INT-037 hours ordering field | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS | 5 cases in both directions | **FIXED** — `sequence` on the wire, `sort_order` in the database, asserted still present on the model; a full PUT round trip writes 0 and 1 to the column and reads back `sequence` | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | Runtime contract smoke | CONTRACT ALIGNMENT tree | PostgreSQL 17.5 + PostGIS, Redis | 17 real requests validated against the regenerated schema | **PASS** — 17/17, including the 400, 401 and 403 cases that now carry the new envelope | artifacts/evidence/contract-alignment-20260918.txt |
| 2026-09-18T04:30:00+03:00 | Schema generation | CONTRACT ALIGNMENT tree | backend container | `manage.py spectacular` | **PASS** — exit 0, zero warnings; two independent generations give the same digest `b59b38c76a13e0fc1d0bc30975cd187778677ff62355988282c8b0ffe0553a8f` | `openapi/schema.sha256` |
| 2026-09-18T04:30:00+03:00 | Generated clients | CONTRACT ALIGNMENT tree | openapi-generator 7.15.0 pinned image | regenerate all three; `tsc --noEmit` on TypeScript | **PASS** — TypeScript 136 models compiles exit 0; Kotlin 331 sources; Swift 170 sources; `ApiError` present in all three and `DetailError`/`DomainError`/`DomainErrorBody` removed from all three | `packages/api-*/generated` |
| 2026-09-18T04:30:00+03:00 | Lint debt | CONTRACT ALIGNMENT tree | backend container | `uv run ruff check .` | 101 errors, unchanged from the baseline measured the same way; every new file passes cleanly | DEBT-001 |
| 2026-09-18T04:30:00+03:00 | Type debt | CONTRACT ALIGNMENT tree | backend container | `uv run mypy .` | 789 errors against a 797 baseline measured the same way on a detached worktree at `c9a4cae`; **reduced, none added**; zero findings in all six new modules. The previously recorded 556 was a host-side under-count and is corrected in DEBT-002 | DEBT-002 |
| 2026-09-18T07:00:00+03:00 | INT-006 launch seed | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | `migrate` on a fresh database, then read the tables | **FIXED** — 14 provinces, 1 group, 5 categories, 5 capability rows, 70 province switches; exactly one switch public and one open to onboarding, both Raqqa plus pharmacies | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Deterministic identity | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | UUIDv5 comparison across a fresh seed and an upgraded database | **PASS** — every canonical row carries the same id in both; Raqqa is `58fea422-2cae-5f93-a396-b81a9a7d37e2`. A row under the right code with the wrong id raises `REFERENCE_ID_MISMATCH` and writes nothing | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Idempotency | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | `seed_launch_baseline` twice, then `--check` | **PASS** — both runs and the check report `created 0` for every entity; counts, ids and flags identical across runs | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Admin authority preserved | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | activate Aleppo, set its sort order to 99, switch Raqqa clinic on and Raqqa pharmacy onboarding off, then re-run the seed | **PASS** — all four operator changes survive the re-run. The seed applies launch defaults at creation only | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Missing-row recovery | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | delete a canonical province, run `--check`, then run the command | **PASS** — `--check` reports 6 missing rows and writes nothing; the command restores the province at the same id `4aad580a-3dfb-5ffe-a3a9-d3eb16f89fe9` with `active=False` and its 5 switches | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Fresh database qualification | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | `check`, `migrate`, `makemigrations --check --dry-run`, `pytest` | **PASS** — no issues, no changes detected, 158 passed from 133 | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Existing database upgrade | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | rewind a fully migrated database to directory 0002, add operator rows, apply 0003 and 0004 | **PASS** — 10/10 checks; the operator's legacy province, category and switch come through untouched and the canonical ids match a fresh seed exactly | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | INT-040 specialization | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | `sqlmigrate directory 0003` | **FIXED** — the model declared DOCTOR, NURSING and MEDICAL_SUPPLIES against a specification that names MEDICAL_CLINIC and NURSING_CENTER. `sqlmigrate` reports `(no-op)`: `choices` is not a PostgreSQL constraint and no row existed anywhere | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | API runtime evidence | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS | live requests against a freshly migrated database | **PASS** — `/public/provinces/` serves Raqqa and is no longer empty; Raqqa's public taxonomy serves pharmacies only; an inactive province answers 404; owner onboarding is offered for Raqqa plus pharmacies and refused for every other pair | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Runtime contract smoke | LAUNCH BASELINE tree | PostgreSQL 17.5 + PostGIS, Redis | 17 real requests, now against the seeded canonical rows | **PASS** — 17/17. The smoke no longer builds a parallel taxonomy; it uses the real Raqqa and pharmacy rows | artifacts/evidence/launch-baseline-20260918.txt |
| 2026-09-18T07:00:00+03:00 | Contract regeneration | LAUNCH BASELINE tree | openapi-generator 7.15.0 pinned image | regenerate schema and clients after the enum correction; `tsc --noEmit` | **PASS** — `CategorySpecializationEnum` is now GENERIC/PHARMACY/MEDICAL_CLINIC/NURSING_CENTER; digest `fc7bbe8206e44c889a60779b23f4ce84de0cd1e9d25c70ab5c34cefa5ae85dfd`; TypeScript compiles exit 0 | `openapi/schema.sha256` |
| 2026-09-18T07:00:00+03:00 | Quality debt | LAUNCH BASELINE tree | backend container | `ruff check .`, `mypy .` | ruff 100 against a 101 baseline; mypy 789 against 797 across 205 source files, up from 191. **Reduced on both, none added**; zero findings in every module this batch adds | DEBT-001, DEBT-002 |
| 2026-09-18T07:00:00+03:00 | Deliberate omissions | LAUNCH BASELINE tree | — | dataset review | No city, neighborhood, boundary or coordinate — no authoritative dataset exists. No `VerificationRequirement` — **LAUNCH_POLICY_PENDING**, a production launch gate, not a development blocker. No facility, user, rating, duty shift, opening hour, advertisement or test phone | DECISION-020 |
| 2026-09-19 | Admin golden path | P11-P13 tree | Playwright, Chromium, production build + Django + PostGIS + Redis | — | **PASS** — 31/31 in 58s | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Admin unit suite | P11-P13 tree | Vitest, node and jsdom projects | — | **PASS** — 71/71 | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Backend regression | P11-P13 tree | `check`, `makemigrations --check`, `pytest` on PostGIS | — | **PASS** — no issues, no changes, 211 passed from 158 | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Contract | P11-P13 tree | spectacular, drift gate, client regeneration | — | **PASS** — 91 operations, 143 components, zero warnings, no schema or client drift | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Admin build | P11-P13 tree | `next build`, bundle search, tsconfig diff | — | **PASS** — exit 0, no warnings, no server secret in the client bundle, no tsconfig drift | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | INT-045 CSP | P11-P13 tree | served login page | — | **FIXED** — all ten script tags carry the request nonce; the console hydrates | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | INT-046 login GET | P11-P13 tree | server-rendered HTML + Playwright | — | **FIXED** — form posts, submit disabled until hydration | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Quality debt | P11-P13 tree | ruff, mypy in the backend container | — | ruff 100; mypy 783 against 797 — reduced, none added | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Evidence streaming | P11-P13 tree | — | — | NOT_VERIFIED — no object storage in the e2e stack; refusal and 404 verified | `artifacts/evidence/admin-binding-20260919.txt` |
| 2026-09-19 | Android connected data layer | ANDROID-BINDING tree | JVM, generated Kotlin client + NetworkModule wiring → Django + PostGIS + Redis + MinIO | `scripts/e2e-android.sh` | **PASS** — 26/26 (21/26 on the first run; five failures traced to three backend defects and two test assumptions) | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Android unit suite | ANDROID-BINDING tree | `apps/android/jvm-verification`, Maven Central only | `gradle test` | **PASS** — 89/89 in 25 classes | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Generated Kotlin client compile | ANDROID-BINDING tree | Kotlin 2.3.21, serialization 1.9.0, Retrofit 3.0.0, OkHttp 5.3.0 | `gradle compileKotlin` | **PASS** after INT-052 and INT-053; it had never compiled | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Android app build | ANDROID-BINDING tree | AGP 9.4.0 | `gradle :app:assembleDebug` | **NOT_VERIFIED** — ENVIRONMENT_LIMITATION: Google Maven 404 | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Backend regression | ANDROID-BINDING tree | `check`, `makemigrations --check`, `pytest` on PostGIS | — | **PASS** — no issues, no changes, 225 passed from 211 | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Contract | ANDROID-BINDING tree | spectacular, drift, client regeneration | — | **PASS** — 91 operations, 144 components, zero warnings, no schema or client drift | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Admin regression | ANDROID-BINDING tree | Playwright + Vitest + tsc after the session binding | `scripts/e2e-admin.sh` | **PASS** — 31/31, 71/71, exit 0 | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Quality debt | ANDROID-BINDING tree | ruff, mypy in the backend container | — | ruff 99 from 100; mypy 727 from 783 — reduced, none added | `artifacts/evidence/android-binding-20260919.txt` |
| 2026-09-19 | Gradle wrapper (INT-019) | 9afa924 | official Gradle 9.6.0 wrapper | `./gradlew --version` | **PASS** — Gradle 9.6.0; jar and distribution checksums match gradle.org | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Google Maven diagnosis | 9afa924 | this network, no dependency or Gradle change | DNS ×3, proxies, hosts, TLS, curl ×5 edges, controls, `gradlew --info` | NETWORK_ENVIRONMENT_FAILURE — 404 from Google's download server for Android content only; re-checked 01:01 UTC | `artifacts/evidence/google-maven-diagnosis-20260919.txt` |
| 2026-09-19 | Android app build | db7ac98 | AGP 9.4.0 | `./gradlew clean`, `./gradlew :app:assembleLocalDebug` | **NOT_VERIFIED** — exit 1, AGP plugin not resolvable (Google Maven 404); no APK, lint, Compose, instrumentation or device | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Android -> Admin -> Android golden path | db7ac98 | JVM data layer + production Admin in Chromium → Django + PostGIS + Redis + MinIO (public and private buckets) | `scripts/e2e-android.sh` | **PASS** — 7/7 steps, exit 0: connected 27/27, hand-off submit, Admin evidence review and approval, hand-off public, public 200 / private 403. First run 1/7: found INT-068 | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Evidence streaming | db7ac98 | real bytes from MinIO's private bucket through the Admin BFF | Playwright `handoff.spec.ts` | **PASS** — owner's JPEG, image/jpeg, no-store, nosniff, audited per document, no key in headers, page or data (INT-066, INT-067) | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Android unit suite | db7ac98 | `apps/android/jvm-verification` | `../gradlew -p . test --rerun-tasks` | **PASS** — 92/92 | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Backend regression | db7ac98 | `check`, `makemigrations --check`, `pytest` on PostGIS | — | **PASS** — no issues, no changes, 245 passed | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Contract | a4b1256 | spectacular + three clients regenerated through Docker | — | **PASS** — five requirement-id fields to integer (INT-068); regeneration byte-identical | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Admin regression | db7ac98 | Playwright + Vitest + tsc + eslint | `scripts/e2e-admin.sh` | **PASS** — 31 passed, 1 skipped (the hand-off spec runs in the Android runner), 71/71, exit 0 | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | Quality debt | db7ac98 | ruff, mypy in the backend container | — | ruff 99, unchanged; mypy 645 from 727 — reduced, none added | `artifacts/evidence/android-golden-path-20260919.txt` |
| 2026-09-19 | FCM delivery | db7ac98 | — | — | EXTERNAL_NOT_VERIFIED — no Firebase project exists | `artifacts/evidence/android-golden-path-20260919.txt` |

`P10 CONTRACT PASS` is achieved for schema generation, contract description, committed
canonical artefacts, the drift gate, contract tests and runtime conformance. Kotlin client
compilation and any Swift build remain unverified for environment reasons and are recorded
as such rather than claimed.

The CONTRACT ALIGNMENT batch closes INT-035, INT-036, INT-037 and INT-038 with executed
evidence. It does not change any phase status: P10 stays `CONNECTED_VERIFIED` and Admin
data binding has not started.

The LAUNCH BASELINE batch closes INT-006 and INT-040 with executed evidence on both a fresh
and an upgraded database. P4 moves to `CONNECTED_VERIFIED`; it is not `CLOSED`, because the
Admin taxonomy is still read-only (INT-018) and the pharmacy verification policy is still
pending.

The P11–P13 batch qualifies P12 and P13 as `CONNECTED_VERIFIED` on executed evidence in a
real browser. They are not `CLOSED`: the evidence-streaming path with real bytes, and the
production launch gate LAUNCH_POLICY_PENDING, remain.

The ANDROID GOLDEN PATH batch verifies the evidence-streaming path with real bytes from
object storage. P13 is not raised to `CLOSED` on that account: raising it is the owner's
decision, and LAUNCH_POLICY_PENDING remains. P14 to P18 stay `SOURCE_IMPLEMENTED`: every
Android result above is JVM verification, which proves portable and data-layer integration
only, and the Android Gradle build did not run.
