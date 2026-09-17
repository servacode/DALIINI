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
