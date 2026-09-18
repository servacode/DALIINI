# HANDOFF

Last updated: 2026-09-18T04:30:00+03:00

## PROJECT SUMMARY

Greenfield Serva Code Directory Platform V3. `docs/spec/` is the verified immutable source of truth. Launch baseline is Raqqa + Pharmacy + Duty with dynamic province/category activation so later categories/provinces do not require client rewrites.

## ARCHITECTURE

Django/DRF/GeoDjango modular monolith; PostgreSQL/PostGIS; Redis cache/Channels/Celery; S3-compatible storage; Next.js Admin/Public Web; Android Kotlin/Compose; iOS SwiftUI; MapLibre; OpenAPI-generated clients.

## CURRENT PHASE

P22 — Android Production is `BLOCKED` on connected release gates and external store/signing credentials. The next independently executable roadmap phase is P23 — iOS Foundation. P10 and P20 also remain open connected gates.

## LAST COMPLETED PHASE

P1 — Design System Foundations (`CLOSED`). P2/P3 are intentionally not called closed because runtime qualification has not executed in this container.

## CURRENT WORKING STATE

P0/P1 gates are closed. P2 remains source-level pending connected backend qualification. P3-P9 are `SOURCE_IMPLEMENTED`. P10 contract tooling exists but real Django-generated schema/TS-Kotlin-Swift clients are still pending. P11 Public Web, P12 Admin Foundation and P13 Admin Operations are `SOURCE_IMPLEMENTED`. P14-P18 Android/mobile source phases are `SOURCE_IMPLEMENTED`. P19 staging source and P20 release-quality source are implemented but their connected gates remain open. P21 Android Play RC source is `SOURCE_IMPLEMENTED` at `9196f4d`; no signed AAB, physical-device, staging, Play-track or production evidence is claimed. P22 is blocked by external release inputs, so P23 iOS Foundation is the next independent implementation phase.

## WHAT IS IMPLEMENTED

- Governance/evidence workflow and design-token generation.
- Django foundation, GeoDjango/PostGIS config, Redis/Channels/Celery/S3 boundaries, production fail-closed settings, request IDs/structured request logs, health/runtime-smoke/CI source.
- Custom UUID User, canonical Syrian phone normalization, OTP abstraction/digest state, registration/login/recovery.
- JOSE/JWT short access tokens + opaque rotating refresh sessions with reuse detection/concurrency grace.
- Session list/revoke/logout-all, blocked-user path, profile GET/PATCH.
- Explicit Admin RBAC models/services independent of future facility membership.
- Redacted append-oriented audit model/service.
- Minimal Province persistence required by User FK.
- Full P4 locations/taxonomy source: City/Neighborhood administrative geometry, dynamic groups/categories, capabilities, province switches, verification requirements, specialties/service tags, safe public selectors/DTOs and audited mutation services.
- Authoritative 14-province seed; Raqqa is the sole active launch province. Health/Pharmacy is the launch taxonomy with Pharmacy public/owner onboarding and Duty enabled only in Raqqa.
- P5 Facility/Owner source: Facility lifecycle, OWNER/MANAGER membership, INITIAL/REVERIFICATION applications, owner/admin APIs, current-policy evidence gating, public-image/private-evidence storage boundaries and safe image processing.

- P11 Public Web source with RTL legal/support/delete-account surfaces and web security headers.
- P12 Admin foundation with RTL staff shell, server-only BFF boundary, HttpOnly refresh-cookie policy and central `can(permission)`.
- P13 Admin Operations backend/API + route surfaces: review/evidence decisions, facilities, users/Admin roles, taxonomy, provinces, verification, ads, audit, analytics, typed settings and system status.
- DRF Bearer access-token authentication bound to explicit Admin RBAC permission codes; user blocking revokes active refresh sessions.
- Audit records now carry redacted before/after snapshots and request IDs; evidence streaming is permission-gated, audited and private/no-store.

## WHAT IS VERIFIED

- P0/P1 executed gates.
- P2/P3 source syntax/AST/YAML/whitespace/governance/design regression.
- P3 pure Python phone-normalization executable smoke.
- Static persisted-secret invariants for OTP/refresh source.
- P4 governance/design regression, compileall/AST, line-length, launch-seed and taxonomy privacy/invariant source checks.
- P5 governance/design regression, full backend compileall/AST, P5 line-length, storage-key DTO privacy and image preload-limit source checks.
- P13 source qualification: 189 backend Python AST files, line-length, 11 executable source-contract tests, governance, design-token drift, Admin route/import/CSS-token checks and whitespace all passed.

## WHAT IS NOT VERIFIED

- No installed Django/DRF runtime in the current container.
- No ruff/mypy/pytest/Django migration drift run.
- No live PostgreSQL/PostGIS, Redis, Celery or S3-compatible runtime qualification.
- P4/P5 Django migration/model/API/PostGIS/S3 runtime tests are not executed.
- No Admin runtime/typecheck/build/Playwright verification; Node 24/pnpm are unavailable here.
- No Android/iOS device verification, no V3 staging runtime verification, and no production verification.

## WHAT IS LEFT

P20 through P26, plus P10 real schema/client generation and connected/runtime qualification of source-implemented backend/web/admin/mobile/staging phases as soon as the V3 remote, toolchains and external services are available.

## KNOWN ISSUES

- `uv.lock` cannot be generated here because PyPI DNS resolution fails and required packages are not cached.
- Current container Node is 22.16.0 while project baseline is Node 24; pnpm is absent.
- Hand-authored migrations must be checked by `makemigrations --check --dry-run` before any connected gate can pass.

## EXTERNAL BLOCKERS

See `BLOCKERS.md`: final domain, owned external provider credentials, store/signing credentials, missing V3 GitHub remote, and paid staging provisioning approval. Local missing runtime/network is documented as an execution limitation, not mixed into the external blocker register.

## IMPORTANT DECISIONS

- DECISION-001: shared typography metrics.
- DECISION-002 / ADR-001: P3 identity/session/RBAC architecture.
- DECISION-003: minimal Province persistence introduced in P3 only for FK integrity; P4 owns full locations/taxonomy behavior.
- DECISION-004: City/Neighborhood optional administrative geometry uses SRID 4326 MultiPolygon.

## REPOSITORY STRUCTURE

Greenfield monorepo matching `docs/spec/26-REPOSITORY-STRUCTURE.md`: `apps/`, `packages/`, `openapi/`, `infrastructure/`, `docs/`, `artifacts/evidence/`, `.github/workflows/`, and root governance files.

## BACKEND STATUS

P2 connected qualification pending. P3-P9 and the P13 Admin backend operations are `SOURCE_IMPLEMENTED`. P10 generated contract remains `IN_PROGRESS`. No backend phase is promoted to connected/staging status from source inspection alone.

## DATABASE STATUS

PostGIS configuration/migration source exists; no live DB in the current environment. Current source includes User/Admin RBAC/session/audit, locations/taxonomy, facility/application/evidence, availability/duty, ratings, content/notifications/analytics and typed `PlatformSetting` persistence. P13 adds Admin permission seed data and Audit before/after/requestId fields. All migrations still require Django `makemigrations --check` and live PostgreSQL/PostGIS execution.

## ADMIN STATUS

P12 and P13 are `SOURCE_IMPLEMENTED`. Routes exist for login/dashboard/reviews/facilities/users/taxonomy/provinces/verification/ads/audit/analytics/settings/system. Backend Admin APIs and permission catalog exist. Data/action binding intentionally waits on the P10 generated TypeScript client; Node 24/pnpm typecheck/build and Playwright golden paths remain pending.

## ANDROID STATUS

P14/P15/P16 `SOURCE_IMPLEMENTED`. Native Kotlin/Compose; applicationId `com.servacode.directory`; minSdk 24; compile/target 36. Foundation, public discovery/account/ratings, and owner onboarding/manage/duty source exist. Owner backend dependency restoration, Photo Picker/private evidence boundaries, per-day hours, closures/managers and native MapLibre owner picker are source-qualified. P10 client binding and all Gradle/device gates remain pending. P17 navigation and P18 mobile live-data hardening are SOURCE_IMPLEMENTED; road/device and connected Channels/FCM qualification remain pending. P19 staging is next.

## IOS STATUS

NOT_STARTED. Native Swift/SwiftUI; bundle ID baseline `com.servacode.directory`.

## MAPS STATUS

P17 `SOURCE_IMPLEMENTED`: MapLibre native route rendering, configurable OSRM/Nominatim-compatible provider boundaries, foreground location stream, navigation state engine, reroute/arrival handling and Arabic TTS exist. Production provider connectivity and real road/device qualification remain pending.

## REALTIME STATUS

P8 backend and P18 Android are `SOURCE_IMPLEMENTED`: Channels invalidation envelope, after-commit publishing, province/user/admin scopes, WSS post-connect auth, foreground/network-aware reconnect, event dedupe and REST-refetch invalidation paths exist. Redis/Channels/FCM delivery is not connected-verified.

## SECURITY STATUS

Security source includes Argon2 configuration, OTP/refresh digest protections, short access tokens, Bearer DRF authentication, explicit Admin RBAC, blocked-user session revocation, audit redaction, private evidence no-store streaming, and secret-safe system status. Runtime IDOR/auth/session/concurrency suites remain pending.

## STAGING STATUS

P19 is `SOURCE_IMPLEMENTED` at commit `1cc6cf1`. `render.yaml` defines isolated V3 staging API/worker/PostgreSQL 17+PostGIS/persistent Key Value/public web/admin; staging inherits production fail-closed settings; Docker sources, smoke and deploy/rollback/backup/restore/DNS/TLS/monitoring runbooks exist. Connected Render inventory was inspected and contains only V2 resources, which were left untouched. No V3 GitHub repository exists in the connected account, and paid staging provisioning was not performed without financial approval; therefore `P19 STAGING RUNTIME PASS` is not claimed.

## PRODUCTION STATUS

NOT_STARTED. Production settings fail closed for DB, Redis, storage, auth secrets, OTP provider, hosts and trusted proxy hops.

## GOOGLE PLAY STATUS

P21 is `SOURCE_IMPLEMENTED` at `9196f4d`. Target SDK 36 policy baseline, fail-closed release/signing validation, Data Safety inventory, app-content checklist, Arabic listing baseline, account-deletion surfaces, Internal/Closed testing runbook and evidence template are present. `P21 PLAY RC PASS` is NOT claimed: no signed AAB, Play upload, device/staging verification, submitted Data Safety/app-content forms or production rollout has occurred. P22 remains blocked by EXT-002/EXT-003 and the open connected quality/staging gates. Re-check current Play policy again at actual submission time.

## CURRENT ENVIRONMENT

Python 3.13.5; uv 0.10.0; Node 22.16.0; no locally installed pnpm/Gradle/Android SDK/Docker/PostgreSQL/Redis/MinIO; package-registry DNS unavailable. Render and GitHub connectors are available, but the accessible remote infrastructure currently contains V2 resources only and no V3 GitHub repository.

## IMPORTANT COMMANDS

- `python infrastructure/scripts/qualify-staging-source.py`
- `STAGING_API_ORIGIN=https://... infrastructure/scripts/staging-smoke.sh`
- `node scripts/check-governance.mjs`
- `node packages/design-tokens/scripts/validate.mjs`
- `node packages/design-tokens/scripts/generate.mjs --check`
- `cd apps/backend && uv lock && uv sync --dev`
- `uv run ruff check .`
- `uv run mypy .`
- `uv run pytest`
- `uv run python manage.py check --settings=directory_backend.settings.test`
- `uv run python manage.py makemigrations --check --dry-run --settings=directory_backend.settings.test`
- `uv run python ../../infrastructure/scripts/backend-runtime-smoke.py`

## HOW TO RUN BACKEND

With Docker/network available, follow `infrastructure/docker/README.md`, run migrations, start Daphne and a Celery worker, then execute the connected runtime smoke.

## HOW TO RUN ADMIN

When Node 24 and pnpm are available: install the workspace, generate/bind the P10 TypeScript API client, configure `ADMIN_API_ORIGIN`, then run `pnpm --filter @servacode/admin dev`. Before qualification run Admin lint/typecheck/build and Playwright golden paths.

## HOW TO RUN ANDROID

With Gradle 9.6+, JDK 17+, Android SDK 36 and ADB available: run the Android workspace build/lint/unit/Compose tests, then instrumentation/device qualification. Current container lacks Gradle/SDK/ADB.

## HOW TO RUN TESTS

Static/source checks work in the current container. Full backend qualification requires dependency installation and PostGIS/Redis/S3/Celery according to CI/`infrastructure/docker/README.md`.

## HOW TO CONTINUE

Start by reading `plan.md`, `PROJECT-STATUS.md`, `BLOCKERS.md` and the immutable `docs/spec/` source. P22 Android Production must not advance without P20 connected quality, signing credentials, owned Play access, staging/device evidence and actual store actions. Continue independent engineering with P23 iOS Foundation. Preserve the P10 generated-client boundary and do not hand-author transport DTOs. Re-run governance/design/source regressions after shared changes.

## LATEST COMMIT SHA

`9196f4d` — P21 Android Play Release Candidate source implementation. A documentation/handoff commit follows this implementation commit in the final package.


## WORKSPACE RECOVERY

On 2026-09-17 the mounted source tree was found missing while management documents remained. The latest extractable local snapshot ended at P2. Required P3-P5 dependency source was reconstructed from the immutable V3 specification and recorded status/handoff before P6 continued. Historical SHAs in status remain evidence of prior work but are not ancestors of the new local recovery repository. Treat new recovery commits as the executable lineage until the original Git repository becomes available.


## P7 PUBLIC DISCOVERY STATUS

Source implemented: public province/city/category metadata, Home, public facility list/detail, PostGIS distance ordering, bbox markers, search, specialty/service filters, SQL availability filters and one-rating-per-user/facility. Static qualification evidence: `artifacts/evidence/p7-public-discovery-source-20260917.txt`. Connected Django/PostGIS/API qualification remains pending.


## P8 REALTIME STATUS

Source implemented with minimal invalidation events, post-connect token auth, province/user/admin scopes, domain RBAC admin gating and `transaction.on_commit` publishing. Evidence: `artifacts/evidence/p8-realtime-source-20260917.txt`. Redis/Channels connected qualification remains pending.


## P9 CONTENT SERVICES STATUS

Source implemented: first-party advertisements with scoped scheduling and validated actions, public ad DTO/Home integration, notification records, encrypted+digested push tokens, Celery retry boundary, FCM interface, APNs placeholder and privacy-minimized analytics registry with retention. Evidence: `artifacts/evidence/p9-content-services-source-20260917.txt`. Connected Django/Celery/FCM qualification remains pending; production provider credentials are external blocker EXT-002.


## P10 CONTRACT STATUS

Tooling foundation exists for canonical Django-generated OpenAPI, schema hashing, drift CI and generated TS/Kotlin/Swift packages. No manual `schema.yaml` was created. Real schema/client generation remains pending because the current environment cannot install/import Django dependencies and OpenAPI Generator is not installed. Evidence: `artifacts/evidence/p10-contract-tooling-source-20260917.txt`.


## P11 PUBLIC WEB STATUS

Source implemented under `apps/web`: Next.js RTL landing/privacy/terms/support/delete-account pages consuming shared Serva design tokens with security headers and env-only domain/contact values. Evidence: `artifacts/evidence/p11-public-web-source-20260917.txt`. Runtime build/E2E are pending because this container has Node 22 and no pnpm while baseline requires Node 24.


## P12 ADMIN FOUNDATION STATUS

Source implemented under `apps/admin`: RTL staff shell, shared tokens, permission-aware UI primitive, server-only backend boundary, HttpOnly/SameSite refresh-cookie helpers, same-origin mutation guard and security headers. Evidence: `artifacts/evidence/p12-admin-foundation-source-20260917.txt`. Actual login/refresh and typed API binding wait on P10 generated contracts and backend runtime.


## P13 ADMIN OPERATIONS STATUS

Source implemented at `ff60f96`: explicit permission catalog, Admin DRF endpoints for all required operational domains, transactional review decisions with evidence recheck, private evidence streaming/audit, facility/user/RBAC/taxonomy/province/verification/ad/settings mutations, analytics/audit/system reads, and matching RTL Next.js route surfaces. Evidence: `artifacts/evidence/p13-admin-operations-source-20260917.txt`. Golden-path PASS remains pending generated client binding, Django/PostgreSQL runtime and Playwright.


## Latest continuation — 2026-09-17 19:55 +03:00

CURRENT PHASE: P21 — Android Play Release Candidate.
LAST COMPLETED SOURCE PHASE: P20 — release-quality harness/source qualification.
LATEST IMPLEMENTATION COMMIT: `5e58355`.

P20 IMPORTANT:
- Auth/account endpoints missing from the recovered tree were restored from the official V3 specification.
- Local source/security qualification passes.
- `P20 RELEASE QUALITY PASS` is still open because connected backend/admin/Android/staging/load/restore gates have not run.
- Use `python infrastructure/quality/release_quality.py --require-connected` at RC; it must return zero before claiming P20 PASS.

P21 NEXT:
- Prepare Play RC artifacts/policy data and source validation.
- Do not claim signed AAB, Play Internal/Closed testing or production submission without actual account/signing/staging/device evidence.


## FINAL CLAUDE HANDOFF SNAPSHOT — 2026-09-17

- P21 implementation commit: `9196f4d860ff88880f785233d31ac6d275576f58`.
- P21 source evidence: `artifacts/evidence/p21-play-rc-source-20260917.txt`.
- P21 state: `SOURCE_IMPLEMENTED`; Play RC gate remains open.
- P22 state: `BLOCKED` on external/connected release inputs.
- Next independently executable phase: P23 iOS Foundation.
- Final distribution archive includes a Git bundle for complete transferable history; no secrets/signing keys are intentionally included.


## RECEIPT AUDIT AND FIX-P0 — 2026-09-17T22:15:00+03:00

### Verification baseline changed

`RECEIPT-AUDIT-2026-09-17.md` is the accepted verification baseline for this project. A gate is PASS only
with a real command exit code. Textual source assertions of the form `assert "x" in source` no longer
qualify a gate on their own. That method is what allowed four P0 defects to pass every gate from P2 to P21.

Package integrity was confirmed: 582/582 files identical to the handoff zip, 5/5 package checksums and
35/35 specification checksums OK, and HEAD matches the expected `bc12f4df93c8893b2cae47bcd92624809d3e5c4b`.

### Resume point corrected

The previous handoff nominated P23 iOS Foundation as the next independently executable phase while also
requiring that API integration stay behind the P10 generated-client boundary. Those two instructions were
incompatible: the generated schema carries no models and the Android client boundaries throw on every call.
The owner accepted the correction on 2026-09-17. **iOS is deferred. P10 precedes P23.**

### FIX-P0 completed

Four P0 defects closed, plus one of the same class that surfaced behind FIX-004:

- FIX-001 `directory_backend/asgi.py` — `get_asgi_application()` now runs before Channels routing is
  imported. Previously daphne aborted with `AppRegistryNotReady`, so the service had never started in any
  environment, including the Dockerfile CMD, `compose.yml` and `render.yaml`.
- FIX-002 `directory_backend/settings/base.py` — removed `django.contrib.messages` and `MessageMiddleware`.
  They were installed without session middleware, so `FallbackStorage` raised `ImproperlyConfigured` on
  every request and all endpoints returned 500. A repository-wide scan first confirmed zero usage of Django
  messages, no `django.contrib.admin`, no `TEMPLATES`, no `render()` and no session usage anywhere.
- FIX-003 `sessions/migrations/0002_rotation_security_fields.py` — dependency now uses the real app label
  `directory_sessions`. Previously the whole migration graph failed to load and no database could be created.
- FIX-004 `build-logic/src/main/kotlin/serva.android.compose.gradle.kts` and FIX-004b
  `core/designsystem/build.gradle.kts` — migrated to the real AGP 9.4 API, verified against the resolved
  AGP jar with `javap`. AGP was not downgraded and Kotlin, Compose and SDK policy were not changed.

### Backend runtime status

Executed against PostgreSQL 17.5 with PostGIS, from an image built out of the fixed source with the
unmodified `apps/backend/Dockerfile` and its unmodified CMD:

- `manage.py check`, `showmigrations` and `migrate` all exit 0; all 30 migrations apply.
- daphne starts and logs `Listening on TCP address 0.0.0.0:8000`.
- `/health/live/` 200, `/health/ready/` 200 with database and redis both reporting ok.
- `/api/v1/public/provinces/` 200, `/api/v1/public/facilities/` 400, `/api/v1/admin/dashboard/` 403.
- No infrastructure 500 remains. The single remaining 500 is `/api/v1/owner/facilities/` for an
  unauthenticated caller, which is INT-010 at the application layer and is scheduled for the next batch.

`P2 BACKEND FOUNDATION CONNECTED PASS` is **NOT** claimed. `makemigrations --check --dry-run` still exits 1
on INT-009 drift, and `uv run pytest` still exits 2 on the INT-028 collection failure.

### Android status

`gradle :app:assembleDebug` previously stopped at `:build-logic:compileKotlin`. It now compiles and jars
`build-logic` and configures all 27 modules, then fails at dependency resolution.

That failure is an **environment limitation, not a project defect**: Google Maven does not serve this
machine. Artifacts that certainly exist return 404, including `androidx.annotation:annotation:1.0.0` and
`com.android.tools.build:gradle:9.4.0` whose jar is present in the local Gradle cache, while Maven Central
returns 200. `:app:assembleDebug` is therefore `NOT_VERIFIED`. Android stays below `BUILD_VERIFIED` and
`DEVICE_VERIFIED` is not claimed.

A `--refresh-dependencies` diagnostic run invalidated cached AGP plugin metadata, so while Google Maven
stays unreachable the build now stops earlier, at plugin resolution. That is local Gradle cache state, not
a change in project source, and it clears once `dl.google.com` is reachable again.

### Environment correction

The environment described in the earlier handoff no longer applies. This workstation has Python 3.13, uv,
Node 24.17.0, pnpm 10.17.1, Docker 29.5.2, PostgreSQL 17.5 with PostGIS, Android SDK 36, JDK 25 and Gradle
9.6.0. `uv.lock` and `pnpm-lock.yaml` were generated successfully, which closes the lockfile item recorded
earlier under known issues. GDAL is absent natively on Windows, so GeoDjango runs through Docker.

### Open internal defects

The full register with severities, introducing phase and reproduction is in `RECEIPT-AUDIT-2026-09-17.md`.
The ones that gate the next steps are INT-009 and INT-028 for P2, then INT-005 for P10, and INT-010,
INT-006, INT-007 and INT-008 behind those.

### How to continue

Read `RECEIPT-AUDIT-2026-09-17.md` in full, then `plan.md`. Close P2 before anything else, then P10.
Do not start iOS. Do not claim any gate without a command exit code.


## P2 CONNECTED QUALIFICATION — 2026-09-17T23:55:00+03:00

### Gate achieved

`P2 BACKEND FOUNDATION CONNECTED PASS` is achieved and P2 is recorded as `CONNECTED_VERIFIED`.
Every criterion was met with a real command exit code, on PostgreSQL 17.5 with PostGIS, from an image
built out of the source with the unmodified `apps/backend/Dockerfile` and its unmodified CMD:

`check`, `showmigrations`, `migrate` and `makemigrations --check --dry-run` all exit 0, with
convergence holding on a second pass; `uv run pytest` reports 49 passed and exits 0; `/health/live/`
returns 200; `/health/ready/` returns 200 with database and redis both ok; `/public/provinces/` 200,
`/public/facilities/` 400, `/admin/dashboard/` 403. No infrastructure 500 remains.

`CONNECTED_VERIFIED` is not phase closure. Connected Celery, S3 and Channels qualification have not
run, and security, load and restore belong to P20.

### What was closed

**INT-009 migration drift.** The drift was larger than first reported: seven apps and thirteen
operations, not six. It collapsed to four justified operations across two apps.

- Nine cosmetic `RenameIndex` were removed by pinning the existing database index names in model state
  (DECISION-006). Eight indexes keep their exact name. One could not: `facilities__prov_cat_status_idx`
  is 31 characters and `Index.max_name_length` is 30, so Django rejects it with `models.E034`; it was
  renamed to `facility_prov_cat_status_idx`, the only unavoidable rename.
- One destructive `RemoveConstraint` was refused. `uniq_submitted_application_per_facility_kind`
  enforces a `06-DATA-MODEL` invariant; `FacilityApplication` simply had no `Meta`. The constraint is
  now declared in model state with the identical name, fields and condition (DECISION-007). The
  database and the migration were not touched.
- The perpetual GiST drop-and-recreate was traced to a duplicate `TstzRange` class declared inside
  `pharmacy_duty/migrations/0001_initial` (DECISION-008). `TstzRange.deconstruct()` round-trips
  perfectly; the earlier hypothesis about unstable deconstruction was wrong. `BaseExpression.identity`
  begins with `self.__class__`, so two identical expressions from two different classes never compare
  equal. The migration now imports the shared helper. Zero SQL.
- The three `accounts` `AlterField` were accepted only after `sqlmigrate` rendered `-- (no-op)` for all
  three (DECISION-009).

**INT-028 pytest collection.** Three missing `tests/__init__.py` markers were added; all twelve test
packages now have one. The official command collects and runs.

**INT-007 test database engine.** `settings/test.py` no longer pins SQLite; it inherits PostgreSQL +
PostGIS from `base.py`. The suite could never have passed on SQLite, which fails on `geo_db_type`.

**INT-030, found by running the suite on PostgreSQL for the first time.** `submit_facility` locked with
`select_for_update()` across a LEFT OUTER JOIN produced by `select_related("category__capabilities")`,
a reverse one-to-one. PostgreSQL refuses `FOR UPDATE` on the nullable side of an outer join, so
`POST /api/v1/owner/facilities/{id}/submit/` would have failed at runtime in every environment. Fixed
with `select_for_update(of=("self",))`; no semantic change.

**The invariant qualifier was hardened.** It used to search for the constraint name inside a migration
file, which can never fail, because a migration keeps its historic `AddConstraint` forever. It now
parses the model with `ast` and asserts the constraint is declared in `FacilityApplication.Meta`.
Negative-tested: removing the constraint from model state makes it exit 1. Four connected tests cover
the rest, including that the autodetector does not propose removal and that the database itself refuses
the violating row.

### Upgrade safety

An existing database was built with the pre-batch code, seeded, then upgraded. Exactly two migrations
applied. Row counts identical. Every `pg_class.oid` preserved, including the GiST exclusion constraint
at 27009 and the invariant partial unique index at 26314; the renamed index kept oid 26124, so it was
renamed in place and not rebuilt.

### Corrections to earlier records

The audit stated that the `PermissionsMixin` fields "were never migrated". That was wrong: they are
present in `accounts/0001_initial`, and only their field definition had drifted. The audit also
reported six drifting apps; the correct number is seven, `analytics` having been lost to output
truncation.

### Still open

- INT-010 `/api/v1/owner/facilities/` returns 500 for an unauthenticated caller. Deferred by decision.
- INT-029, new: a Redis connection error propagates after the transaction has committed, because
  `publish_after_commit` guards `channel_layer is None` but not a connection failure.
  `05-SYSTEM-ARCHITECTURE §13` asks for safe degradation.
- INT-005 the generated OpenAPI schema carries no component schemas. This is the P10 blocker.
- INT-006 no province or taxonomy seed exists.
- INT-008 `DutyShift` is missing `created_by`, `cancelled_at` and `ended_early_at`.

### Quality debt

`DECISIONS.md` now carries a register. DEBT-001 ruff at 104 issues, reduced from a 106 baseline.
DEBT-002 mypy at 556, untouched. DEBT-003 the deferred `PermissionsMixin` removal evaluation, with its
six preconditions. All three are mandatory before staging or production closure and none blocks P10.
The standing rule is no new lint or type debt in any file that is touched.

### Android

Unchanged and still `SOURCE_IMPLEMENTED`. Google Maven does not serve this machine, which is an
`ENVIRONMENT_LIMITATION`, not an internal defect. Do not downgrade AGP, change Compose or AndroidX
versions, change SDK policy, or use an untrusted mirror. `BUILD_VERIFIED` requires
`gradle :app:assembleDebug` to exit 0 from a network that reaches the official Google Maven or a
trusted artifact proxy the project owns.

### Next

**P10 Contracts.** It must precede all further client work: the schema carries 72 paths with zero
component schemas and zero request bodies, so Admin, Android and iOS are all blocked behind it. iOS
stays deferred.


## P10 OPENAPI CONTRACTS RECOVERY — 2026-09-18T02:40:00+03:00

### The blocker is cleared

The generated contract went from 84 operations with **zero** component schemas, **zero**
request bodies, **zero** response schemas and no security scheme, to 140 component schemas,
35 request bodies, 72 response schemas, an explicit `bearerAccessToken` scheme and 84
hand-declared operation ids, with **zero generator errors and zero warnings** where there
had been 328 and 76.

All three clients generate from it: TypeScript 138 models and 23 API classes, Kotlin 335
sources, Swift 172 sources. Admin, Android and iOS are no longer blocked behind a DTO-less
boundary, and no consumer needs to hand-write a transport DTO.

### How to keep it honest

Django and DRF are the source of truth. After any change that touches a request or
response:

```bash
./scripts/generate-openapi.sh        # regenerate schema.yaml and schema.sha256
./scripts/generate-api-clients.sh    # regenerate the three clients
```

Commit the regenerated artefacts with the source change. `./scripts/check-openapi-drift.sh`
and the CI `contract-drift` job fail otherwise. Everything under `openapi/` and
`packages/api-*/generated/` is a build artefact; hand-editing any of it is a defect.

Generation needs the GeoDjango native libraries because the models are spatial. CI installs
them; the backend container already has them.

### Five defects found and fixed while writing the contract

Three of them meant the authentication surface had never worked in any environment. None
could have been caught by the existing tests, which assert on source text.

- **INT-033** — registration completion, login and password reset all returned 500. The
  views pass `**validated_data` into services while the serializers declare camelCase
  fields with no `source`, so the keyword names never matched. Fixed with `source`; the
  wire contract is unchanged.
- **INT-031** — `Province` filtered on `is_active` at three sites; the field is `active`.
  Registration start, registration completion and the profile province change returned 500.
- **INT-034** — `facility_detail` ordered business hours by `sequence`, which does not
  exist; it is `sort_order`. Every owner facility detail, create, update and relocate
  response returned 500.
- **INT-010** — owner, media and rating views declared no `permission_classes`. Now
  `IsAuthenticated`, and the anonymous caller gets 403 rather than 500.
- **INT-016** — the interactive schema route was public everywhere. Now `public` in
  development, admin-only in staging and not routed in production.

### Three divergences recorded, not silently reconciled

The contract describes the runtime, because a generated client is built from the document
and a schema that describes the specification instead would produce clients that break on
the first response. See DECISION-012.

- **INT-035** — DRF cursor pagination emits `{next, previous, results}`;
  `08-API-CONTRACT.md` specifies `{items, nextCursor, hasMore}`.
- **INT-036** — several Admin list endpoints answer from `QuerySet.values()` and return
  snake_case keys while the hand-built payloads are camelCase; the specification requires
  one convention at the boundary.
- **INT-037** — `06-DATA-MODEL.md` names the BusinessHour ordering field `sequence`; the
  model calls it `sort_order`. The runtime name was kept.

Reconciling these is product work with its own review, not something to slip into a schema
batch.

**Closed 2026-09-18 by the CONTRACT ALIGNMENT batch**, together with INT-038, which had no
identifier of its own until the canonical register was written. The runtime was changed to
match the specification and the artefacts regenerated. See the section below.

### Verification levels, stated precisely

- Schema generation, contract description, canonical artefacts, drift gate, 12 contract
  tests and a 17-case runtime smoke against PostGIS and Redis: all **PASS**.
- TypeScript client: generated **and compiled**, `tsc --noEmit` exits 0.
- Kotlin client: generated. Compilation **NOT_VERIFIED** — `ENVIRONMENT_LIMITATION`. The
  Gradle distribution download from services.gradle.org resets on this network, the same
  condition that blocks Google Maven. The client needs only Maven Central, so it should
  compile from a network that can reach the Gradle services.
- Swift client: generated as a contract artefact. No Swift toolchain here, and **iOS work
  is not started**. A generated Swift client is not the beginning of P23.

### Next

**Admin data binding, P11 to P13.** The Admin is still a descriptive scaffold:
`backendRequest`, `setRefreshCookie` and `getRefreshCookie` exist but are never called, and
every page renders a static `OperationPage` naming the endpoint it would call. None of the
operational components the design system requires exist yet, and neither Playwright nor
Vitest is installed, so `P13 ADMIN GOLDEN PATH PASS` currently has no runner.

Start with the login and refresh BFF against `authLogin` and `authRefresh`, and fix INT-012
while doing it: the `__Host-` refresh cookie sets `secure` only in production, and browsers
reject a `__Host-` cookie without `Secure`, so the development flow cannot work today.

Android and iOS remain where they were. `/public/provinces/` still returns an empty list
because the P4 seed does not exist.


## CONTRACT ALIGNMENT — 2026-09-18

The API boundary conventions are now settled and enforced by tests. Anything built on top
of this API can rely on them.

**Errors.** Every failing request returns `{code, message, details, requestId}`. Branch on
`code`, never on `message`. `details` is always a `{field path: [message, ...]}` map, empty
when the error is not field-scoped; nested paths look like `contacts[1].phone` and anything
without a field of its own is under `nonFieldErrors`. `requestId` equals the
`X-Request-ID` response header and the id in the server logs, so it is the right thing to
quote in a bug report. An unexpected failure returns `INTERNAL_ERROR` and nothing else —
no exception text, no stack frame, no SQL, no connection string.

Server-side: raise `core.exceptions.DomainError` or `ConflictError`. Never return an error
`Response` by hand — it bypasses the envelope and it does not roll the transaction back.

**Cursor pages.** `{items, nextCursor, hasMore}`. `nextCursor` is an opaque token; send it
back unchanged as `?cursor=` and never parse it. There is no `previous`. A malformed cursor
is a 400 `VALIDATION_ERROR` on the `cursor` field. New paginated endpoints subclass
`core.pagination.CursorPage` and set only `ordering`, which must end in a unique column.

**Casing.** camelCase everywhere, including the Admin endpoints that read through
`QuerySet.values()`. A wire name that differs from a column is translated with `source=` in
the serializer. Do not rename a column to change a wire name. The business-hour ordering
field is `sequence` on the wire and `sort_order` in the database.

One exception remains: `AdminCapabilitiesRequestSerializer` and
`AdminCapabilitiesSerializer` still use `supports_*` on both request and response. That is
INT-039. It is not a list endpoint, so it was left out of this batch deliberately rather
than widened into it without review.

**Regenerate after any request or response change.** `./scripts/generate-openapi.sh` then
`./scripts/generate-api-clients.sh`, and commit the artefacts with the source change.
`ApiError` replaced `DetailError`, `DomainError` and `DomainErrorBody` in all three
clients, so a consumer that referenced those types will need updating.

`/public/provinces/` still returns an empty list. The P4 seed (INT-006) does not exist yet.
