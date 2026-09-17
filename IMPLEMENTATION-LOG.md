# Implementation Log

## 2026-09-17T08:28:00+03:00 — V3 intake and P0 bootstrap

**Goal**

Establish a clean greenfield repository using the V3 handoff as immutable implementation baseline.

**Files created**

Repository structure, root governance files, safe environment examples, CI skeleton, README/SECURITY, and `docs/spec/` copy of the full handoff.

**Files modified**

None from a prior application. This is a new repository.

**Implemented**

- Verified every checksum listed by the handoff.
- Confirmed the all-in-one document embeds the standalone specification files verbatim.
- Initialized Git `main` with an unborn HEAD.
- Declared Python 3.13 and Node 24 targets.
- Added governance validation script and CI skeleton.

**Problem observed**

Local execution image reports Node 22.16.0 and no `pnpm` binary.

**Root cause**

The container toolchain differs from the project baseline; no project code caused this.

**Resolution**

P0 pins Node 24 in `.nvmrc`, `package.json`, and CI. Toolchain alignment will be performed before Node-dependent implementation gates.

**Commands run**

- `unzip` handoff into an isolated inspection directory.
- `sha256sum -c SHA256SUMS.txt`.
- `git init -b main`.
- `git status --short`.
- `git branch --show-current`.
- `git rev-parse HEAD` (expected unborn repository failure recorded as `UNBORN_HEAD`).
- Toolchain version inspection.

**Tests/results**

- Handoff checksum verification: PASS for every listed file.
- P0 repository gate: pending.

**Decision**

No architecture decision beyond the baseline was introduced.

**Next step**

Run P0 governance/hygiene gate, capture evidence, commit, then advance automatically to P1.

## 2026-09-17T08:28:00+03:00 — P0 governance gate

**Goal**

Qualify the repository/governance baseline before implementation phases.

**Commands run**

- `node scripts/check-governance.mjs`
- `python3 -m json.tool docs/spec/BASELINE-DECISIONS.json`
- `git diff --check`
- implementation scan for forbidden React Native/Flutter references
- obvious secret-file hygiene scan

**Tests/results**

All checks passed. Gate result: `P0 GOVERNANCE PASS`.

**Problem/root cause/resolution**

No P0 defect remained after validation.

**Next step**

Commit P0 and proceed automatically to P1 Design System Foundations.

## 2026-09-17T08:28:00+03:00 — P1 Design System Foundations

**Goal**

Create one canonical token source and deterministic platform outputs before feature UI work.

**Files created/modified**

`packages/design-tokens/`, `docs/design/`, CI, governance/evidence files.

**Implemented**

Canonical baseline palette, semantic mappings, typography roles, spacing, radius, restrained elevation/motion, platform generators, font policy and central component specifications.

**Problem**

The approved spec named typography roles but did not assign numeric metrics.

**Root cause**

The design document intentionally provided semantic roles but left concrete type metrics for implementation validation.

**Resolution**

Introduced `DECISION-001` with a conservative Arabic-first Tajawal scale centralized in tokens, preserving later token-level revision.

**Commands/tests**

- token generation
- token validation
- WCAG contrast checks
- generated output drift check
- P0 governance regression
- `git diff --check`

**Results**

`P1 DESIGN TOKENS PASS`. Critical tested contrast ratios are >= 5.60:1.

**Next step**

Commit P1 and proceed to P2 Backend Foundation.

## 2026-09-17T08:28:00+03:00 — P2 Backend Foundation source implementation

**Goal**

Build the backend foundation and attempt the required connected qualification.

**Files created/modified**

`apps/backend/`, `infrastructure/docker/`, `infrastructure/scripts/backend-runtime-smoke.py`, CI and governance/evidence files.

**Implemented**

Django 5.2/Python 3.13 project contract; settings split; GeoDjango/PostGIS DB configuration; Channels ASGI; Redis channel layer; Celery; S3-compatible storage backends; request IDs; JSON logging; liveness/readiness; production fail-closed settings; Docker Compose definitions for PostGIS/Redis/MinIO/backend/worker; connected runtime smoke script; backend CI job.

**Problem**

`uv lock`/`uv sync` could not download dependencies, and the execution container has no Docker/PostgreSQL/Redis/MinIO binaries.

**Root cause**

The current execution environment has unavailable DNS/network access for PyPI and lacks local connected-service tooling. This is not a project code defect and not an external credential blocker.

**Resolution**

Completed the source implementation and connected-test harness without weakening requirements. Ran dependency-free source checks. P2 remains `SOURCE_IMPLEMENTED`; the mandatory connected gate is explicitly not marked PASS.

**Commands/tests**

- attempted `uv lock` / `uv sync --dev` (environment network failure)
- `python3 -m compileall` on backend/runtime smoke source
- TOML/source structure assertions
- `git diff --check`

**Results**

Source checks PASS. `P2 BACKEND FOUNDATION CONNECTED PASS` is NOT achieved.

**Next step**

Continue independent source work where safe, while connected qualification remains pending for an environment with dependency/network/service access.

## 2026-09-17T08:28:00+03:00 — P2 source commit recorded

Backend foundation source was committed as `9848273744715ca1efc7773a6cdcdba2ea9278cc`. The connected gate remains pending and is not represented as PASS.

## 2026-09-17T08:59:49+03:00 — P2 correction + P3 Accounts/Auth/RBAC source implementation

### Goal

Continue automatically from the documented P2 state, re-attempt P2 qualification, then implement P3 source without claiming an unexecuted runtime gate.

### Files created

- `apps/backend/accounts/**`
- `apps/backend/sessions/**`
- `apps/backend/audit/**`
- `apps/backend/locations/**` minimal Province FK dependency
- `apps/backend/core/exceptions.py`
- `apps/backend/core/tasks.py`
- `apps/backend/core/throttling.py`
- `docs/adr/ADR-001-P3-IDENTITY-SESSION-RBAC.md`
- `artifacts/evidence/p3-auth-rbac-source-20260917.txt`

### Files materially modified

- backend settings/env/URLs/pyproject/Dockerfile
- CI workflow
- Docker Compose/runtime smoke
- request logging middleware/formatter
- production settings tests
- governance/status/handoff files

### Implemented

- UUID custom User with canonical `+9639xxxxxxxx` storage and DB check constraint.
- Minimal Province persistence required by the authoritative User FK; full P4 remains deferred.
- Six-digit OTP challenge with digest-only storage, expiry, max attempts and one-time consumption.
- OTP provider abstraction with disabled-by-default provider and deterministic test provider.
- Registration start/verify/complete flow with signed short-lived flow state.
- Login with generic credential failure and dummy-hash path for unknown users.
- Standard JOSE/JWT short-lived access token via PyJWT.
- Opaque random refresh secrets; only HMAC digest stored.
- Atomic refresh rotation, previous-secret concurrency grace and post-grace compromise/revoke.
- Session list/current logout/all logout/IDOR-safe session revoke.
- Recovery start/verify/reset; reset is one-time and revokes sessions.
- Blocked-user rejection and block service session revocation.
- Profile GET/PATCH.
- Explicit Admin Role/Permission/UserRole domain RBAC, separate from future facility OWNER/MANAGER membership.
- Append-oriented audit model + recursive redaction service; role/block changes audited.
- Central DRF auth throttles using Redis-backed Django cache in connected environments.
- CamelCase auth/account API boundary and standard error envelope.
- CI design for PostGIS/Redis/MinIO + Celery connected qualification.

### Problems found

1. Current environment still cannot resolve PyPI and has no connected runtime.
2. P2 staging settings imported `base` after `production`, weakening inherited production settings.
3. Dockerfile contained `uv sync --no-dev || true`, masking dependency-install failure.
4. Built-in `django.contrib.sessions` conflicted with the required custom `sessions` domain and was unnecessary for token auth.
5. OTP failed-attempt updates were saved inside an atomic block and then immediately raised, which would roll the attempt update back.
6. Blocked refresh session revocation had the same raise-inside-atomic rollback problem.
7. Production settings did not explicitly require `DATABASE_URL`, allowing the development default to remain if omitted.
8. Previous runtime smoke did not prove Celery and Compose did not create required S3-compatible buckets.
9. Exact `uv.lock` is still missing because dependency resolution cannot reach PyPI.

### Root cause

Most issues were source-level qualification gaps that could not be discovered by syntax compilation alone; P2 had been described too strongly before connected/tooling qualification. Transaction rollback behavior also required explicit security review of failure paths.

### Resolution

- Corrected staging inheritance and production fail-closed DB/proxy config.
- Removed swallowed install failure.
- Removed unused Django cookie-session/auth middleware/apps; custom sessions remain authoritative.
- Moved security state-changing error raises outside transaction commit boundaries where needed.
- Added MinIO bucket init and Celery task round-trip to runtime smoke.
- Added required structured request fields to production logging.
- Downgraded P2 status to `IN_PROGRESS` until lockfile + connected/runtime gates execute.
- Documented the refresh concurrency design in ADR-001.

### Commands executed

- `git status --short`
- `git branch --show-current`
- `git rev-parse HEAD`
- `uv lock --offline` → failed because required packages were not cached.
- `uv lock` → failed after retries because `pypi.org` DNS cannot resolve.
- governance/design-token validators.
- `python -m compileall`.
- AST/line-length/source-secret invariants.
- pure Python phone-normalization smoke.
- YAML parsing for CI/Compose.
- `git diff --check` / `git diff --cached --check`.

### Tests/results

PASS (actually executed): governance regression, design token validation/drift, Python syntax/AST, source line-length, static secret-persistence invariants, phone normalization smoke, YAML parse, whitespace.

NOT EXECUTED: ruff, mypy, pytest, Django checks/migrations, PostgreSQL/PostGIS, Redis, Celery, S3 runtime. These remain required before P2/P3 gates can close.

### Decisions

- `DECISION-002` + ADR-001: explicit P3 identity/session/RBAC architecture.
- `DECISION-003`: minimal Province persistence is introduced only to satisfy P3 referential integrity; P4 behavior is not claimed.

### Commit

`f9408d88aa07cd6c96ad24913d212ab339005a3f` — `feat: implement identity sessions and rbac source`

### Next

Continue safe source work with P4 Locations/Taxonomy while P2/P3 connected qualification remains pending; do not mark either gate CLOSED until dependencies and runtime tests execute.


## 2026-09-17T09:36:00+03:00 — P4 Locations/Taxonomy source implementation

### Goal

Implement the authoritative locations/taxonomy dependency layer, launch seed and policy invariants without hardcoded client behavior.

### Files created/modified

- `apps/backend/locations/**`
- `apps/backend/directory/**`
- backend installed apps/root URLs
- P3 tests updated to consume the authoritative Raqqa seed rather than creating duplicate Province rows
- governance/evidence files

### Implemented

- City and Neighborhood with optional SRID 4326 administrative boundary geometry.
- Dynamic category groups, categories, per-province switches and one-to-one capabilities.
- Verification requirements, specialties and service tags.
- Exact public and owner-onboarding eligibility selectors.
- Audited mutation services for activation, per-province switches, capabilities and verification policy.
- Safe public Province/City/Category DTOs.
- Seed for all 14 Syrian provinces with Raqqa as the only active launch province.
- Launch Health/Pharmacy taxonomy with Pharmacy public + owner onboarding enabled only in Raqqa and Duty capability enabled.
- Category code/slug immutability.
- Duty-specialization invariant in both directions: Duty cannot be enabled for an unsupported specialization, and a category with Duty enabled cannot be changed to an unsupported specialization.
- Specialty scope invariant rejects missing, dual and empty-string scope.
- Verification file-bound constraint.

### Problems found

1. A category could change away from `PHARMACY` after Duty was already enabled, leaving an invalid persisted capability relationship.
2. `Specialty.specialization=""` could satisfy the DB null/non-null scope check even though it is not a valid specialization.
3. Full Django/runtime qualification remains unavailable because dependencies/services cannot be installed or started in this container.

### Root cause

The initial capability invariant only validated capability writes, not later category specialization changes. The initial specialty DB constraint distinguished NULL from non-NULL but did not reject the empty string.

### Resolution

- Added reverse invariant validation on `DirectoryCategory.clean()`.
- Tightened `Specialty` model validation and DB constraint to require a non-empty specialization scope.
- Added regression tests for both cases.
- Kept P4 at `SOURCE_IMPLEMENTED`; no runtime PASS is claimed.

### Commands/tests executed

- `git diff --check`
- P0 governance regression
- design-token validation and generated drift check
- `python -m compileall -q apps/backend`
- AST parse for 145 Python files
- source line-length gate
- launch seed static invariant check (14 provinces; Raqqa/Pharmacy/Duty)
- taxonomy privacy/invariant source checks
- `uv lock --offline` (failed: Argon2 not cached)
- `uv lock` (failed: PyPI DNS resolution unavailable)

### Results

Source/static qualification passed. Django/pytest/PostGIS runtime qualification was not executable and therefore `P4 TAXONOMY PASS` is NOT claimed.

### Decision

`DECISION-004` records the City/Neighborhood MultiPolygon choice.

### Next step

Commit P4 source and proceed automatically to P5 Facility/Owner source implementation while preserving pending connected gates.


## 2026-09-17T10:02:00+03:00 — P5 Facility/Owner source implementation

### Goal

Implement the authoritative owner/facility domain after P4 without crossing into P6 hours/duty logic, and preserve truthful gate semantics while the connected backend runtime is unavailable.

### Files created

- `apps/backend/facilities/**` models, services, selectors, serializers, views, URLs, migration and tests.
- `artifacts/evidence/p5-owner-domain-source-20260917.txt`.

### Files modified

- backend installed apps, root URLs and media safety environment contract.

### Implemented

- Facility lifecycle states and PostGIS Point location.
- OWNER/MANAGER membership with domain-level last-owner protection and IDOR-safe membership checks.
- INITIAL/REVERIFICATION applications with submitted-state uniqueness and transactional admin review.
- Current-policy submission validation and verification evidence min/max gates.
- Sensitive-edit pending strategy: ACTIVE → REVERIFICATION_REQUIRED without mutating live sensitive values.
- Owner config/facility/location/images/evidence/membership endpoints.
- Admin application review, evidence streaming, suspend/reactivate/close operations with RBAC and audit.
- Public image DTOs expose service routes, never raw storage keys; evidence remains private.
- Image uploads are byte/pixel/edge limited, decoded, re-encoded to JPEG and metadata stripped.
- Storage cleanup covers DB/audit failure after object upload.

### Problems found

1. Initial image implementation loaded pixel data before applying the project pixel/edge limit.
2. Uploaded S3 objects could become orphaned if audit failed after the DB row was created inside the surrounding transaction.
3. Connected Django/PostGIS/S3 execution remains unavailable in this container.

### Root cause

The first two issues were ordering/transaction-boundary problems discovered during security qualification, not spec changes. The third is the existing local execution limitation.

### Resolution

- Apply image dimension limits from decoded headers before `source.load()`.
- Keep storage deletion in the exception path through row creation and audit, while delete operations use `transaction.on_commit`.
- Keep P5 at `SOURCE_IMPLEMENTED`; do not claim the runtime gate.

### Commands/tests

Executed governance validator, design-token validation/drift, full backend `compileall`/AST, P5 line-length check, DTO raw-storage-key source invariant, image preload-limit invariant and `git diff --check`. Authored P5 owner IDOR, last-owner, lifecycle/reverification, evidence-gate and media privacy tests. Django/DRF/PostGIS/Redis/Celery imports remain unavailable, so pytest/migration/concurrency tests were not claimed.

### Commit

`11aa673` — `feat: implement facility owner domain source`

### Next step

P6 Availability/Duty source implementation, then connected qualification later in a capable environment.


## 2026-09-17T13:40:00+03:00 — Workspace recovery + P6 Availability/Duty source

### Goal
Recover the latest available source snapshot without hiding the workspace loss, reconstruct the documented P3-P5 dependency surface from the immutable specification/status records, and complete P6 source implementation.

### Recovery finding
The mounted `directory-platform-v3` directory contained only management documents. The only locally extractable project snapshot was `directory-platform-v3-progress.zip`, which ended at P2. P3-P5 implementation status existed in PROJECT-STATUS/HANDOFF but their source tree was not present in the available snapshot. Recovery therefore rebuilt the required dependency surface from `docs/spec/` and the recorded handoff. No gate was upgraded because of reconstruction.

### P6 implemented
- Weekly BusinessHour model and atomic replacement service.
- Split shifts and overnight intervals; overlap validation across midnight.
- TemporaryClosure CRUD.
- Central `get_facility_availability` / `get_next_open` using Asia/Damascus.
- Priority: TEMP_CLOSED > DUTY > OPEN > CLOSED.
- Pharmacy DutyShift CRUD.
- PostgreSQL exclusion constraint preventing overlapping duty per facility.
- `btree_gist` extension migration before the exclusion constraint.
- Owner/manager membership check on every owner hours/closure/duty endpoint.
- Runtime-oriented tests authored for priority, overnight hours and DB overlap.

### Qualification
Static source qualification passed. Django/PostGIS runtime remains unavailable; P6 is `SOURCE_IMPLEMENTED`, not closed. Evidence: `artifacts/evidence/p6-availability-source-20260917.txt`.

### Next
P7 Public Discovery/Search/Geo source.


## 2026-09-17T13:55:00+03:00 — P7 Public Discovery/Search/Geo/Ratings source

### Goal
Implement the public discovery contract on top of the canonical taxonomy/facility/availability domains without leaking private owner/evidence data.

### Implemented
- Active public province/city/category endpoints and safe capability DTOs.
- Home, facility list/detail, map markers and search endpoints.
- Province-wide discovery; no hidden radius cutoff.
- PostGIS `Distance` annotation and nearest ordering when coordinates are supplied.
- Bounding-box filtering for map queries.
- Specialty/service relations and filters.
- Availability filters moved from Python row-by-row evaluation to centralized SQL `Exists` selectors matching TEMP_CLOSED > DUTY > OPEN > CLOSED semantics.
- Search scopes include facility, category, city/neighborhood, specialty and service labels.
- Rating model with DB uniqueness and 1..5 constraint, facility rating write/delete and account rating list.
- Public DTOs expose media routes and never raw storage keys/evidence.

### Qualification
Static qualification passed. Django/PostGIS runtime and EXPLAIN/index proof are still required. P7 is `SOURCE_IMPLEMENTED`, not closed.

### Next
P8 Realtime source implementation.


## 2026-09-17T14:10:00+03:00 — P8 Realtime source implementation

### Goal
Implement realtime as an invalidation/event transport while preserving REST as source of truth and preventing token or sensitive-object leakage.

### Implemented
- Stable event catalog from the baseline specification.
- Minimal envelope: version/name/scope/resourceId/occurredAt only.
- Hashed group names so raw user/province IDs are not embedded in channel group identifiers.
- WebSocket authentication occurs via explicit post-connect message; consumer never reads access tokens from URL query parameters.
- Anonymous province scope, authenticated current-user scope, and admin scope gated by explicit domain permission `realtime.admin`.
- Publisher defers every event using `transaction.on_commit`.
- Signal hooks emit invalidations for facilities, hours/closures, duty, category-province configuration and facility applications/review queue.
- Access token source uses HS256 with constant-time signature comparison, exp/sub validation and production fail-closed signing key requirements.

### Qualification
Static qualification passed. Redis/Channels connected delivery and rollback/no-event integration remain unverified, so P8 is `SOURCE_IMPLEMENTED`.

### Next
P9 Ads/Push/Analytics source.


## 2026-09-17T16:18:00+03:00 — P9 Ads / Push / Analytics source implementation

Goal: implement roadmap P9 without coupling provider credentials or third-party SDKs to domain truth.

Created: `content_services/**`, `notifications/**`, `analytics/**` including models, migrations, selectors/services, provider boundaries, Celery task, public ads endpoint and source tests.

Modified: backend settings/env/URLs, Home response, backend dependency declaration.

Implemented: first-party advertisements; HTTPS-only external actions; target/schedule selectors; audit service integration; notification persistence; encrypted/digested push tokens; development/FCM/APNs provider boundaries; retrying push task; analytics event registry, privacy field allowlists, event persistence and retention purge.

Problem: initial static gate found P9 lines over the repository 100-character limit.

Root cause: newly authored model/registry expressions were not wrapped to repository style.

Resolution: formatting only; no behavioral workaround. Gate rerun from the beginning passed.

Commands: compileall; AST parse; P9 line-length check; executable analytics registry privacy smoke; source push/ad invariants; governance validator; design-token validation/drift; `git diff --check`.

Results: static/source qualification PASS. Runtime Django/PostgreSQL/Celery/FCM tests not executed in this environment.

Next: P10 OpenAPI generated-client infrastructure; real schema/client generation must wait for a Django-capable toolchain.


## 2026-09-17T16:22:00+03:00 — P10 contract tooling foundation

Goal: establish canonical OpenAPI generation and deterministic drift controls without hand-authoring a schema.

Created: `scripts/generate-openapi.sh`, `scripts/check-openapi-drift.sh`, `scripts/generate-api-clients.sh`, hash writer, generator configs, generated-only package READMEs.

Modified: CI workflow with contract-drift job.

Implemented: Django/drf-spectacular schema command, SHA-256 artifact, pinned OpenAPI Generator 7.15.0 expectation and TS/Kotlin/Swift output boundaries.

Tests: bash syntax, Python helper compile, JSON config parsing, generated-only policy, no manual schema, CI source assertions and whitespace passed.

Remaining: actual schema/client generation cannot execute until Django dependencies/toolchain are available. P10 stays IN_PROGRESS.

Next: continue independent P11 Public Web source implementation.


## 2026-09-17T16:28:00+03:00 — P11 Public Web source implementation

Goal: build release-required public/legal web surfaces without inventing production domain or legal identity details.

Created: Next.js TypeScript app under `apps/web` with landing, privacy, terms, support and delete-account pages plus shared shell/config/styles.

Modified: design-token package exports so web consumes the same generated CSS tokens.

Implemented: Arabic RTL root, responsive layouts, environment-backed ROOT_DOMAIN/support/privacy contacts, CSP/content-type/referrer/frame headers, no Admin behavior.

Version verification: current official sources showed Next.js 16.3.3 Active LTS security release and React 19.3 stable; source pins were updated accordingly.

Tests: JSON/config parse, required pages, RTL/token assertions, no hardcoded hex in app CSS, security-header assertions, no invented production URL/email, `git diff --check`.

Remaining: Node 24/pnpm install, lint/typecheck/build/Playwright/accessibility; legal/company wording requires owner/legal review before production.

Next: P12 Admin foundation can proceed at source level.


## 2026-09-17T16:34:00+03:00 — P12 Admin Foundation source implementation

Goal: establish secure RTL custom Admin foundations without exposing refresh tokens to browser JavaScript.

Created: `apps/admin` Next.js TypeScript source, shared layout/styles, permission helper, server-only backend adapter, refresh-cookie helpers and same-origin logout route.

Implemented: right-side desktop navigation, responsive foundation, `can(permission)`, HttpOnly/SameSite cookie policy, no local/session storage token code, CSP/referrer/frame/content-type headers, env-backed backend/root-domain configuration.

Tests: package/config parse, RTL/shared token assertions, browser-storage token absence, permission primitive, same-origin/BFF assertions, security headers, domain placeholders and whitespace passed.

Remaining: P10 generated client and backend auth contract binding; Node 24/pnpm lint/typecheck/build/unit/Playwright.

Next: P13 Admin Operations source work.

## 2026-09-17T16:33:17+03:00 — P13 Admin Operations source implementation

### Goal
Implement the custom staff operating surface and backend operations required by the P13 specification without bypassing Django authorization or hand-authoring transport DTOs that belong to P10.

### Files created
- `apps/backend/admin_console/**`
- `apps/backend/platform_settings/**`
- `apps/backend/accounts/authentication.py`
- `apps/backend/accounts/migrations/0003_admin_permission_catalog.py`
- `apps/backend/audit/migrations/0002_operational_fields.py`
- Admin operation/detail route surfaces under `apps/admin/app/**`
- `apps/admin/lib/operations/catalog.ts`
- `apps/admin/components/operations/**`

### Files modified
- Audit model/service, content-services audit request IDs, realtime hooks.
- Backend settings/URLs/env examples.
- Admin shell/dashboard/styles.

### Implemented
- Review queue/detail with kind/province/category/status/date/evidence-completeness filters.
- Review detail includes facility/map coordinates, public image IDs, private evidence checklist IDs, duplicate warnings and audit timeline without storage keys.
- Transactional approve/reject using row locks; approval rechecks current required evidence; rejection requires reason.
- Private evidence stream permission + audit + `Cache-Control: private, no-store`.
- Facility suspend/reactivate/close transitions.
- User search/block/unblock and Admin role replacement; blocking revokes active refresh sessions.
- Taxonomy groups/categories/capabilities/per-province switches and immutable code/slug enforcement.
- Province and verification requirement operations.
- First-party ad CRUD through the P9 validation/audit service.
- Audit filtering, analytics summary, typed platform settings and privileged system status.
- Audit `before_snapshot`, `after_snapshot`, `request_id` plus stronger secret-key redaction.
- Stable Admin permission catalog with no default role grants.
- DRF Bearer authentication restored from the existing access-token implementation.
- Realtime invalidation for category/group/verification/province configuration changes.
- RTL staff route surfaces for every route required by the P13 Admin specification.

### Problems found and root causes
1. The recovered source had token issue/decode logic but no DRF Bearer authentication class, so permission checks could not reliably bind `request.user`.
   - Fix: added `BearerAccessTokenAuthentication` and made it the default DRF authentication while public views keep explicit anonymous overrides.
2. Initial Admin CSS referenced four semantic token names that do not exist in the canonical design-token output.
   - Fix: replaced them with existing canonical token names and added a zero-missing-token qualification check.
3. The extracted Git snapshot did not preserve local author configuration.
   - Fix: restored the same repository-local identity used by prior commits (`Serva Code <serva-code@local.invalid>`); no global config or personal identity was used.

### Commands and tests
- Python AST parse across 189 backend Python files.
- P13 Python line-length check.
- 11 executable source-contract tests.
- `git diff --check`.
- `node scripts/check-governance.mjs`.
- `node packages/design-tokens/scripts/generate.mjs --check`.
- Admin route presence, relative import target and CSS token-reference checks.

### Results
Static/source qualification PASS. Implementation commit: `ff60f96714212b98db00009009762de9ce40f3ae`.

P13 is `SOURCE_IMPLEMENTED`, **not** `P13 ADMIN GOLDEN PATH PASS`: Django/PostgreSQL runtime, generated P10 client binding, Node 24/pnpm typecheck/build and Playwright golden paths remain unexecuted in this environment.

### Next
P14 Android Foundation.


## 2026-09-17T17:45:00+03:00 — P13 source recovery

Goal: restore P13 source after the persisted Git bundle was found to end at P12 despite management documents recording P13 completion.

Root cause: the prior snapshot included a repository bundle created before P13; the later P13 working Git objects were not persisted.

Resolution: restored P13 from the official V3 specification plus the recorded P13 implementation/status handoff. No business rules were invented.

Qualification: backend AST 184 files, 7 source-contract checks, Admin canonical CSS tokens/relative imports, governance, design-token drift and whitespace passed. Runtime/Playwright remain unverified.

Commit: `507814d`.

## 2026-09-17T17:58:00+03:00 — P14 Android Foundation source implementation

Goal: create the native Android foundation from scratch according to the Android specification.

Implemented: 27-module Kotlin/Compose graph; Gradle Kotlin DSL/version catalog/convention plugins; Hilt boundaries; memory-only access token; Android Keystore AES/GCM refresh vault; refresh Mutex; Room cache; DataStore preferences; while-in-use LocationProvider; MapLibre MapController abstraction; typed analytics; observability boundary; shared generated design tokens with RTL; type-safe navigation; Bootstrap Composable → ViewModel → UseCase → Repository.

Security: no background location, no raw refresh storage, no signing keys/secrets, cleartext traffic disabled, no React Native/Flutter.

Qualification: Android source qualification, governance/design-token drift, line length and whitespace passed. Gradle/Android SDK/ADB and DNS to Gradle distribution were unavailable, so build/lint/tests/device were not executed.

Commit: `4c0a693`.

Next: P15 Android Public source.


## 2026-09-17T18:31:00+03:00 — P15 Android Public source qualification

Goal: complete the public native Android experience on top of P14 without duplicating P10 transport contracts.

Implemented: Home/province/location/search/directory/facility detail/map/account/ratings; public cache-first paths; Room API-order preservation; Coil public images; MapLibre native viewport surface; location-denial/approximate-safe behavior; typed navigation; ratings 1..5 validation.

Toolchain decision: AGP 9.4.0 + Kotlin 2.3.21 + KSP 2.3.12; Kotlin 2.4.20 intentionally deferred until supported/build-verified.

Tests: P14 qualifier PASS; P15 qualifier PASS; governance PASS; design-token validation + regenerate/diff PASS; `git diff --check` PASS. Gradle/SDK/ADB unavailable, so build/unit/Compose/instrumentation/device tests were not executed.

Result: P15 `SOURCE_IMPLEMENTED`. Implementation commit: `7178ba5`.

Problem discovered before P16: recovered backend lineage is missing documented owner config/facility/images/evidence/members endpoint source even though facility models and hours/duty endpoints exist. Root cause is prior workspace/source loss. P16 will restore this dependency from the immutable API/product specs before implementing the owner mobile surface.

Next: P16 Android Owner.

## 2026-09-17T18:44:51+03:00 — P16 Android Owner source qualification

Goal: complete the native Android owner phase while repairing the owner API dependency lost during prior workspace recovery.

Created/modified: owner backend API/media/permission/service/presenter/migration/test source; Android owner/onboarding/duty modules; owner API boundary; map picker extensions; account/navigation wiring; P16 source qualifiers.

Implemented: membership-scoped owner config/facility CRUD/submit; public image/private evidence upload; manager membership controls; current-policy submit recheck; last-owner protection; safe media re-encode; My Facilities; draft autosave; map/location; per-day hours; Photo Picker uploads; evidence/review/status; temporary closures; duty scheduling/start-now/end-early/cancel.

Problems found and root causes:
- Recovered lineage lacked the owner API source documented by the immutable contract. Reconstructed it from V3 specs before Android binding.
- Last owner could have been downgraded through member upsert because model `clean()` is not called by `update_or_create`. Replaced with row-locked explicit mutation and last-owner check.
- Clearing a city while retaining an existing neighborhood caused an invalid dependent selection. Core update now clears the neighborhood automatically when city is explicitly cleared unless a replacement neighborhood is supplied.
- Initial duty UI invented a four-hour duration. Removed the invented business rule; start-now uses an owner-specified end time.
- Initial hours editor would have saved the same schedule for all seven days. Replaced with explicit per-day enable/time controls seeded from the current draft.

Commands/tests:
- `python -m compileall -q apps/backend` PASS.
- `python apps/backend/scripts/qualify-owner-source.py` PASS.
- `python apps/android/scripts/qualify-source.py` PASS.
- `python apps/android/scripts/qualify-public-source.py` PASS.
- `python apps/android/scripts/qualify-owner-source.py` PASS.
- Governance/design-token validation/regeneration drift/`git diff --check` PASS.
- Pure `DutyValidator` executable smoke compiled/run through local `kotlinc` PASS.
- Four Django owner tests authored but not executed because Django/PostgreSQL are unavailable.

Result: P16 `SOURCE_IMPLEMENTED`, implementation commit `682218a`. `P16 ANDROID OWNER DEVICE PASS` is not claimed.

Next: P17 Maps/Navigation.


## 2026-09-17T18:56:00+03:00 — P17 Maps / Navigation source qualification

Goal:
Implement Phase 17 routing/geocoding/navigation foundations without public demo dependencies or fake route data.

Created/modified:
- Android map/location/network/navigation source, tests and qualification script.
- Facility Details and app navigation wiring for built-in directions.
- `artifacts/evidence/p17-navigation-source-20260917.txt`.

Implemented:
- `RoutingProvider` + OSRM-compatible adapter.
- `GeocodingProvider` + Nominatim-compatible adapter.
- HTTPS/provider policy and environment-only provider URLs.
- Foreground location updates with callbackFlow cleanup.
- Navigation state machine, off-route/reroute cooldown/arrival thresholds, remaining distance/ETA.
- Arabic maneuver phrases + native Android TTS.
- MapLibre route and current-location rendering.

Problems found and root cause:
- MapLibre async callback was stored outside observable Compose state; route draw could be skipped when map initialization completed after route state. Fixed by storing `MapLibreMap?` in Compose mutable state.
- P15 regression qualifier treated explicit denylist literals/tests as shipped demo dependencies. Tightened the qualifier to exclude the central denylist and test-only source while preserving runtime checks.
- Facility Details had no product path into the already-defined `BuiltInNavigation` route. Added explicit Directions action and NavHost destination.

Commands/tests:
- `python apps/android/scripts/qualify-navigation-source.py` => PASS.
- P14/P15/P16 source qualifiers => PASS.
- `git diff --check` => PASS.
- Pure Kotlin navigation smoke compiled with local `kotlinc` and executed with `java` => PASS.

Qualification:
P17 is SOURCE_IMPLEMENTED only. `P17 NAVIGATION ROAD PASS` remains unpassed because Gradle/Android SDK/ADB and a real road/device environment are unavailable.

Next:
P18 — offline/realtime/push hardening.


## 2026-09-17T19:10:00+03:00 — P18 mobile live-data source qualification

Goal:
Harden Android offline/realtime/push behavior while preserving REST as source of truth.

Implemented:
- Typed P8-compatible realtime event envelope/catalog.
- WSS-only WebSocket config, province subscription and post-connect access-token authentication/user subscription.
- Process-lifecycle foreground connection coordinator with validated-network awareness.
- Exponential reconnect from 1s capped at 30s, jitter and reset after stable connection.
- Event dedupe and active-screen invalidation -> REST refetch for public and owner surfaces.
- Explicit stale UI warning that cached open/duty state may be outdated.
- Push token registration/deactivation boundary and identifier-only push payload parser.
- Notification permission declaration and P18 source tests/qualifier.

Problems found and root cause:
- Cached UI previously said only “last saved data”, which did not make time-sensitive open/duty uncertainty explicit. Updated stale copy to warn that availability may be old.
- Realtime did not yet exist on Android despite P8 backend source. Added lifecycle/network-aware client instead of allowing events to mutate domain state directly.

Commands/tests:
- P14-P18 source qualifiers => PASS.
- `git diff --check` => PASS.
- Pure Kotlin reconnect policy smoke => PASS.

Qualification:
P18 is SOURCE_IMPLEMENTED only. Connected Channels/Redis, FCM and physical-device lifecycle/push gates remain pending.

Next:
P19 — staging production-like deploy.

## 2026-09-17T19:35:00+03:00 — P19 staging source qualification

Goal: prepare a production-like, isolated staging topology without touching V2 resources or inventing a domain/credential.

Implemented:
- Corrected staging settings to inherit production exactly once; added fail-closed `DATABASE_URL`, refresh/recovery HMAC requirements and trusted HTTPS proxy handling.
- Removed swallowed backend Docker dependency failure (`|| true`) and retained two-stage dependency install behavior.
- Added PostGIS extension enablement to the initial location migration.
- Added `render.yaml` for isolated V3 staging API, Celery worker, PostgreSQL 17, persistent/no-eviction Key Value, public web and admin.
- Added web/admin monorepo Dockerfiles, staging env placeholder contract, health smoke script and source qualifier.
- Added deploy, rollback, backup/restore, incident, DNS/TLS and monitoring runbooks.

External verification/actions:
- Connected Render inventory was inspected. Existing API/admin/Postgres/Key Value resources are V2 and were deliberately not modified.
- Connected GitHub search found no V3 repository. Available connector actions cannot create a repository, so connected deployment cannot start safely.
- The Blueprint uses production-like paid staging plans; provisioning is financial and was not performed implicitly.

Commands/tests actually run:
- `python infrastructure/scripts/qualify-staging-source.py` PASS.
- `bash -n infrastructure/scripts/staging-smoke.sh` PASS.
- `python -m compileall -q apps/backend` PASS.
- P19 Python line-length check PASS.
- P19 secret-marker scan PASS.
- `node scripts/check-governance.mjs` PASS.
- `node packages/design-tokens/scripts/validate.mjs` PASS.
- `git diff HEAD^ --check` PASS.

Evidence: `artifacts/evidence/p19-staging-source-20260917.txt`.
Implementation commit: `1cc6cf1`.

Qualification: P19 is `SOURCE_IMPLEMENTED`; `P19 STAGING RUNTIME PASS` is NOT claimed.

Next: P20 full E2E/security/load/restore quality harnesses; run connected suites once EXT-004/EXT-005 and provider credentials are resolved.


## 2026-09-17T19:55:00+03:00 — P20 local release-quality qualification

Goal: build an executable P20 quality gate and repair source-integrity gaps discovered by that gate.

Implemented:
- `infrastructure/quality/` manifest, orchestrator, source-security, staging golden path, bounded load, restore evidence validator and release blocker report.
- Restored missing Auth/Account API source: OTP registration/recovery, login, rotating refresh, sessions, profile and account deletion.
- Added OTP/login/recovery throttling and session compromise/reuse handling.
- Account deletion revokes sessions, anonymizes PII and refuses deletion while the user is sole owner of a non-closed facility.

Tests/results:
- Auth source-contract: 3 passed.
- Backend compileall: PASS.
- P20 local orchestrator: SOURCE_QUALIFIED.
- P14-P19 source regressions through orchestrator: PASS.
- Strict `--require-connected`: FAIL as designed because backend/admin/Android runtime, staging golden path and restore evidence are unavailable.
- `git diff --check`: PASS.

Evidence:
- `artifacts/evidence/p20-release-quality-source-20260917.txt`
- `artifacts/evidence/quality/p20-local.json`
- `artifacts/evidence/quality/p20-connected-required.json`

Commit: `5e58355`.

Gate status: P20 source implemented only; `P20 RELEASE QUALITY PASS` not achieved.
Next: P21 Android Play RC source preparation while connected P20 remains pending.


## 2026-09-17T20:27:00+03:00 — P21 Play RC source qualification and handoff freeze

Goal: finish the source-only Android Play RC preparation and freeze a precise handoff for continuation in Claude.

Implemented:
- Fail-closed Android release validation for production endpoints and upload-signing environment inputs.
- Environment-variable support for Android production endpoint configuration.
- In-app account deletion request flow and session cleanup boundary.
- Play policy baseline dated 2026-09-17, Data Safety inventory, app-content checklist, Arabic store-listing baseline, testing runbook, release environment template and evidence template.
- Dedicated P21 source qualifier.

Commands/tests actually run:
- `python3 apps/android/scripts/qualify-play-rc-source.py` -> PASS (7 checks).
- `python3 apps/android/scripts/qualify-source.py` -> PASS (27 modules).
- `node scripts/check-governance.mjs` -> PASS.
- `node packages/design-tokens/scripts/validate.mjs` -> PASS.
- `node packages/design-tokens/scripts/generate.mjs --check` -> PASS.
- `git diff --check` -> PASS.

Implementation commit: `9196f4d`.
Evidence: `artifacts/evidence/p21-play-rc-source-20260917.txt`.

Qualification: P21 is `SOURCE_IMPLEMENTED`; `P21 PLAY RC PASS` is NOT claimed because no signed AAB, device/staging gate, Play track upload or policy submission occurred. P22 is externally blocked; next independently executable roadmap phase is P23 iOS Foundation.


## 2026-09-17T22:15:00+03:00 — FIX-P0: restore backend and Android foundation runtime

Goal: close the four P0 defects found by the receipt audit (`RECEIPT-AUDIT-2026-09-17.md`) so that the backend actually boots and the Android build gets past `build-logic`. Scope was deliberately limited to those defects; no other known issue was touched.

### Why this batch exists

Every phase up to P21 had been qualified with *textual* source checks. Textual checks cannot observe that a server does not start, that a migration graph does not load, or that Kotlin does not compile. The first real execution of the toolchain surfaced four P0 defects that all prior gates had passed over.

### FIX-001 — ASGI initialization order

File: `apps/backend/directory_backend/asgi.py`

Root cause: line 8 imported `directory_backend.routing`, which reaches `realtime.routing` then `realtime.consumers` then `locations.models`, while `get_asgi_application()` was only called on line 10. Importing a model before the app registry is populated raises `django.core.exceptions.AppRegistryNotReady` and daphne exits immediately. This is the command used by the Dockerfile CMD, `compose.yml` and `render.yaml`, so no environment could ever have started the service.

Fix: call `get_asgi_application()` before importing Channels routing, which is the documented Channels ordering. `# noqa: E402` marks the intentional post-setup imports. No workaround and no lazy-import shim.

### FIX-002 — Django messages without session middleware

File: `apps/backend/directory_backend/settings/base.py`

Root cause: `django.contrib.messages` and `MessageMiddleware` were installed while `django.contrib.sessions` and `SessionMiddleware` were not. The default `FallbackStorage` constructs `SessionStorage`, which raises `ImproperlyConfigured` in `process_request` on every single request. All endpoints returned HTTP 500.

Evidence gathered before removing anything:

- `django.contrib.messages` appeared in exactly two places, both of them in `settings/base.py`.
- Zero occurrences of `messages.*`, `add_message`, `get_messages`, `request._messages`, `MESSAGE_STORAGE` or `MESSAGE_TAGS` anywhere in the backend.
- No `django.contrib.admin`, no `TEMPLATES` setting, no `render()` call — nothing server-rendered exists.
- No `django.contrib.sessions`, no `SessionMiddleware`, no `request.session` usage.
- `00-START-HERE.md`, `07-BACKEND-DJANGO.md`, `09-ADMIN-NEXTJS.md` and `23-AUTONOMOUS-CLAUDE-EXECUTION.md` all forbid Django Admin as the operating UI.

Fix: removed both entries. Adding Django cookie sessions was rejected because the architecture is API-first with Bearer access tokens and opaque rotating refresh material; cookie sessions are not required by any specification.

### FIX-003 — Sessions migration dependency app label

File: `apps/backend/sessions/migrations/0002_rotation_security_fields.py`

Root cause: `sessions/apps.py` declares `label = "directory_sessions"`, but the migration depended on `("sessions", "0001_initial")`. Django resolves migration dependencies by app label, so the graph contained a dangling node and every migration command failed with `NodeNotFoundError`. No database could ever be created.

Fix: `dependencies = [("directory_sessions", "0001_initial")]`. A repository-wide scan confirmed this was the only wrong label reference; other migrations already use `directory_sessions` correctly.

### FIX-004 — Android AGP 9 convention plugin

File: `apps/android/build-logic/src/main/kotlin/serva.android.compose.gradle.kts`

Root cause: the plugin used the AGP 8 signature with six star projections on `CommonExtension`. Verified directly against the resolved AGP 9.4.0 jar with `javap`: `public interface CommonExtension extends ExtensionAware` carries no type parameters. The unresolved generic cascaded into `buildFeatures` and `compose` being unresolved.

Fix: configure the non-generic `com.android.build.api.dsl.CommonExtension` and set `buildFeatures.compose`. `javap` confirmed that `CommonExtension.getBuildFeatures()` returns `BuildFeatures`, that `BuildFeatures` exposes a `compose` property, and that AGP 9.4 offers no `buildFeatures(Action)` overload — so the property form is the correct API rather than a workaround. AGP was not downgraded, and Kotlin, Compose and SDK policy were left unchanged.

### FIX-004b — Android source sets under AGP 9 (same defect class, surfaced by FIX-004)

File: `apps/android/core/designsystem/build.gradle.kts`

With FIX-004 in place the build advanced and then failed at project configuration with `DefaultAndroidLibrarySourceSet_Decorated cannot be cast to AndroidLibrarySourceSet`. The generated `android { }` accessor still resolves to the removed AGP 8 source-set type. `javap` confirmed that AGP 9.4 exposes `LibraryExtension.getSourceSets()` typed to the new DSL `AndroidLibrarySourceSet`, and that `AndroidSourceSet` has both `getJava()` and `getKotlin()`.

Fix: configure `com.android.build.api.dsl.LibraryExtension` directly and register the generated token directory on `kotlin` rather than `java`, because the generated artefact is `DirectoryTokens.kt`.

### Commands actually run

Backend, against PostgreSQL 17.5 with PostGIS in Docker, from an image built out of the fixed source using the unmodified `apps/backend/Dockerfile`:

- `manage.py check` gave exit 0.
- `manage.py showmigrations` gave exit 0 with a valid graph and `directory_sessions` listed correctly.
- `manage.py migrate` gave exit 0 and applied all 30 migrations.
- `manage.py makemigrations --check --dry-run` gave exit 1 (INT-009 drift, deliberately out of scope).
- `uv run pytest` gave exit 2 (INT-028 collection failure, deliberately out of scope).
- daphne started with the unmodified Dockerfile CMD and logged `Listening on TCP address 0.0.0.0:8000`.
- `GET /health/live/` returned 200 with `{"status": "ok"}`.
- `GET /health/ready/` returned 200 with `{"status": "ready", "checks": {"database": "ok", "redis": "ok"}}`.
- `GET /api/v1/public/provinces/` returned 200 with an empty item list, empty because of INT-006.
- `GET /api/v1/public/facilities/` returned 400 with a required-field error, which is correct.
- `GET /api/v1/admin/dashboard/` returned 403, RBAC fail-closed, which is correct.
- `GET /api/v1/owner/facilities/` returned 500 through INT-010, recorded for the next batch and not fixed here.

Android, Gradle 9.6.0 with JDK 25 and Android SDK 36:

- `gradle :app:assembleDebug` before FIX-004 failed at `:build-logic:compileKotlin`.
- After FIX-004 the tasks `:build-logic:compileKotlin` and `:build-logic:jar` passed and the build failed at project configuration.
- After FIX-004b all 27 modules configured and the build failed at dependency resolution.

Regressions after the fixes: governance, design-token validation, design-token drift, the P19 staging qualifier and all five Android source qualifiers exit 0. `ruff check .` still reports exactly 106 errors, the pre-existing baseline, so no lint regression was introduced.

### Result

P2 backend runtime is restored end to end. `P2 BACKEND FOUNDATION CONNECTED PASS` is still NOT claimed, because `makemigrations --check` fails on INT-009 and `pytest` cannot collect through INT-028.

Android moved past the P0 compile blocker. `:app:assembleDebug` remains `NOT_VERIFIED` because Google Maven is unreachable from this machine, which is an environment limitation and not a project defect: artifacts that certainly exist, including `androidx.annotation:annotation:1.0.0` and `com.android.tools.build:gradle:9.4.0`, return 404 while Maven Central answers 200. Android status stays below `BUILD_VERIFIED` and `DEVICE_VERIFIED` is not claimed.

Evidence: `artifacts/evidence/fixp0-runtime-20260917.txt`. Audit baseline: `RECEIPT-AUDIT-2026-09-17.md`.


## 2026-09-17T23:55:00+03:00 — P2 connected qualification: migration convergence, invariants, test suite

Goal: close INT-009, INT-028 and INT-007 so the P2 gate can be judged on real command exit codes. Scope was held to those items plus the two defects that directly blocked them.

### TASK-P2-01 — INT-009 migration drift

Analysis first, with `makemigrations --dry-run --verbosity 3` and no migration written. The drift was larger than previously reported: **seven apps, thirteen operations**, not six apps. `analytics` had been cut off by output truncation in the earlier report.

The thirteen operations fell into three very different classes:

**Class 1 — nine `RenameIndex`, cosmetic.** The migrations hardcoded index names that Django no longer derives from `models.Index(fields=[...])` with no explicit `name`. Resolution per DECISION-006: pin the existing database name in model state. Eight indexes keep their exact name. One could not: `facilities__prov_cat_status_idx` is 31 characters and `Index.max_name_length` is 30, verified directly against Django, so Django rejects it in model state with `models.E034`. It was renamed to `facility_prov_cat_status_idx`, 28 characters, the single unavoidable rename in this batch.

**Class 2 — one `RemoveConstraint`, destructive.** `uniq_submitted_application_per_facility_kind` was created by `facilities/0003_owner_media_integrity` and enforces a `06-DATA-MODEL` invariant, but `FacilityApplication` had no `Meta` at all, so the autodetector proposed dropping it. Resolution per DECISION-007: declare the constraint in model state with the identical name, fields and condition. The database and migration are untouched.

In PostgreSQL the constraint is materialised as a partial unique index:
`CREATE UNIQUE INDEX uniq_submitted_application_per_facility_kind ON public.facilities_facilityapplication USING btree (facility_id, kind) WHERE ((status)::text = 'SUBMITTED'::text)`

**Class 3 — one `RemoveConstraint` plus `AddConstraint` on the duty GiST constraint, perpetual churn.** Investigated with a probe rather than assumed. `TstzRange.deconstruct()` round-trips perfectly, so the helper was never at fault; the earlier hypothesis about unstable deconstruction was wrong. The real cause: `pharmacy_duty/migrations/0001_initial` declared its own local `TstzRange` class instead of importing the one in `pharmacy_duty/models.py`. `BaseExpression.identity` begins with `self.__class__`, so the two structurally identical expressions could never compare equal:

```
model type : TstzRange | module: pharmacy_duty.models
migr  type : TstzRange | module: pharmacy_duty.migrations.0001_initial
same class : False
```

Resolution per DECISION-008: the migration imports the shared helper. The constraint operation line is byte-identical.

**And the three `AlterField` on `accounts`**, which were accepted only after proving they are metadata-only. `sqlmigrate accounts 0005` renders `-- (no-op)` for all three; the difference is `verbose_name` and `help_text` on `PermissionsMixin` fields. See DECISION-009.

Correction to an earlier claim: the audit said the `PermissionsMixin` fields "were never migrated". That was wrong. They are present in `accounts/0001_initial`; only their field definition drifted.

Result: **13 operations across 7 apps reduced to 4 operations across 2 apps**, both justified and both accepted:
- `accounts/0005_alter_user_groups_alter_user_is_superuser_and_more` — three no-op `AlterField`.
- `facilities/0004_rename_facilities__prov_cat_status_idx_facility_prov_cat_status_idx` — one `ALTER INDEX ... RENAME TO ...`.

### TASK-P2-02 — hardened the invariant qualifier

`scripts/qualify-owner-source.py` proved the submitted-uniqueness invariant by searching for its name inside a migration file. That can never fail: a migration keeps its historic `AddConstraint` forever, so the check would still have reported PASS after the constraint was dropped from the database.

Replaced with `check_submitted_uniqueness_invariant`, which parses `facilities/models.py` with `ast`, walks into `FacilityApplication.Meta.constraints` and asserts the constraint is declared there with the right name, the `(facility, kind)` scope and the partial `status=SUBMITTED` condition, then still checks the migration as a second leg.

Negative-tested: with the constraint removed from model state the qualifier exits 1 with a precise message; restored, it exits 0.

The remaining two requirements — that the autodetector does not propose removal, and that the database actually refuses the violating row — are covered by connected tests, since a source qualifier has no database.

### TASK-P2-03 — INT-028 pytest collection

`accounts/tests`, `admin_console/tests` and `search/tests` had no `__init__.py` while `test_source_contract.py` and `test_source_security.py` basenames repeated, so pytest aborted collection before running anything. Added the three missing package markers; all twelve test packages now have one.

### TASK-P2-04 — INT-007 test database engine

`settings/test.py` pinned SQLite while the domain owns spatial fields, a PostGIS extension migration, `btree_gist` and an exclusion constraint. No database-backed test could ever pass; the suite failed on `geo_db_type`. The SQLite override was removed so the test settings inherit the PostgreSQL + PostGIS configuration from `base.py` through `DATABASE_URL`.

### INT-030 — defect found by running the suite on PostgreSQL for the first time

`submit_facility` locked with `select_for_update()` over `select_related("category__group", "category__capabilities", "province")`. `category__capabilities` is a reverse one-to-one, so `select_related` emits a LEFT OUTER JOIN, and PostgreSQL refuses `FOR UPDATE` on the nullable side of an outer join:

`django.db.utils.NotSupportedError: FOR UPDATE cannot be applied to the nullable side of an outer join`

`POST /api/v1/owner/facilities/{id}/submit/` would have failed at runtime in every environment. SQLite never enforced the rule, so the defect stayed invisible. Fixed with `select_for_update(of=("self",))`, which keeps the intended lock on `facilities_facility` and leaves the joined reference rows unlocked. No semantic change. The other three `select_for_update` sites were checked and are unaffected.

### INT-029 — recorded, not fixed

With Redis unreachable, any write to a model with a realtime hook raises after the transaction has already committed: `publish_after_commit` guards `channel_layer is None` but not a connection error. `05-SYSTEM-ARCHITECTURE §13` asks for safe degradation. Out of scope here.

### Commands actually run

Against PostgreSQL 17.5 with PostGIS, from an image built out of the fixed source with the unmodified `apps/backend/Dockerfile`:

Fresh database, full sequence — `check` 0, `migrate` 0, `makemigrations --check --dry-run` 0, `migrate` 0, `makemigrations --check --dry-run` 0. Convergence holds on a second pass.

Existing database upgrade path — a database was built with the pre-batch code at `189a500`, seeded with a province, category, capabilities, facility, a SUBMITTED application and a duty shift, then upgraded. Exactly two migrations applied. Row counts identical before and after. `pg_class.oid` compared across the upgrade:

| object | before | after | verdict |
|---|---|---|---|
| `prevent_overlapping_duty_for_facility` | 27009 | 27009 | untouched, no GiST rebuild |
| `uniq_submitted_application_per_facility_kind` | 26314 | 26314 | preserved, not dropped |
| `facilities__prov_cat_status_idx` → `facility_prov_cat_status_idx` | 26124 | 26124 | renamed in place, not rebuilt |
| `pharmacy_du_facilit_idx` | 27011 | 27011 | untouched |
| `business_ho_facilit_idx` | 26209 | 26209 | untouched |

Invariant behaviour against the real database — five checks, all passing: a second SUBMITTED application of the same kind is rejected by `uniq_submitted_application_per_facility_kind`; a DRAFT alongside it is accepted; a SUBMITTED application of a different kind is accepted; an overlapping duty shift is rejected by `prevent_overlapping_duty_for_facility`; a shift starting exactly when the previous one ends is accepted, confirming the half-open range.

`uv run pytest`, the official command with no import-mode workaround — **49 passed, 0 failed**, against a run that previously collected nothing at all.

Regressions — governance, design-token validation, design-token drift, the P19 staging qualifier and the hardened owner qualifier all exit 0.

Lint — `ruff check .` reports 104 errors against a 106 baseline. Debt reduced, none added. `mypy` stays at its 556 baseline and is untouched. Both are now tracked as DEBT-001 and DEBT-002.

### Result

`makemigrations --check --dry-run` exits 0. `uv run pytest` exits 0. The migration graph is valid, migrations converge on both a fresh and an upgraded database, and both business invariants are proven enforced by PostgreSQL itself.

Evidence: `artifacts/evidence/p2-connected-20260917.txt`.


## 2026-09-18T02:40:00+03:00 — P10 OpenAPI contracts recovery

Goal: turn a document that described nothing into a contract three client generators can
consume, so Admin, Android and iOS stop being blocked behind a DTO-less boundary.

### Where it started

`manage.py spectacular` exited 0 and produced 72 paths, which is why the phase had been
recorded as tooling that merely needed running. The document itself was empty of contract:
84 operations, **zero** component schemas, **zero** request bodies, **zero** response
schemas, no security scheme, five operation-id collisions resolved with numeral suffixes,
328 errors and 76 warnings. Every error was the same one, "unable to guess serializer",
because every view is a plain `APIView` returning a hand-built dict.

A client generated from that would have had no models at all.

### Approach

Django and DRF stay the executable source of truth. Nothing here changes behaviour to make
the schema easier. Every serializer added describes what a view already returns; none of
them builds a response. `@extend_schema` supplies only what introspection cannot know:
query parameters, multipart bodies, alternative status codes, response envelopes, tags and
the explicit operation id.

Work was driven from a full inventory of all 87 routed operations grouped by domain, not
endpoint by endpoint at random.

### Shared infrastructure

`core/openapi.py` carries the authentication extension, the error components and the
reusable value objects. Registering `BearerAccessTokenScheme` alone removed 71 of the 76
warnings, because until then drf-spectacular could not resolve the authenticator and
emitted no security scheme at all.

`core/enums.py` binds reused choice sets to stable component names. Without it
drf-spectacular invented hash-suffixed names such as `Status652Enum` for the three
different `status` fields, and those names are not stable across runs, so they would have
leaked into every generated client and churned the drift gate.

`core/schema_view.py` gates the interactive schema route by environment.

### Error contract

Documented as the runtime actually behaves, not as the specification describes. There is no
custom DRF `EXCEPTION_HANDLER`, so exactly two shapes reach clients: `DetailError` for
authentication, permission, not-found and throttling, and `DomainError` for domain rule
rejections, plus DRF's field-scoped validation map. `08-API-CONTRACT.md` specifies a single
richer envelope with `code`, `message`, `details` and `requestId` which nothing emits.
Describing that instead would have produced clients that break on the first error. See
DECISION-012; the divergence is recorded as INT-035 and INT-036 rather than hidden.

### Defects found while writing the contract

Five, three of which meant the authentication surface had never worked. None of them could
have been caught by the existing tests, which assert on source text.

**INT-033.** `RegisterCompleteView`, `LoginView` and `RecoveryResetView` pass
`**serializer.validated_data` into their services, but the serializers declare camelCase
fields with no `source`, so the keys arrived as `challengeId` and `deviceName` while the
services take `challenge_id` and `device_name`. Registration completion, login and password
reset all returned 500. Fixed with `source`, the pattern the project already uses
elsewhere; the wire names are unchanged.

**INT-031.** `Province` was filtered on `is_active` at three sites; the model field is
`active`. Registration start, registration completion and the profile province change all
returned 500.

**INT-034.** `facility_detail` ordered business hours by `sequence`, which does not exist;
the field is `sort_order`. Every owner facility detail, create, update and relocate response
returned 500. `06-DATA-MODEL.md` does name the field `sequence`, so the presenter followed
the document while the model followed another name. The runtime name was kept and the
divergence recorded as INT-037.

**INT-010.** Eleven owner views, both rating views and the media views declared no
`permission_classes`, so DRF defaulted to `AllowAny`. Fixed here because the contract must
state the security requirement, and the contract test for protected operations enforces it.
`GET /api/v1/owner/facilities/` now returns 403 rather than 500.

**INT-016.** The interactive schema route was public in every environment. Now governed by
`OPENAPI_SCHEMA_EXPOSURE`: public in development, admin-only in staging, not routed in
production.

### Commands actually run

- `manage.py spectacular` — 0 errors, 0 warnings, from 328 and 76.
- Two independent generations from a clean container — byte-identical.
- `./scripts/generate-api-clients.sh` — TypeScript, Kotlin and Swift, generator 7.15.0.
- `tsc --noEmit` on the generated TypeScript client — exit 0.
- `uv run pytest` — 61 passed, including the 12 new contract tests.
- Runtime smoke against PostgreSQL 17.5 with PostGIS and Redis — 17/17 responses validated
  against the declared schema, covering auth, public list and detail, owner mutation, admin
  mutation, validation failure and permission failure.
- Regressions: governance, design-token validation and drift, the P19 staging qualifier and
  the hardened owner qualifier all exit 0.
- `ruff check .` — 101 errors against the 104 baseline. Debt reduced, none added. Every file
  created or modified in this batch passes ruff cleanly.

### Drift gate

The previous gate ran `git diff --exit-code` on `openapi/schema.yaml` while that file was
untracked. `git diff` ignores untracked paths, so it reported success on a contract that did
not exist. It now refuses to run unless both canonical files are tracked, compares blob
hashes across a regeneration, and independently verifies the recorded digest. CI also
regenerates all three clients and fails if they differ from what is committed, and the
backend job gained a PostGIS service plus the GeoDjango native libraries.

### Verification levels, stated precisely

TypeScript: generated and compiled. Kotlin: generated; compilation `NOT_VERIFIED` because
the Gradle distribution download from services.gradle.org resets on this network, the same
`ENVIRONMENT_LIMITATION` that blocks Google Maven. The Kotlin client needs only Maven
Central, so it should compile from a network that can reach the Gradle services. Swift:
generated as a contract artefact; no Swift toolchain here, and iOS work is not started.

Evidence: `artifacts/evidence/p10-contracts-20260918.txt`.
