# HANDOFF

Last updated: 2026-09-17T18:31:00+03:00

## PROJECT SUMMARY

Greenfield Serva Code Directory Platform V3. `docs/spec/` is the verified immutable source of truth. Launch baseline is Raqqa + Pharmacy + Duty with dynamic province/category activation so later categories/provinces do not require client rewrites.

## ARCHITECTURE

Django/DRF/GeoDjango modular monolith; PostgreSQL/PostGIS; Redis cache/Channels/Celery; S3-compatible storage; Next.js Admin/Public Web; Android Kotlin/Compose; iOS SwiftUI; MapLibre; OpenAPI-generated clients.

## CURRENT PHASE

P17 — Maps / Navigation (`IN_PROGRESS`). P10 remains independently `IN_PROGRESS` until Django can generate the real OpenAPI schema and clients.

## LAST COMPLETED PHASE

P1 — Design System Foundations (`CLOSED`). P2/P3 are intentionally not called closed because runtime qualification has not executed in this container.

## CURRENT WORKING STATE

P0/P1 gates are closed. P2 remains source-level pending connected backend qualification. P3-P9 are `SOURCE_IMPLEMENTED`. P10 contract tooling exists but real Django-generated schema/TS-Kotlin-Swift clients are still pending. P11 Public Web, P12 Admin Foundation and P13 Admin Operations are `SOURCE_IMPLEMENTED`. P13 implementation commit is `ff60f96`; its static gate passed, but Node 24/pnpm, Django/PostgreSQL runtime and Playwright golden paths were not available, so `P13 ADMIN GOLDEN PATH PASS` is not claimed. P14, P15 and P16 Android source are `SOURCE_IMPLEMENTED`; P17 Maps/Navigation is active.

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
- No Android, iOS, staging or production runtime/device verification.

## WHAT IS LEFT

P17 through P26, plus P10 real schema/client generation and connected/runtime qualification of source-implemented backend/web/admin/mobile phases as soon as capable toolchains are available.

## KNOWN ISSUES

- `uv.lock` cannot be generated here because PyPI DNS resolution fails and required packages are not cached.
- Current container Node is 22.16.0 while project baseline is Node 24; pnpm is absent.
- Hand-authored migrations must be checked by `makemigrations --check --dry-run` before any connected gate can pass.

## EXTERNAL BLOCKERS

See `BLOCKERS.md`: final domain, production provider credentials, and store/signing credentials. Local missing runtime/network is documented as an execution limitation, not mixed into the external blocker register.

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

P14/P15/P16 `SOURCE_IMPLEMENTED`. Native Kotlin/Compose; applicationId `com.servacode.directory`; minSdk 24; compile/target 36. Foundation, public discovery/account/ratings, and owner onboarding/manage/duty source exist. Owner backend dependency restoration, Photo Picker/private evidence boundaries, per-day hours, closures/managers and native MapLibre owner picker are source-qualified. P10 client binding and all Gradle/device gates remain pending. P17 navigation source is active.

## IOS STATUS

NOT_STARTED. Native Swift/SwiftUI; bundle ID baseline `com.servacode.directory`.

## MAPS STATUS

NOT_STARTED beyond Geo/PostGIS foundation. MapLibre provider/client work remains later roadmap phases.

## REALTIME STATUS

P8 `SOURCE_IMPLEMENTED`: Channels invalidation envelope, after-commit publishing and province/user/admin scopes. P13 extended invalidation to category/group/verification/province configuration changes. Redis/Channels delivery is not connected-verified.

## SECURITY STATUS

Security source includes Argon2 configuration, OTP/refresh digest protections, short access tokens, Bearer DRF authentication, explicit Admin RBAC, blocked-user session revocation, audit redaction, private evidence no-store streaming, and secret-safe system status. Runtime IDOR/auth/session/concurrency suites remain pending.

## STAGING STATUS

NOT_STARTED.

## PRODUCTION STATUS

NOT_STARTED. Production settings fail closed for DB, Redis, storage, auth secrets, OTP provider, hosts and trusted proxy hops.

## GOOGLE PLAY STATUS

NOT_STARTED. Target SDK/policy release work remains roadmap P21/P22 and must be rechecked at submission time.

## CURRENT ENVIRONMENT

Python 3.13.5; uv 0.10.0; Node 22.16.0; no pnpm; OpenJDK 21; no Docker/PostgreSQL/Redis/MinIO; PyPI DNS unavailable.

## IMPORTANT COMMANDS

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

Continue P17 from `plan.md`: implement production-configurable RoutingProvider/GeocodingProvider, OSRM/Nominatim-compatible adapters, turn-by-turn state engine and Arabic TTS while preserving PostGIS geo truth and foreground-only location. Do not promote navigation beyond source status without Gradle/device plus real road verification.

## LATEST COMMIT SHA

`682218a` — P16 Android Owner implementation commit. Management-document commit follows this handoff update.


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
