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
