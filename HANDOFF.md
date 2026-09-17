# HANDOFF

Last updated: 2026-09-17T16:18:00+03:00

## PROJECT SUMMARY

Greenfield Serva Code Directory Platform V3. `docs/spec/` is the verified immutable source of truth. Launch baseline is Raqqa + Pharmacy + Duty with dynamic province/category activation so later categories/provinces do not require client rewrites.

## ARCHITECTURE

Django/DRF/GeoDjango modular monolith; PostgreSQL/PostGIS; Redis cache/Channels/Celery; S3-compatible storage; Next.js Admin/Public Web; Android Kotlin/Compose; iOS SwiftUI; MapLibre; OpenAPI-generated clients.

## CURRENT PHASE

P10 — OpenAPI generated clients (`IN_PROGRESS`).

## LAST COMPLETED PHASE

P1 — Design System Foundations (`CLOSED`). P2/P3 are intentionally not called closed because runtime qualification has not executed in this container.

## CURRENT WORKING STATE

P0/P1 gates are closed. P2 foundation source is substantially implemented but exact `uv.lock` and connected/tooling qualification remain unavailable because the container cannot resolve PyPI or run Docker/PostGIS/Redis/MinIO. P3 Accounts/Auth/RBAC and P4 Locations/Taxonomy remain `SOURCE_IMPLEMENTED`. P5 Facility/Owner is also `SOURCE_IMPLEMENTED` at `11aa673`, including lifecycle, membership, applications, media/evidence privacy and reverification source. P6 Availability/Duty, P7 Public Discovery/Search/Geo/Ratings, P8 Realtime and P9 Content Services are `SOURCE_IMPLEMENTED`; P10 Contracts is now active.

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

## WHAT IS VERIFIED

- P0/P1 executed gates.
- P2/P3 source syntax/AST/YAML/whitespace/governance/design regression.
- P3 pure Python phone-normalization executable smoke.
- Static persisted-secret invariants for OTP/refresh source.
- P4 governance/design regression, compileall/AST, line-length, launch-seed and taxonomy privacy/invariant source checks.
- P5 governance/design regression, full backend compileall/AST, P5 line-length, storage-key DTO privacy and image preload-limit source checks.

## WHAT IS NOT VERIFIED

- No installed Django/DRF runtime in the current container.
- No ruff/mypy/pytest/Django migration drift run.
- No live PostgreSQL/PostGIS, Redis, Celery or S3-compatible runtime qualification.
- P4/P5 Django migration/model/API/PostGIS/S3 runtime tests are not executed.
- No Admin, Android, iOS, staging or production runtime/device verification.

## WHAT IS LEFT

P10 through P26, plus connected qualification of P2-P9 as soon as a capable environment is available.

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

P2 `IN_PROGRESS`; P3 `SOURCE_IMPLEMENTED`; P4 `SOURCE_IMPLEMENTED`; P5 `SOURCE_IMPLEMENTED`; P6 `IN_PROGRESS`.

## DATABASE STATUS

PostGIS configuration/migrations source exists; no live DB in current environment. P3 introduces User/OTP/RBAC/session/audit tables. P4 adds City/Neighborhood and taxonomy tables plus deterministic province/taxonomy seed. P5 adds Facility, FacilityMembership, FacilityApplication, FacilityPublicImage and VerificationEvidence.

## ADMIN STATUS

NOT_STARTED. Backend RBAC primitives exist; custom Next.js Admin remains P12/P13.

## ANDROID STATUS

NOT_STARTED. Native Kotlin/Compose; applicationId baseline `com.servacode.directory`.

## IOS STATUS

NOT_STARTED. Native Swift/SwiftUI; bundle ID baseline `com.servacode.directory`.

## MAPS STATUS

NOT_STARTED beyond Geo/PostGIS foundation. MapLibre provider/client work remains later roadmap phases.

## REALTIME STATUS

Channels/Redis foundation source exists; domain realtime events are P8.

## SECURITY STATUS

P3 security source implements Argon2 configuration, OTP digest-only storage, JWT access, opaque refresh HMAC digests, reuse detection, blocked-user denial, throttling and audit redaction. Runtime security suites are pending.

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

Not available; P12 not started.

## HOW TO RUN ANDROID

Not available; P14 not started.

## HOW TO RUN TESTS

Static/source checks work in the current container. Full backend qualification requires dependency installation and PostGIS/Redis/S3/Celery according to CI/`infrastructure/docker/README.md`.

## HOW TO CONTINUE

Continue P6 at source level from `plan.md`, keeping availability/duty logic centralized. Do not mark P2/P3/P4/P5/P6 CLOSED from source inspection. When a capable environment becomes available: generate/commit `uv.lock`, run ruff/mypy/pytest/Django migration checks, run connected smoke, fix root causes, then update evidence/status before advancing gate state.

## LATEST COMMIT SHA

260444f17c6af5ffe8a9daa9a0ae95dd17c98799


## WORKSPACE RECOVERY

On 2026-09-17 the mounted source tree was found missing while management documents remained. The latest extractable local snapshot ended at P2. Required P3-P5 dependency source was reconstructed from the immutable V3 specification and recorded status/handoff before P6 continued. Historical SHAs in status remain evidence of prior work but are not ancestors of the new local recovery repository. Treat new recovery commits as the executable lineage until the original Git repository becomes available.


## P7 PUBLIC DISCOVERY STATUS

Source implemented: public province/city/category metadata, Home, public facility list/detail, PostGIS distance ordering, bbox markers, search, specialty/service filters, SQL availability filters and one-rating-per-user/facility. Static qualification evidence: `artifacts/evidence/p7-public-discovery-source-20260917.txt`. Connected Django/PostGIS/API qualification remains pending.


## P8 REALTIME STATUS

Source implemented with minimal invalidation events, post-connect token auth, province/user/admin scopes, domain RBAC admin gating and `transaction.on_commit` publishing. Evidence: `artifacts/evidence/p8-realtime-source-20260917.txt`. Redis/Channels connected qualification remains pending.


## P9 CONTENT SERVICES STATUS

Source implemented: first-party advertisements with scoped scheduling and validated actions, public ad DTO/Home integration, notification records, encrypted+digested push tokens, Celery retry boundary, FCM interface, APNs placeholder and privacy-minimized analytics registry with retention. Evidence: `artifacts/evidence/p9-content-services-source-20260917.txt`. Connected Django/Celery/FCM qualification remains pending; production provider credentials are external blocker EXT-002.
