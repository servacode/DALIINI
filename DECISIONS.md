# Decisions

The authoritative baseline decisions are in `docs/spec/02-BASELINE-DECISIONS.md` and `docs/spec/BASELINE-DECISIONS.json`. They are not duplicated here as new decisions.

No project-specific decision outside the approved baseline has been required yet.

For a material architecture decision, create an ADR under `docs/adr/` using `ADR-TEMPLATE.md` and reference it here.

## DECISION-001 — Baseline typography metrics

**Date:** 2026-09-17

**Subject:** Concrete size/line-height/weight values for the typography roles named by the approved Design System specification.

**Decision:** Use a restrained mobile-first scale from 12px through 32px, with generous Arabic line heights and weights 400/600/700. The canonical values live in `packages/design-tokens/tokens/typography.json`.

**Reason:** The source specification fixes role names and Tajawal but does not provide numeric metrics; deterministic cross-platform generation requires concrete values. The selected values prioritize Arabic legibility and can be revised through the token source after visual/device qualification.

**Alternatives:** Platform-specific independent sizes; arbitrary per-screen values. Both were rejected because they violate the unified token model.

**Impact:** All clients receive the same semantic scale; native clients may map units appropriately while preserving role intent.

## DECISION-002 — P3 identity/session/RBAC implementation

**Date:** 2026-09-17

**Subject:** Concrete Django implementation for the approved P3 security properties.

**Decision:** Adopt the custom User, explicit Admin RBAC, JOSE/JWT access + opaque rotating refresh, digest-only OTP/refresh persistence, concurrency-grace behavior, and provider abstraction described in `docs/adr/ADR-001-P3-IDENTITY-SESSION-RBAC.md`.

**Reason:** The baseline specifies the required security semantics but leaves these implementation details open. P3 needs one deterministic, testable design before code is written.

**Alternatives:** See ADR-001.

**Impact:** P3 API/session clients must treat refresh material as nullable on a concurrency-grace response; Admin RBAC remains separate from future facility membership.

## DECISION-003 — Minimal Province persistence introduced as a P3 dependency

**Date:** 2026-09-17

**Subject:** `User.profile_province` is required by the P3 data model even though full Locations/Taxonomy is P4.

**Decision:** Introduce only the authoritative `Province` persistence fields needed to satisfy the User FK during P3. City, Neighborhood, province seed data, activation operations, taxonomy and all P4 behavior remain owned by P4.

**Reason:** Replacing the documented FK with an unvalidated UUID would weaken referential integrity, while implementing the whole P4 domain early would violate roadmap sequencing.

**Alternatives:** Unvalidated `profile_province_id`; implementing all of P4 during P3. Both were rejected.

**Impact:** P3 can preserve the required FK without claiming P4 implementation or closure.

## DECISION-004 — Explicit P13 Admin permission-code catalog

**Date:** 2026-09-17

**Subject:** The specification requires a Permission Matrix and backend rechecks but does not assign concrete permission-code strings.

**Decision:** Use operation-scoped stable codes such as `review.read`, `review.decide`, `evidence.read`, `facility.manage`, `user.roles.manage`, `taxonomy.manage`, `province.manage`, `ads.manage`, `audit.read`, `analytics.read`, `settings.manage`, and `system.read`. Seed permissions only; do not invent default role grants.

**Reason:** Backend and Admin UI require deterministic permission identifiers while role composition remains an operational policy that the source specification intentionally leaves configurable.

**Alternatives:** Hard-code role names into views; use Django `is_staff`; grant a default super-role automatically. These were rejected because they move authorization truth away from explicit RBAC or invent production policy.

**Impact:** Role definitions can be composed later without changing endpoint permission semantics. Every privileged mutation still rechecks the permission in Django.


## DECISION-005 — Android P14/P15 toolchain pin

**Date:** 2026-09-17

**Subject:** Kotlin compiler/plugin version used with AGP 9.4 for the native Android project.

**Decision:** Keep AGP `9.4.0` and pin Kotlin compiler plugins to `2.3.21`; keep KSP `2.3.12`. Do not move this source to Kotlin 2.4.20 until an actual Gradle build proves a supported combination.

**Reason:** Current Android/Compose guidance for AGP 9.4 uses Kotlin 2.3.21, while JetBrains' Kotlin Gradle plugin compatibility table does not list AGP 9.4 in Kotlin 2.4.20's fully-supported range. The project uses AGP 9 built-in Kotlin for Android source and only applies Kotlin compiler plugins where required.

**Alternatives:** Upgrade immediately to Kotlin 2.4.20; downgrade AGP. Both were rejected without build evidence because the former exceeds the documented KGP AGP range and the latter discards the current stable AGP baseline.

**Impact:** Android dependency versions stay deterministic until Gradle/device qualification is available.


## DECISION-006 — Index names are pinned in model state, not re-derived

**Date:** 2026-09-17

**Subject:** Nine `models.Index` declarations carried no explicit `name`, so Django derived new hash-based names that did not match the names already created by the migrations and present in the database. The autodetector proposed nine `RenameIndex` operations.

**Decision:** Pin the existing database index name explicitly in `models.Index(name=...)` for every index, so model state, migration state and the database agree. Eight of the nine keep their exact existing name.

The ninth is a deliberate exception: `facilities__prov_cat_status_idx` is 31 characters and `django.db.models.Index.max_name_length` is 30, so Django rejects it in model state with `models.E034`. It is the only index that had to be renamed, to `facility_prov_cat_status_idx` (28 characters). An explicit name was chosen over Django's derived `facilities__provinc_c99cb5_idx` because a pinned name never shifts again when the field list changes, whereas a derived hash does.

**Reason:** Renaming indexes that already work is pure churn on a production database. The correct direction is to make model state describe reality.

**Alternatives:** Accept the nine `RenameIndex` operations. Rejected: nine `ALTER INDEX` statements against a live database, with no functional gain, and the derived names would drift again on any field change.

**Impact:** `makemigrations` proposes zero index operations. Verified on an upgraded database: every pinned index kept its original `pg_class.oid`, and the one renamed index kept oid 26124 — a rename in place, not a rebuild.

## DECISION-007 — Model state is repaired to match the database, never the reverse, for business invariants

**Date:** 2026-09-17

**Subject:** `facilities/migrations/0003_owner_media_integrity` created `uniq_submitted_application_per_facility_kind`, a partial unique constraint enforcing `06-DATA-MODEL`: only one active submitted application of the applicable kind per facility. `FacilityApplication` had no `Meta` at all, so the autodetector proposed `RemoveConstraint`.

**Decision:** Declare the constraint in `FacilityApplication.Meta.constraints` with the identical name, fields and condition taken from the migration. The database and the migration stay untouched.

**Reason:** The constraint is a business invariant fixed by the specification. A drift between model and database is a defect in the model, not permission to drop a guarantee. Accepting the generated migration would have deleted the invariant while every existing gate still reported PASS.

**Alternatives:** Accept the generated `RemoveConstraint`, or re-invent an equivalent constraint under a new name. Both rejected: the first destroys the invariant, the second causes an unnecessary index rebuild.

**Impact:** `makemigrations` no longer proposes removal. The invariant is now covered by four connected tests that prove PostgreSQL itself rejects a second `SUBMITTED` application of the same kind while still allowing a `DRAFT` and a different kind.

## DECISION-008 — Expression helpers used by constraints live in one module only

**Date:** 2026-09-17

**Subject:** `pharmacy_duty/migrations/0001_initial` declared its own local copy of the `TstzRange` expression helper instead of importing the one in `pharmacy_duty/models.py`. The autodetector therefore proposed an endless `RemoveConstraint` plus `AddConstraint` pair on the duty exclusion constraint.

**Decision:** The migration imports `TstzRange` from `pharmacy_duty.models`. The constraint operation itself is byte-identical.

**Reason:** Proven by probe, not assumed. `TstzRange.deconstruct()` round-trips perfectly, so the helper was never the problem. `BaseExpression.identity` begins with `self.__class__`, so two structurally identical expressions built from two different Python classes can never compare equal, and the autodetector reports a change on every run. The two classes were `pharmacy_duty.models.TstzRange` and `pharmacy_duty.migrations.0001_initial.TstzRange`.

**Alternatives:** Add a custom `deconstruct()`. Rejected because the deconstruction was already stable and the real cause was class duplication. Accept the drop/recreate. Rejected because rebuilding a GiST exclusion constraint on a production table is not cosmetic.

**Impact:** Zero SQL. Verified on an upgraded database: `prevent_overlapping_duty_for_facility` kept `pg_class.oid` 27009 across the upgrade. No other migration in the repository declares a class, so this pattern does not repeat.

## DECISION-009 — Django `PermissionsMixin` stays, but is not authoritative (deferred removal)

**Date:** 2026-09-17

**Subject:** `accounts.User` inherits `PermissionsMixin`, which adds `is_superuser`, `groups` and `user_permissions`, while the platform's real authorization is the explicit `AdminRole` / `AdminPermission` / `HasAdminPermission` triple.

**Decision:** Keep `PermissionsMixin` for now. Accept the three `AlterField` operations on `groups`, `is_superuser` and `user_permissions`, having proven they are metadata only: `sqlmigrate accounts 0005` renders `-- (no-op)` for all three, with no type, default, nullability, relation or permission-behaviour change.

The authoritative source for admin authorization remains the project RBAC. `user_has_admin_permission` requires an active `UserAdminRole`; `is_superuser` grants nothing.

**Reason:** Removing the mixin means dropping fields and M2M tables, editing migration history, changing the `User` contract and touching Django auth internals. That needs an ADR and a security regression suite, which is far beyond a migration-convergence batch.

**Alternatives:** Remove it inside P2. Rejected as an unscoped architectural and security change.

**Impact:** One parallel authorization surface remains present but unused. See the technical debt register below.


## DECISION-010 — Generated API clients are committed, not built on demand

**Date:** 2026-09-18

**Subject:** `08-API-CONTRACT.md` requires generated clients and a committed schema hash, but does not say whether the client code itself is committed or produced during each consumer build.

**Decision:** Commit the generated TypeScript, Kotlin and Swift clients under `packages/api-*/generated/`, alongside the canonical `openapi/schema.yaml` and `openapi/schema.sha256`.

**Reason:** CI can then prove the committed clients still match the schema, which is the only way the drift gate means anything. A contract change becomes visible in the diff of the pull request that causes it, rather than appearing silently in someone's build output. Admin, Android and iOS can build without installing a code generator.

**Alternatives:** Generate during each consumer build. Rejected: three toolchains would each need the generator, the drift gate would have nothing to compare against, and a contract change would be invisible in review.

**Impact:** About 1000 generated files are tracked. Every one is a build artefact: hand-editing any of them is a defect, because the next regeneration discards the change and CI fails. `openapi/README.md` records the policy and the official regeneration commands.

## DECISION-011 — Operation ids are explicit and independent of Python class names

**Date:** 2026-09-18

**Subject:** drf-spectacular derived operation ids from the path and view, which produced five collisions that Django resolved with numeral suffixes such as `v1_admin_applications_retrieve_2`.

**Decision:** Declare an explicit `operation_id` on every operation, following `<area><Resource><Action>` in lowerCamelCase: `publicFacilitiesList`, `ownerFacilitySubmit`, `adminReviewApprove`.

**Reason:** Operation ids are part of the public contract and become method names in every generated client. Deriving them from Python class names means an internal rename silently breaks three clients, and numeral suffixes are both meaningless and unstable.

**Alternatives:** Keep the derived ids and accept the suffixes. Rejected: unstable identifiers in a published contract.

**Impact:** 84 unique, descriptive ids. A contract test rejects duplicates and numeral-suffixed names, so the collision cannot return.

## DECISION-012 — The contract documents the runtime, not the specification, where they disagree

**Date:** 2026-09-18

**Subject:** Three places where `08-API-CONTRACT.md` and the running service differ: the error envelope, the pagination envelope, and camelCase at the boundary.

**Decision:** Describe what the runtime emits, and record each divergence as a defect. Do not write a schema that describes behaviour nobody implements.

**Reason:** A generated client is built from the document. A schema that describes the specification rather than the service produces clients that break at the first response. Changing the runtime to match the specification is real product work with its own review; it is not something to slip into a schema batch.

**Alternatives:** Document the specified shapes. Rejected, it would ship a contract that lies. Change the runtime now. Rejected as out of scope and unreviewed.

**Impact:** The contract is honest today, and INT-035, INT-036 and INT-037 carry the reconciliation work.

**Closed:** 2026-09-18 by the CONTRACT ALIGNMENT batch. The runtime was changed to match the specification and the schema regenerated from it, so the divergence no longer exists. See DECISION-013 to DECISION-016.

---

## DECISION-013 — One DRF exception handler owns every error response

**Date:** 2026-09-18

**Subject:** Three unrelated error shapes reached clients: DRF's `{"detail": ...}`, DRF's field map, and a hand-rolled `{"error": {"code": ...}}` built in views. INT-038.

**Decision:** Install `core.exceptions.exception_handler` as `REST_FRAMEWORK["EXCEPTION_HANDLER"]`. It renders every failure as `{code, message, details, requestId}`. Views raise `DomainError` or `ConflictError` instead of returning an error `Response`, and twelve hand-built error bodies were converted.

**Reason:** A shape assembled per view cannot be kept consistent, cannot be described once in the contract, and cannot be audited for leaks. A single handler is the only place where "no stack trace, no SQL, no secret, no token material" can actually be enforced. Raising also rolls the surrounding transaction back, which a returned `Response` did not.

**Alternatives:** A base view class. Rejected, it does not cover exceptions raised in serializers, permissions, authentication or throttling. A middleware. Rejected, it runs after DRF has already rendered the body.

**Impact:** `requestId` comes from `RequestIdMiddleware`, so the value in the body, the `X-Request-ID` header and the server log are the same string. An unexpected failure is logged with its traceback under `django.request` and answered with a bare `INTERNAL_ERROR`; a 5xx `APIException` is deliberately not echoed either.

---

## DECISION-014 — The cursor is a token, and `previous` is not exposed

**Date:** 2026-09-18

**Subject:** `08-API-CONTRACT.md` specifies `{items, nextCursor, hasMore}`. DRF emits `{next, previous, results}` where the links are absolute URLs. INT-035.

**Decision:** `core.pagination.CursorPage` emits the specified three keys. `nextCursor` is the opaque cursor token extracted from DRF's link, not the link. `previous` is not emitted, because the specified envelope has no field for it.

**Reason:** The URL carries the scheme, host, path and every query parameter the caller sent. A client that persists a page token would persist all of that, and a host rewritten by a proxy would leak into stored client state. The token alone is sufficient and reveals nothing about routing.

**Alternatives:** Keep `previous` as a fourth key. Rejected, it would diverge from the specified envelope again, and no consumer exists that needs backward paging; the field can be added deliberately when one does.

**Impact:** Backward paging is unreachable until the contract adds a field for it. `ordering` must end in a unique column on every subclass, which is documented on the class.

---

## DECISION-015 — A malformed cursor is a validation error, not a 404

**Date:** 2026-09-18

**Subject:** DRF raises `NotFound` when `decode_cursor` fails.

**Decision:** `CursorPage.decode_cursor` converts it to a `ValidationError` on the `cursor` field, so the caller gets 400 `VALIDATION_ERROR` with `details.cursor`.

**Reason:** The cursor is client input in a query parameter. A 404 tells the caller the collection does not exist, which is false and sends a client debugging the wrong thing.

**Alternatives:** Leave DRF's behaviour. Rejected as actively misleading.

**Impact:** Covered by `test_a_mangled_cursor_is_a_field_error_not_a_missing_collection`.

---

## DECISION-016 — Wire names are translated at the boundary, never by renaming columns

**Date:** 2026-09-18

**Subject:** Admin list endpoints answered from `QuerySet.values()` and returned column names (INT-036), and `06-DATA-MODEL.md` calls the business-hour ordering field `sequence` while the column is `sort_order` (INT-037).

**Decision:** Do the translation in serializers with `source=`. Keep `QuerySet.values()` so a grid page still does not instantiate models. Rename no database column and write no `RenameField` migration.

**Reason:** A column rename rewrites a table, invalidates every existing query and index reference, and buys nothing the boundary cannot provide. The API contract and the storage schema are allowed to use different vocabularies; the serializer is where they meet.

**Alternatives:** `RenameField` for `sort_order` to `sequence`. Rejected, a migration for a naming preference. Drop `values()` and serialize model instances. Rejected, it would undo a deliberate performance choice.

**Impact:** The Admin serializers in `admin_console/schemas.py` are now the real output path rather than documentation. A structural test fails if any future response serializer in that app declares a field containing an underscore. `AdminCapabilities*` is the one remaining snake_case pair and is registered as INT-039.

---

## DECISION-017 — The launch baseline ships as a data migration, applied by shared code

**Date:** 2026-09-18

**Subject:** How the fourteen provinces and the health taxonomy reach a database. INT-006.

**Decision:** A data migration, `directory/0004_launch_baseline`, plus a `seed_launch_baseline` management command. Both call the same `apply_dataset`, so the logic exists once. The data lives in `directory/reference_data/launch_v1.py`, which holds nothing but values — no model imports, no QuerySets, no side effects.

**Reason:** A migration means a fresh database is correct without anyone remembering to run anything, and migration history then describes the product's required initial state. The command covers what a migration cannot: qualification, verifying a restored database, and recreating a canonical row that was removed. Duplicating the logic across the two would guarantee they diverge.

**Alternatives:** Migration only. Rejected, there would be no way to repair or verify a live database. Command only. Rejected, staging and production would depend on someone remembering. A fixture. Rejected, `loaddata` overwrites by primary key and would undo operator changes.

**Impact:** `apply.py` may only use plain field access and the default manager, because a historical model registry has no custom methods. `launch_v1` is immutable now that it has entered a shared migration; a future baseline is `launch_v2` and a new migration.

---

## DECISION-018 — Reference rows carry deterministic UUIDv5 primary keys

**Date:** 2026-09-18

**Subject:** Primary keys for canonical provinces, category groups and categories.

**Decision:** `uuid5(NAMESPACE, "<entity type>:<code>")` with a fixed project namespace hardcoded as a literal. The immutable `code` is the identity; the UUID is derived from it. If a row already exists under the right code but a different primary key, the seed reports `REFERENCE_ID_MISMATCH` and writes nothing.

**Reason:** The same province must be the same id in development, CI, staging, production and any restored database, so a client can cache an id and an operator can compare two environments directly. Rewriting a primary key to force agreement is the one thing a seed must never do quietly: foreign keys already point at it.

**Alternatives:** Random UUIDs with `update_or_create` on `code`. Rejected, every environment would disagree on ids. Rewriting the primary key on mismatch. Rejected as unsafe under live foreign keys.

**Impact:** Raqqa is always `58fea422-2cae-5f93-a396-b81a9a7d37e2`. A mismatch is a loud qualification failure that a human resolves.

---

## DECISION-019 — The seed never overrules an operator, and has no force mode

**Date:** 2026-09-18

**Subject:** What a second run of the seed is allowed to change.

**Decision:** Launch defaults apply at creation only. Once a row exists it is left alone — `active`, `public_enabled`, `owner_registration_enabled`, names and sort orders included. The seed repairs a missing canonical row and verifies identity; that is all. There is no `--force` and no `--reset`.

**Reason:** Activating Aleppo or switching a category off is an operational decision taken in the Admin. A seed that re-imposed launch defaults would silently revert it, and would do so most destructively in production, where the divergence from launch is largest. A command able to flatten production configuration should not be one keystroke away.

**Alternatives:** `update_or_create` with the full launch defaults. Rejected for exactly that reason. A `--force` flag. Rejected in this batch; forced reconciliation for disaster recovery belongs in a separate, audited command.

**Impact:** `--check` reports what is missing and exits non-zero without writing, for use as a gate. Proven by a test that activates a province, flips two switches, re-runs the seed and asserts none of it was reverted.

---

## DECISION-020 — No city, neighborhood or verification requirement is invented

**Date:** 2026-09-18

**Subject:** The boundary of the launch dataset.

**Decision:** Seed provinces, one category group, five health categories, their capabilities and all seventy province switches. Seed no city, no neighborhood, no boundary, no coordinate, and no `VerificationRequirement`. No facility, user, rating, duty shift, opening hour, advertisement or test phone.

**Reason:** The specification names provinces explicitly and gives no authoritative city or neighborhood dataset, so any city list would be invented geography presented as canonical; `city` and `neighborhood` are nullable for that reason. A `VerificationRequirement` is not UI configuration — it decides what blocks submission, approval and re-verification — so creating a mandatory one would be a product policy decision taken inside a migration, where the Admin cannot see it and no reviewer signed it off.

**Alternatives:** Seed Raqqa's cities. Rejected as invented data. Seed a storefront-photo and business-card requirement as the specification sketches. Rejected: the specification does not settle whether either is required, how many files each takes, or whether a pharmacy licence document is needed instead.

**Impact:** **LAUNCH_POLICY_PENDING.** Pharmacy verification requirements must be configured and qualified before owner onboarding is opened publicly in production. This blocks neither backend nor Admin development; it is a production launch gate. Recorded in the debt register below.

---

## DECISION-021 — The Admin learns its permissions from `/admin/me/`, resolved in one place

**Date:** 2026-09-18

**Subject:** INT-042. Nothing returned the caller's own Admin permissions.

**Decision:** `GET /api/v1/admin/me/` returns `userId`, `displayName` and the deduplicated, sorted permission codes. It is guarded by holding an active Admin role, not by a permission code, because requiring one would be circular. The codes come from `accounts.rbac.admin_permissions_for`, beside the check `HasAdminPermission` already used.

**Reason:** A navigation built from one resolver and enforced by another would eventually disagree. Reading permissions through `adminUserRetrieve` plus `adminRolesList` needs `admin.users.read` and `admin.roles.read`, which most operators will not hold. Probing every route for a 403 is slow and noisy.

**Alternatives:** Probing. Rejected. Returning role names. Rejected: the UI branches on permissions, and role names are labels an administrator can rename.

**Impact:** Django `groups`, `user_permissions` and `is_superuser` grant nothing, asserted by test. An operator whose roles carry no permissions gets an empty list, which is a valid state. The UI gate is presentation; every endpoint still enforces.

---

## DECISION-022 — One BFF route, one registry of named operations, reads and writes apart

**Date:** 2026-09-18

**Subject:** How thirteen screens reach the backend without hand-written transport.

**Decision:** `lib/api/operations.ts` names every call and maps it to a generated client method. `/api/admin/[operation]` serves `READS` over GET and `WRITES` over POST. An unregistered name is a 404 before any network call. Evidence content has its own streaming route because it is a file, not an envelope.

**Reason:** §2 forbids scattered fetch calls and hand-written DTOs. A registry keeps the whole transport surface in one reviewable file, and separating reads from writes means a mutation has no GET form, so a link, a prefetch or an image tag cannot trigger one.

**Alternatives:** One route file per operation. Rejected: forty near-identical files. A generic proxy to Django. Rejected: it would forward anything the browser asked for.

**Impact:** No screen names an endpoint or builds a query string. Regenerating the client touches the registry only where a signature changed.

---

## DECISION-023 — Session material lives in HttpOnly cookies named from the public origin

**Date:** 2026-09-18

**Subject:** INT-012 and §4–§7.

**Decision:** Access and refresh material are both `HttpOnly`, `SameSite=Strict`, `Path=/`, no `Domain`. Whether they carry `Secure` and the `__Host-` prefix is decided by `ADMIN_PUBLIC_ORIGIN`: https gives both, loopback http gives neither, http on any other host stops the process at boot. Refresh coalesces per session and a call retries at most once, only after a 401.

**Reason:** `NODE_ENV` knows nothing about the transport and was the original defect. The prefix and `Secure` are one decision because a browser refuses a `__Host-` cookie without `Secure`. The refresh secret rotates and reuse is treated as theft, so two concurrent refreshes must not both spend it.

**Alternatives:** An explicit `ADMIN_INSECURE_COOKIES` switch. Rejected after it reintroduced `NODE_ENV` as a veto. Access token in memory on the client. Rejected by §6.

**Impact:** The browser never holds a token. Misconfiguration fails at boot rather than at first login.

---

## DECISION-024 — Scripts are allowed by a per-request nonce, and every page renders per request

**Date:** 2026-09-19

**Subject:** INT-045. A static `script-src 'self'` blocked the App Router's inline scripts and the console never hydrated.

**Decision:** `proxy.ts` generates a nonce per request and sends `script-src 'self' 'nonce-…' 'strict-dynamic'`; Next stamps it on its own scripts. The root layout calls `connection()` so no page is prerendered without a request. Styles keep `'unsafe-inline'`. HSTS and `upgrade-insecure-requests` are sent only on https.

**Reason:** This is the documented approach for Next 16 and it avoids `'unsafe-inline'` for scripts. A prerendered page has no request and therefore no nonce.

**Alternatives:** `'unsafe-inline'` for scripts. Rejected: it gives up most of what CSP is for. Hashes. Rejected: the RSC payload differs per render.

**Impact:** An operations console gains nothing from static prerendering, so the cost is nil. The fault was invisible to every HTTP-level check and was found by the first real browser run.

---

## DECISION-025 — Retirement, not deletion, for taxonomy and verification policy

**Date:** 2026-09-18

**Subject:** INT-018. Which lifecycle operations Cycle J needs.

**Decision:** Category groups and categories gain create and update; verification requirements gain update; advertisements gain update. None of the first three gains delete. `code` and `slug` are refused on update, visibly. A requirement cannot move between categories.

**Reason:** Facilities, applications and evidence reference these rows under `PROTECT`, the specification offers no delete for them, and `active = false` already removes a row from every public surface. Silently ignoring a changed `code` would let an operator believe a rename worked.

**Alternatives:** Full CRUD. Rejected by §19: the goal is an operational Admin, not a CRUD generator.

**Impact:** `LAUNCH_POLICY_PENDING` stays open. The tool to configure pharmacy verification exists; the policy is not decided by its existence.

---

## DECISION-026 — The end-to-end suite spends logins sparingly rather than loosening the throttle

**Date:** 2026-09-19

**Subject:** The backend throttles login at ten attempts a minute and the suite tripped it.

**Decision:** `global-setup.ts` signs each operator in once through the real BFF; tests reuse those sessions. Only tests about the session itself create their own. The runner flushes Redis per run.

**Reason:** Raising the throttle for tests would weaken production to suit a test harness.

**Alternatives:** A test-only throttle setting. Rejected for the same reason.

**Impact:** A full run spends seven logins. A session shared this way must outlive the suite; access tokens last about fourteen minutes and the suite takes under one.

---

# Technical Debt Register

Mandatory before staging or production closure. None of these blocks P2 or P10.

## DEBT-001 — Ruff baseline

**Recorded:** 2026-09-17 · **Baseline:** 106 issues at `bc12f4d`, 104 after this batch.

Concentrated in `directory/models.py`, which is written in a compressed style with semicolons and lines up to 249 characters against a 100 limit. Rule: **no new lint debt** — any file touched must not increase the count. Forbidden remedies: disabling rules broadly, blanket ignores, or weakening CI to make the result green.

## DEBT-002 — Mypy baseline

**Recorded:** 2026-09-17 · **Baseline:** 556 errors in 82 files.

**Correction, 2026-09-18:** the 556 figure was measured on the Windows host, where the
`django-stubs` plugin cannot import `directory_backend.settings.test` because GDAL is
absent, so it silently analyses less. Measured in the backend container, where the plugin
loads, the same tree at `c9a4cae` reports **797 errors in 100 files across 191 source
files**. That is the figure to compare against from now on; earlier reports are not
rewritten, and 556 is kept above so the record stays traceable. After the CONTRACT
ALIGNMENT batch the container figure is **789**.

`[tool.mypy] strict = true` is declared while the codebase is largely unannotated. Rule: **no new type debt**. Removing `strict` requires a recorded ADR; a blanket ignore is forbidden. New modules are expected to be strict-clean; every module added in the CONTRACT ALIGNMENT batch is.

## DEBT-003 — Evaluate removal of Django `PermissionsMixin` after contract and runtime recovery

**Recorded:** 2026-09-17 · See DECISION-009.

The project must not keep two parallel authorization surfaces without need. Before the dedicated phase, prove all of the following:

- no use of `user.has_perm`,
- no use of `user.has_perms`,
- no reliance on Django `ModelBackend` permissions,
- no use of `groups` or `user_permissions`,
- `is_superuser` grants no bypass on any authorization path,
- no Django Admin depends on them.

Requires an ADR and a security regression suite. Until then the mixin stays present but non-authoritative.

## LAUNCH_POLICY_PENDING — Pharmacy verification requirements

**Recorded:** 2026-09-18 · See DECISION-020.

`launch_v1` seeds no `VerificationRequirement`, deliberately. Before owner onboarding is opened to the public in production, the pharmacy requirements must be decided, configured through the Admin and qualified: which documents are required, how many files each accepts, and whether a licence document replaces or accompanies the storefront and business-card proofs the specification sketches.

This is a **Production Launch Gate**, not a development blocker. Backend and Admin work proceed without it.
