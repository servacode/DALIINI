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

## DECISION-027 — The generated Kotlin client is compiled as source, and only `api/` sees it

**Date:** 2026-09-19

**Subject:** How the P10 Kotlin client enters the Android build.

**Decision:** `:core:network` adds `packages/api-kotlin/generated/src/main/kotlin` as a source directory, as `:core:designsystem` does with the generated tokens. Within the module, only `core/network/api/` references generated types: one configured `GeneratedClient`, one error mapper, one file of mappers, and adapters behind `PublicApiBoundary`, `OwnerApiBoundary` and `AuthApiBoundary`. Repositories and screens see domain models and `AppError` only.

**Reason:** The spec's module list has no place for a separate client module, and a copy would drift from the committed artefact the drift gate protects.

**Alternatives:** A published artefact (no repository to publish to); a `:core:api` module (outside the spec's module list).

**Impact:** Regenerating the client changes the app with no copy step. A contract change that renames a generated type fails the build in `api/`, nowhere else.

---

## DECISION-028 — A JVM build over the Android sources, while Google Maven is out of reach

**Date:** 2026-09-19

**Subject:** Google Maven answers 404 from this machine, so nothing Android can be built.

**Decision:** `apps/android/jvm-verification` is a second Gradle build that compiles the generated client and every platform-free Android source file straight from the modules' directories, with the app's own version catalog, and runs the module unit tests and a connected suite against the real backend. It is evidence for the transport and data layers only and is never reported as `BUILD_VERIFIED`.

**Reason:** Without it, the batch would have written thousands of lines of Kotlin that nothing compiled. It found six defects the Android build could not have found either, because they are in the contract and the backend.

**Alternatives:** Downgrading AGP or using an unvetted mirror, both refused by the owner; waiting for a network that reaches Google Maven, which leaves the binding unverified.

**Impact:** The files it must leave out are listed in its `build.gradle.kts`. Screens and ViewModels remain uncompiled until the Android build runs.

---

## DECISION-029 — Two HTTP clients, each with its own dispatcher

**Date:** 2026-09-19

**Subject:** Where the refresh authenticator runs, and what it can block.

**Decision:** An anonymous client for discovery, sign-in, registration, recovery and refresh, with no token and no authenticator. An authorized client for the user's own requests, with the token interceptor and the refresh authenticator. Each has its own OkHttp dispatcher.

**Reason:** A wrong password must not trigger a refresh; public data must not carry a token that could turn it into a 401; and the authenticator blocks the thread of the request that failed, so on a shared dispatcher five such requests would occupy the per-host limit that the refresh call itself needs. The unit test deadlocks when the dispatchers are shared.

**Impact:** A refresh can always run, however many requests are waiting for it.

---

## DECISION-030 — An access token is bound to its session

**Date:** 2026-09-19

**Subject:** INT-051 and INT-054.

**Decision:** Access tokens carry `sid`. REST authentication and the WebSocket resolve the live session through one query, `live_sessions_for`; a socket re-checks before delivering a user or admin event and closes with 4401 if the session is gone. A detected refresh replay is recorded in a transaction that commits before the refusal is raised.

**Reason:** Revocation and replay detection were in the code but did not take effect.

**Alternatives:** Shorter access tokens, which narrow the window without closing it.

**Impact:** One indexed query per authenticated request, replacing the user lookup it already made. Tokens issued before this change stop working; there is no production traffic to disturb.

---

## DECISION-031 — The generated serializer is configured before first use

**Date:** 2026-09-19

**Subject:** Three generator defaults that are wrong for this API.

**Decision:** `GeneratedClient` sets `encodeDefaults = false` and `explicitNulls = false`, registers a contextual serializer for free-form maps, and puts a converter first that sends a UUID multipart part as plain text. Its OkHttp client is always the app's.

**Reason:** The defaults sent every unset PATCH field as null (INT-062), could not decode a free-form map, quoted the UUID part so the backend refused it, and would have logged request bodies — passwords included — once a logger was attached.

**Impact:** A field cannot be cleared by sending null through a generated PATCH; none of the app's flows needs to.

**Note, 2026-09-19:** since INT-068 the evidence upload's `requirementId` part is an integer, which Retrofit's scalars converter already sends as plain text; no generated operation has a UUID part today.

---

## DECISION-032 — Environments are flavors, and cleartext exists only in `local`

**Date:** 2026-09-19

**Subject:** Where the app finds its backend (§5).

**Decision:** `local`, `staging` and `production` flavors. `local` defaults to the emulator's host alias and may use cleartext only to it, through a network security config that exists in that flavor alone. `staging` and `production` take their addresses from Gradle properties or the environment and default to placeholders, which `ApiEnvironment` refuses at the first request rather than at start-up. Only the production release tasks feed Play validation.

**Reason:** No developer machine address, no localhost and no placeholder may reach a shipped build.

**Impact:** A phone on a local network needs its server's address added to the local network security config.

---

## DECISION-033 — Core library desugaring for `java.time`

**Date:** 2026-09-19

**Subject:** The generated client uses `java.time`; `11-ANDROID-KOTLIN.md` sets `minSdk 24`, which predates it.

**Decision:** Desugaring in both conventions, `desugar_jdk_libs` 2.1.5.

**Reason:** Changing the minimum SDK is a product decision; regenerating the client with string dates would move date parsing out of the one boundary that should own it.

**Impact:** The desugaring artifact comes from Google Maven and could not be downloaded here to confirm; the first Android build confirms it.

---

## DECISION-034 — The app offers an owner control only when it knows the category supports it

**Date:** 2026-09-19

**Subject:** INT-056: owner facility responses carry no capabilities.

**Decision:** Duty controls appear when the owner configuration for the facility's province says its category supports duty, and are hidden otherwise, including when the category is absent from the configuration.

**Reason:** Hiding a control that would work is recoverable; offering one that the backend then refuses is the worse failure, and the backend refuses it either way.

**Alternatives:** Capabilities on owner facility responses, which is the right fix and is recorded as INT-056.

**Superseded 2026-09-19:** INT-056 is fixed. Each owner facility carries its category's capabilities, and the app reads them from the facility. The rule stands: a control whose capability is not confirmed is not offered.

---

## DECISION-035 — The official Gradle wrapper, verified by checksum

**Date:** 2026-09-19

**Subject:** INT-019: the repository had no Gradle wrapper.

**Decision:** `apps/android` carries the Gradle 9.6.0 wrapper exactly as Gradle publishes it. The jar's SHA-256 matches gradle.org's published value, and `distributionSha256Sum` pins the distribution. `gradlew` is LF and executable, `gradlew.bat` is CRLF, and the jar is binary (`apps/android/.gitattributes`).

**Reason:** AGP 9.4.0 requires Gradle 9.6.0 at least. A wrapper that is hand-made, or not checked against a checksum, is an unverified executable in the build.

**Impact:** Every Android build, including `jvm-verification`, runs through `./gradlew`. The wrapper downloads the distribution from services.gradle.org, which answers on this network.

---

## DECISION-036 — Public media has a permanent address; private evidence has none

**Date:** 2026-09-19

**Subject:** INT-061 and INT-065: public images were served with signatures that expire, and advertisement images pointed at a route that does not exist.

**Decision:** Two classes of object, in two buckets. PUBLIC_MEDIA (facility photos and advertisement images) is addressed through one function, `public_media_url`, from `S3_PUBLIC_MEDIA_BASE_URL`. That setting is required in production, where it names the CDN, and defaults to the public bucket in development. The address is never signed, never expires and is the same on every read. PRIVATE_EVIDENCE stays in its own bucket, which grants no anonymous read. It has no public address, and its bytes reach an operator only through the backend: permission-checked, audited, `no-store`, and named after nothing in storage (INT-066, INT-067). Keys are unguessable (`uuid4`) in both.

**Reason:** A photo that a cache-first screen has stored must still load a day later, and an operator must never receive a link to a private document that outlives the check that allowed it.

**Alternatives:** Pre-signed URLs with a long expiry, which still expire and differ on every read, and so defeat any cache. Making the whole bucket public was ruled out by the batch instruction.

**Impact:** A photo uploaded to a draft is readable by anyone who has its address before the facility is approved. The address is not guessable and is shown only to the owner. Production needs a public-read bucket or CDN origin for `directory-public` that grants object reads only, never listing: listing would expose every key, a draft's photo included (INT-081). The golden path and the local stack prove that policy on MinIO: an object reads 200 and an anonymous listing is refused with 403.

---

## DECISION-037 — A push token belongs to a session

**Date:** 2026-09-19

**Subject:** INT-057: registering a device for push.

**Decision:** `PUT /account/push-token/` registers the calling session's device and replaces any earlier token from the same session and platform. `POST /account/push-token/unregister/` stops pushes to a token. Both answer 204 and never return the token. When a session ends, through logout, revocation, a detected refresh replay, a password reset or account deletion, its tokens stop receiving pushes. A provider's refusal deactivates the token. The token is stored only as Fernet ciphertext and a keyed digest, and never reaches a log, the Admin or analytics.

**Reason:** A token outliving the session that registered it would deliver a signed-out user's notices to whoever holds the phone next.

**Impact:** The app registers again after each sign-in; `PushRegistrationCoordinator` does so.

---

## DECISION-038 — Firebase is configured by the build, never committed

**Date:** 2026-09-19

**Subject:** FCM on Android without committing a Firebase configuration.

**Decision:** `firebase-messaging` 25.1.1 is integrated without the google-services plugin. The four client values (project id, application id, API key and sender id) come from Gradle properties or the environment, and `FirebaseApp` is initialised only when all four are present. Otherwise the app runs with push off. A push carries identifiers only; the app shows a neutral notice and fetches the substance over REST.

**Reason:** No Firebase project exists for this product yet. A fake `google-services.json` or placeholder key would pretend otherwise and could reach a build.

**Impact:** `FCM_PROVIDER_DELIVERY = EXTERNAL_NOT_VERIFIED` until a Firebase project is created and its values are supplied to a build. The Android 13 runtime notification permission request is not implemented yet; without it, the notice stays silent where notifications are off.

---

## DECISION-039 — The hand-off is proven across the three real parties

**Date:** 2026-09-19

**Subject:** How to prove Android -> Admin -> Android without an Android build.

**Decision:** `scripts/e2e-android.sh` runs the app's data layer, from `jvm-verification`, against Django, PostGIS, Redis and MinIO in two phases (`HANDOFF_PHASE`). Between them, the production Admin build runs in Chromium: the owner's evidence is submitted from the JVM, opened and approved in the browser, and the result is read back from the JVM. The fixtures add one verification requirement, prefixed `e2e-m-`, to the test database only.

**Reason:** The hand-off crosses three processes, and each boundary has hidden a defect. INT-068 was invisible until a requirement existed. The launch baseline must not gain one before the pharmacy policy is decided (LAUNCH_POLICY_PENDING).

**Impact:** This proves the data layer and the backend, not the Android UI. `jvm-verification` proves portable and data-layer integration only; the Android build, Compose, instrumentation and a device are separate gates.

---

## DECISION-040 — Compile against API 37, target API 36

**Date:** 2026-09-19

**Subject:** The first real Android build (CI run 35425779550) failed `checkLocalDebugAarMetadata`: 21 of the pinned libraries — Navigation 2.10.1, Compose UI 1.12.1, Foundation and Animation 1.12.0, Lifecycle 2.11.0, Core 1.19.0 and Coil 3.6.2 — require consumers to compile against API 37 or later.

**Decision:** `compileSdk = 37` in both Android conventions. `targetSdk` stays 36 and `minSdk` stays 24.

**Reason:** Both `02-BASELINE-DECISIONS.md` ("compileSdk: 36 (or higher compatible stable without changing target policy)") and `11-ANDROID-KOTLIN.md` ("compileSdk 36+") allow it. Downgrading the libraries instead is forbidden by the batch instruction. `compileSdk` only decides which APIs the code may reference; runtime behaviour follows `targetSdk`.

**Impact:** None at runtime. Code that uses an API above `minSdk` still needs a version check, which lint enforces.

---

## DECISION-041 — One stable debug signing key, kept as a secret

**Date:** 2026-09-19

**Subject:** INT-091. Every CI run signed the debug APK with a key generated on its own runner, so a device could not update from one build to the next without losing the app and its data.

**Decision:** Every debug build is signed with one project debug key: RSA 2048, PKCS12, alias `directory-debug`, certificate SHA-256 `21e9ff5c409a14cec929bb078d53625f4e3142ef1bb027337360e9e6d5b62caa`. `SECURITY.md` counts signing keys as secrets, so the keystore is never committed. CI reads it from the repository's Actions secrets (`DIRECTORY_DEBUG_KEYSTORE_BASE64`, `_KEYSTORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`). An authorised machine points Gradle at its private copy through the same four names. The workflow refuses to build without the secrets and fails when an APK carries any other certificate. The fingerprint is public and is pinned in the workflow.

**Reason:** The redesign that follows produces many builds for one device. Each would otherwise need an uninstall and cost the device's local state.

**Boundaries:** Debug builds only. This is not the upload key or the Play signing key, and release signing is unchanged (`ANDROID_UPLOAD_*`). The key authenticates nothing on the backend.

**Impact:** The app installed from run 35430793735 carries a key that existed only on a destroyed runner. Moving that device to the stable key takes one final uninstall. The keystore's only full copy outside GitHub is on the machine that created it (`~/.android/directory-platform-debug.keystore`), and it should be backed up privately. GitHub secrets cannot be read back.

## DECISION-042 — A province carries the point its map opens on

**Date:** 2026-09-19

**Subject:** INT-092. The map and the owner location picker opened on the whole world, because nothing in the system says where a province is. `06-DATA-MODEL.md` gives a province an optional MultiPolygon geometry; the implementation has none, and no authoritative boundary data exists in the project.

**Decision:** `Province.map_center` is a nullable PostGIS point (SRID 4326). It is reference data, like the launch baseline: the frozen module `directory/reference_data/province_map_centers_v1.py` gives Raqqa the centre of Raqqa city (35.9528, 39.0085). Migration `directory/0005` and `seed_launch_baseline` write it through the same `apply_map_centers`, which never overwrites a centre that is set. The API serves it as `mapCenter` (the shared `Coordinates` shape, or null) in `publicProvincesList` and in the province of `ownerConfigRetrieve`; the shared `NamedRef` is unchanged. The Android fixtures read the same value instead of repeating it. On the device the map opens on the user's position when it is known without prompting, else on this centre, at a city-level zoom; that choice lives in one place (`MapCameraPolicy`), not in a composable.

**Reason:** The backend owns geographic truth (PostGIS), a province activated later needs its centre without an app release, and a coordinate written into several screens would drift. A boundary polygon would also answer the question, but inventing one is worse than carrying the one point a map needs.

**Boundaries:** Only Raqqa has a centre. A province without one serves null, and the app then keeps the map's default camera rather than guessing. A centre for another province is a new frozen module and migration, or an operator's change. The boundary geometry in the specification stays open.

## DECISION-043 — The splash is the system splash handing over to the app's own

**Date:** 2026-09-19

**Subject:** UI/UX redesign, Screen 01. The app had no splash of its own: Android showed its default placeholder icon on a grey-white window, then the app showed a spinner under a grey status bar, then Home.

**Decision:** The launcher activity starts in `Theme.Directory.Starting`, built on the AndroidX SplashScreen library (`androidx.core:core-splashscreen` 1.2.0). It gives Android 12+ and the older versions the same brand mark on the same background (one vector whose margin is inside it, because the system scales a splash icon into its own box), and `installSplashScreen()` hands the window over to the unchanged `Theme.Directory`. The app's splash (`DirectorySplashScreen`, shown by the bootstrap destination) starts from the same colour and the same mark in the same place: the window's centre, at 120 dp. It then adds the name, a one-line tagline, a secondary footer and a light ping under the pin. It paints the system bars in its background only while it is on screen. It waits for nothing: navigation leaves as soon as bootstrap, which reads the device only, has an answer. The token generator also emits the colours as Android resources, so XML (the mark, the launch theme) uses tokens rather than hex values.

**Reason:** A splash drawn only in Compose appears after the system's, so the two have to match or the hand-over flashes. The library is the supported way to set Android's own splash, with one configuration for every version.

**Boundaries:** The mark and the copy are placeholders until approved (`docs/design/BRAND-ASSETS.md`). No launcher icon is added here. Other screens keep their theme and bars; the grey status bar of Home is Home's until its own redesign.

## DECISION-044 — A first run, once, and never again after a redesign

**Date:** 2026-09-20

**Subject:** UI/UX redesign, Screens 02 and 03. The app had no welcome and no screen for the location question: a first run landed on Home, which asked for a province, and the location was only offered by a button on Home, opening the system dialog with no explanation.

**Decision:** Two screens run once, in order, before Home: a welcome (`DirectoryRoute.Welcome`) and the location question (`DirectoryRoute.LocationPermission`). A device that has been through them records it in one preference, `welcome_completed`; a device that already carries a selected province is treated as having been through them, so no one who installed the app earlier is sent back through onboarding by a redesign. The location screen explains what the location buys — nearest-first order, distances, a map on the right area — and asks only when the user presses the button; "ليس الآن" is a second choice beside it and leaves the offer on Home open. Either answer ends the first run. The permissions asked are the two foreground ones, from `FOREGROUND_LOCATION_PERMISSIONS`, which is also what any other screen launches.

**Reason:** The system dialog alone does not say why an app wants a location, and a first-time user met a province picker before knowing what the app was for. Doing it once, with a recorded answer, keeps the rest of the app's flow untouched.

**Boundaries:** Background location stays forbidden (`15-SECURITY-PRIVACY.md`); nothing here asks twice, and a refusal keeps the province and the whole directory, costing only distances and nearest-first order. Home's own location offer, the province picker and every route after Home are unchanged. The copy is provisional until product copy is approved.

## DECISION-045 — One design system, and screens that draw only what the API has

**Date:** 2026-09-23

**Subject:** The UI redesign of the whole Android app against the approved references in `docs/design/ui-reference/`.

**Decision:** `core:designsystem` owns the visual language: Tajawal bundled and set once in `DirectoryTheme`, every measurement and colour from the generated design tokens, the app's own 31-icon vector set, and the components every screen is assembled from — page frame, top bar, bottom bar, cards, buttons, fields, chips, facility rows and cards, category and action circles, photo pager, star row and picker, status pill, step indicator, and one shape each for loading, empty, error, offline, permission and confirmation. No screen names a font, a colour, a corner or a spacing of its own; the qualifier already forbids a hardcoded colour anywhere in Android source.

**Reason:** A reference shows a look; only one place can hold it. Screens that style themselves drift apart within a release, and the approved references are a single visual language, not twenty-eight of them.

**Boundaries — what the references show and the product does not have:** saved facilities (Screen 16), a notification inbox (Screen 17), written reviews and other people's ratings (Screen 09), owner metrics (Screen 18), a distance filter (Screen 07), a picture on a list row, and an account picture (INT-017). None of these exist in the API, so none of them is drawn: the bottom bar carries three destinations rather than four, the ratings screen is the user's own stars, and the owner's dashboard shows no counts. Mock names, photos, distances and figures in the references are not product truth and none were copied into the app. The account screens ask for the phone and password the auth contract defines, not the email and "remember me" the reference sketches. Nothing in this batch changed a ViewModel's contract, a repository, a use case or a route's meaning; the one behaviour added is a dial intent from a facility's published phone number, which asks the system dialer and calls nothing itself.

---

## DECISION-046 — The app knows where the user is, and says so

**Date:** 2026-09-24

**Subject:** The province a user browses, and the header that names it.

**Decision:** The app resolves the device's own position against the platform's own geography —
the city and neighbourhood boundaries already seeded in PostGIS — through one public endpoint,
`public/locations/resolve`, and shows the answer ("الرقة", or "الرقة — المشلب") as the header.
The order is: the device's position, then the last place resolved for this device, then the
province the user once chose, and only then the province picker. No external geocoder is called,
the coordinate is not stored, and the resolver asks at most once a minute and only when the
device has actually moved about 500 m.

**Reason:** A first-time user was met by a list of provinces before they had seen anything the
app is for. The phone already knows where it is, and the platform already knows what it calls
that place — the same name every list and filter is scoped by. Resolving it server-side keeps
the name and the data in agreement, which a device-side geocoder could not promise.

**Boundaries:** The location stays optional and foreground-only. A refusal costs the distances,
the nearest-first order and the automatic header, and nothing else; the province picker remains,
reachable from the header and from the profile. A point outside every province the platform
serves resolves to nothing rather than to the nearest guess from across the country.

---

## DECISION-047 — Quick filters are the backend's own query

**Date:** 2026-09-24

**Subject:** "الأقرب إليك", "مفتوح الآن" and "مناوب الآن" on Home and on the map.

**Decision:** Each chip is the directory query the backend already takes, with the flags it
already understands, for the whole province rather than one category. The map's filter bar and
the list's chips send the same parameters to the same server-side filters, so a map and a list
asked the same question give the same answer. Nothing about opening hours or duty shifts is
computed on the device. "الأقرب إليك" is offered only while a position is known, because
distance is the backend's to compute.

**Reason:** These three questions are why people open the app, and they were buried in a filters
screen. Reusing the existing query keeps one ordering rule, one availability rule and one
pagination contract instead of a second, quietly different implementation on the client.

**Boundaries:** The facilities endpoint now accepts a request without a category — it already
behaved that way; the contract says so now — while the province remains required, as every list
in this product is scoped by one.

---

## DECISION-048 — Every word the app says is a resource, and a view model says none of them

**Date:** 2026-09-25

**Subject:** The 315 Arabic sentences that were written into Kotlin across seventeen modules.

**Decision:** Each module reads what it says from its own `res/values/strings.xml`, through a
`…Copy` object whose members are read in composition. Words more than one module needs — the days
of the week, the mark between list items, an error's sentence, an owner's status, whether a
facility is open, how far it is, from when to when — are in the design system's `strings.xml` and
read through `DirectoryWords`, `OwnerWords`, `AvailabilityWords`, `appErrorText` and `closureText`.
Nothing in `core/model` holds a sentence: the model names what happened — `AppErrorMessage`,
`OwnerAction`, `RoundedDistance`, `ManeuverPhrase`, `OnboardingNotice`, `HomeEmptyReason`,
`SpokenDistance` — and a renderer in the design system turns the name into the reader's language.
A view model keeps the `AppError` in its state rather than a sentence built from it.

The app's own name stays a Kotlin constant (`DirectoryBrand.NAME`), because a proper noun is not a
translation and the launcher's label in the manifest must agree with it; `BrandNameTest` holds the
two together. The voice's rule about which street names an Arabic synthesiser can read stays in
Kotlin too, because it is a judgement about characters rather than a sentence a translator could
write.

`check_words` in `apps/android/scripts/qualify-source.py`, a CI gate, fails on an Arabic literal
anywhere in Kotlin outside tests, with those two exceptions named in it.

**Reason:** A second language must be a second file, not a search through every screen; and a
sentence built when a request failed is frozen in the language the phone had at that moment. The
split also keeps the decisions testable without Android — the platform-free harness compiles and
tests every one of the enums above — and stops a test from asserting Arabic prose, which broke
every time a wording was improved.

**Boundaries:** No Arabic `values-ar` directory is added: the words stay in the default
`values/`, which is what a single-language app reads, and a second language becomes
`values-en/strings.xml` beside it with no Kotlin change. Test sources and the harness's
`connectedTest` still name Arabic places and read Arabic answers back, because that is the data
under test rather than what the app says.

---

## DECISION-049 — The province's map is kept on the device, and fetched only on a free connection

**Date:** 2026-09-26

**Subject:** What happens to the map and to a trip when the connection goes, which in Syria it does.

**Decision:** The app keeps one offline pack: the province the reader chose, a box of
`PACK_RADIUS_KM` (25 km) around the point that province's map opens on, zooms `PACK_MIN_ZOOM` to
`PACK_MAX_ZOOM` (6 to 14). It is written into MapLibre's own offline database, which is the store
the map already reads from, so a tile kept costs one fetch and is never fetched again.

It is fetched automatically, but only on a connection the system reports as unmetered, and never
again once the reader has deleted it — a deletion is stored as `offlineMapDeclined` and asking for
the pack in settings withdraws it. A download the app started stops when that free connection goes;
one the reader asked for does not. A download that stopped carries on from where it was rather than
starting over. A connection failure is retried; a server refusal or an oversized pack is not.

The arithmetic (`MapPack.kt`) and the policy (`mapPackAction`) are pure Kotlin the platform-free
harness compiles and tests. `OfflineMapPacks` is the MapLibre machinery and knows nothing about
provinces; `OfflineMapCoordinator` in the app module joins the chosen province to it.

**Reason:** A computed route is guided from the phone — the engine walks the line it was given — so
a cut connection does not end a trip by itself. What it takes away is the map under the line, and a
navigator over an empty grey field is not a navigator. Twenty-five megabytes fetched once on a
Wi-Fi is the whole difference, and twenty-five megabytes taken out of a Syrian mobile bundle without
being asked is not something this app gets to do, which is why both halves of the rule exist.

**Boundaries:** A province whose `mapCenter` the platform has not set gets no pack: a box centred on
a guess is megabytes of somewhere the reader is not. A trip that leaves the box falls back to
whatever the ambient cache holds, and above zoom 14 the map draws the zoom-14 tile scaled up, which
is how every offline map behaves at its edge. Routing itself still needs a network: a trip that is
already running survives, and a new one cannot be computed offline. The download lives in the app's
process — there is no background worker — so it advances while the app is open and resumes the next
time it is.

---

## DECISION-050 — The platform's geography is OpenStreetMap's, imported, not invented

**Date:** 2026-09-26

**Subject:** What a coordinate resolves to, and where those boundaries come from.

**Decision:** Syria's administrative boundaries and city quarters are imported from an
OpenStreetMap extract — the same Geofabrik file the routing engine already builds its graph from.
`scripts/osm-boundaries.sh` filters it with GDAL into a feature-per-line file;
`manage.py import_osm_boundaries` matches, places and writes it. `scripts/local-stack.sh` runs the
import when the machine has the file.

The mapping, decided in `locations/osm.py`:

* `admin_level=4` (محافظة) is matched to the fourteen provinces this platform already has, by a
  normalised name — no kind word, no diacritics, one spelling per letter — and neither creates nor
  renames one.
* `admin_level=5` (منطقة) becomes a **city**: 67 of them, and the name a Syrian actually says —
  الرقة, تل أبيض, الطبقة.
* `admin_level=6` (ناحية) is not imported. It would answer "ناحية مركز الرقة" where a reader
  expects "الرقة".
* `admin_level=10` and `place` in `neighbourhood`/`suburb`/`quarter` become a **neighbourhood**:
  192 of them, attached to the district that contains their own point. Damascus's quarters are
  drawn as boundaries and Aleppo's and Raqqa's as places; to a reader they are the same thing.

Every row's key is the OpenStreetMap id of the boundary it came from, so a second import corrects
the same rows. A quarter that falls in no district is skipped rather than attached to the nearest
one: the resolver answering with a city is true, and a quarter on the wrong side of a line is not.

**Reason:** The only geography finer than a province was five rectangles this repository invented
so that a position would resolve to something on a device. The corner of Home read
"الرقة — الدرعية" because a box said so. A reader who knows their own city can tell the difference,
and being confidently wrong about where somebody is standing is worse than saying only "الرقة".

**Boundaries:** Coverage is what contributors have drawn: quarters exist for Damascus (93), Aleppo
(43), Raqqa (27), Deir ez-Zor (16), Homs (10) and one each in three more provinces. Everywhere else
a position resolves to its district, which is the honest answer. The extract is not in the
repository — it is 80 MB of someone else's data — so a machine without it runs exactly as before.
Province map centres are left alone: where a map opens is a product decision, not a centroid.

The data is © OpenStreetMap contributors under the ODbL, which the app already attributes on the
map; anything derived from these boundaries carries the same licence.

---

---

# Technical Debt Register

Mandatory before staging or production closure. None of these blocks P2 or P10.

## DECISION-051 — The iPhone app is built from the Android code, with Kotlin Multiplatform

**Date:** 2026-10-03 · **Approved by:** the owner

**Subject:** How the iOS app is built. `docs/spec/12-IOS-SWIFTUI.md` names SwiftUI.

**Decision:** The iOS app is not written a second time in Swift. After the Android release, the
platform-free layers (models, network, database, session, domain and view models) move into
Kotlin Multiplatform modules, and the screens move to Compose Multiplatform; iOS then reuses most
of the Android code, with native pieces only where a platform demands them (maps, push, sign-in
storage). Until then new Android code keeps Android types out of the domain and data layers so
the move stays mechanical.

**Reason:** Two native code bases double every feature, fix and test for a team of one, and the
iOS app would trail Android for its whole life. The spec is immutable, so the change of course is
recorded here rather than by editing it.

## DECISION-052 — Registration codes keep the current channel

**Date:** 2026-10-03 · **Approved by:** the owner

**Decision:** The code is delivered as it is today: through the paired WhatsApp account
(`services/whatsapp-bot`) or the official Cloud API, one setting apart. The recommendation to
make the Cloud API the primary channel was declined. What changes is protection, not the channel:
a delivery failure is answered with `OTP_RECIPIENT_INVALID` (422) or `OTP_DELIVERY_UNAVAILABLE`
(503) instead of an internal error, and each number has a budget across all caller addresses
(`otp_phone_hour` 5/hour, `otp_phone_day` 10/day), so many addresses taking turns cannot keep the
sending account busy on one number.

## DECISION-053 — The site carries no state emblem

**Date:** 2026-10-03 · **Approved by:** the owner

**Decision:** The national crest is removed from the site's bar, and the asset from the
repository. The platform shows only its own brand.

**Reason:** A state emblem on a private service reads as government endorsement. That misleads
visitors and is the kind of affiliation Google Play's impersonation policy refuses.

## DECISION-054 — A directory of places, and nothing else

**Date:** 2026-10-03 · **Approved by:** the owner

**Decision:** The platform finds places and says whether they are open and on duty. A medicine
inquiry ("is this drug in stock") is out of scope, with the rest of `01-MASTER-SPECIFICATION.md`
§9.

## DECISION-055 — Facility data comes from its owners first

**Date:** 2026-10-03 · **Approved by:** the owner

**Decision:** Pharmacists and facility owners add their own facilities through onboarding. A bulk
import (for example the pharmacists' syndicate list) comes later, and when it does an imported
facility must be claimable by its owner rather than duplicated.

## DECISION-056 — Local object storage is SeaweedFS

**Date:** 2026-10-03

**Decision:** `infrastructure/docker/compose.yml` runs SeaweedFS (pinned) as the S3 service,
with an anonymous identity that may read `directory-public` and nothing else.

**Reason:** MinIO's community images are no longer pulled from Docker Hub (both tags this
repository pinned fail with "pull access denied"), so a fresh machine could not start the stack.
SeaweedFS keeps the isolation the tests assert: a public object answers 200, a private one 403,
and listing the public bucket 403.

## DECISION-057 — Search folds Arabic spelling on both sides

**Date:** 2026-10-03

**Decision:** Text search compares folded text: alef forms to bare alef, ta marbuta to ha, alef
maqsura to ya, hamza seats to their carriers, Persian ya and kaf to Arabic, vowel marks and
tatweel removed, case and spaces normalised. The term is folded in Python
(`search.arabic.normalize_arabic`) and the column in PostgreSQL by `directory_normalize_ar`
(migration `search/0001`), used through the `ar_contains` lookup. A test holds the two folds to
the same answers.

## DECISION-058 — A facility under review is frozen for its owner

**Date:** 2026-10-03

**Decision:** While a facility is `SUBMITTED`, its owner cannot change its core fields, location
or photographs (`FACILITY_LOCKED_DURING_REVIEW`, 409). The operator decides on the snapshot taken
at submission; edits after it would be published by the approval without anyone having seen them.
A rejection hands the facility straight back for editing.

The review decision is now also pushed to the owner's devices, not only written to the inbox.

## DECISION-059 — The Next.js lint plugin globs with tinyglobby, and the audit is clean

**Date:** 2026-10-03 · **Requested by:** the owner ("fix it, leave nothing unfixed")

**Problem:** `pnpm audit` failed on GHSA-vfj7-8cjw-p6xm, stack exhaustion in `braces` ≤ 3.0.3,
which has no patched release. Its only route into the workspace was
`eslint-config-next > @next/eslint-plugin-next > fast-glob 3.3.1 > micromatch > braces`, and the
newest plugin still pins that `fast-glob`.

**Decision:** the root `package.json` overrides that one edge,
`@next/eslint-plugin-next>fast-glob`, with `tinyglobby` (fdir + picomatch, no `braces`). The plugin
calls a single function, `globSync(pattern, { onlyDirectories: true })`, and only when
`settings.next.rootDir` is set; tinyglobby exports the same function with the same option.
Neither ESLint config sets `rootDir`, so in this repository the call is never made at all. One
difference is recorded in case that changes: given a literal directory, tinyglobby also lists its
subdirectories, where fast-glob returns the directory alone.

The vitest advisory (GHSA-82fw-gwwq-j7x9, moderate) is fixed by upgrading the test toolchain in
both apps: vitest 5.0.3, vite 8.3.2 (now a direct dev dependency, as vitest 5 requires),
@vitejs/plugin-react 6.1.1 and jsdom 30.1.1. `pnpm audit` reports no known vulnerabilities.

**Remove the override** when `@next/eslint-plugin-next` stops depending on `fast-glob` or a
patched `braces` is published.

## DECISION-060 — Identity v2: emerald, gold for duty, sand, two faces, a flat mark

**Date:** 2026-10-03 · **Approved by:** the owner, from the preview at
https://claude.ai/artifact/RLeGePFPSRRtDGws6tizje

**Decision:** one identity for the site, the console and the Android app, written once in
`packages/design-tokens`:

* **Colour.** The logo's emerald stays the colour of action. **Gold** (`semantic.accent.*`) is new
  and belongs to one state only, on duty now, plus the operator's place in the console's shell;
  the vocabulary gives `availability.DUTY` the new `accent` tone. Grounds are **sand** in light and
  deep emerald in dark instead of cool grey. Every pair the validator checks clears 4.5:1 in both
  themes, the gold ones included.
* **Type.** **Alexandria** for titles and large numbers, **IBM Plex Sans Arabic** for everything
  read. Each typography role names its face (`family: display | primary`), so no surface chooses.
  Tajawal is retired; `docs/spec/10-DESIGN-SYSTEM-UX.md` still names it and is left unchanged.
* **Mark.** The owner's artwork redrawn flat as vectors (`brand/mark.svg`, `mark-on-dark.svg`):
  the same letter, road and pin, without the bevel and glow that turned to a smudge at icon size.
  Every raster — launcher, round, store, splash, web symbols and icons — is cut from it by
  `apps/android/scripts/build-brand-assets.py`.

**Not in this decision:** the layouts. The site, the console and the app keep their current
screens in the new identity until phases 3, 4 and 5 redesign them.

## DECISION-061 — Uvicorn serves the API, and every route has a limit

**Date:** 2026-10-03 · **Phase 2.1 of the roadmap.**

**Decision:**

* **Server.** Uvicorn replaces Daphne in the image, both blueprints and the compose stack:
  several worker processes (`WEB_CONCURRENCY`, 2 by default) where Daphne ran one, with the
  Redis channel layer carrying events between them. It is started with `--no-access-log`, because
  its request line would carry a person's coordinates, and with `--no-proxy-headers`, so the
  client address and scheme are worked out in one place only (`DRF_NUM_PROXIES`,
  `SECURE_PROXY_SSL_HEADER`). Daphne stays installed for `manage.py runserver`.
* **Limits.** `DEFAULT_THROTTLE_CLASSES` gives every view without its own throttles a generous
  backstop: 600 a minute per anonymous address, 1,200 per account, the website server's keyed
  reads under its own `web_server` limit. Anonymous callers are told apart by the connection's
  address unless `DRF_NUM_PROXIES` says how many proxies to trust, in every throttle and not only
  the contact form's, so a forged `X-Forwarded-For` buys nothing. Production refuses to start
  without `DRF_NUM_PROXIES`, because unstated it would put every visitor behind one limit.
* **Strangers get 404.** An owner route asked about a facility by someone who does not manage it
  answers 404, not 403, so the answer does not confirm that the facility exists.
* **Probes answer first.** `/health/live/` and `/health/ready/` are answered before host and
  HTTPS checks. Docker's own probe asks `127.0.0.1` over plain HTTP, which production refused
  with 400, so a healthy container would have been reported dead.

## DECISION-062 — Operators list and correct facilities; console lists are paged

**Date:** 2026-10-03 · **Phase 2.2 of the roadmap.**

**Decision:**

* **Operators add facilities.** `POST /admin/facilities/` lists a facility with no owner, ACTIVE
  (published and counted as verified) or DRAFT. Until now a pharmacy could only enter the
  directory through its owner's application, so one whose owner had not registered could not
  be listed at all. It can be claimed by its owner later (phase 2.4).
* **Operators correct facilities.** `PATCH /admin/facilities/{id}/` takes the same fields and the
  same validation as an owner's edit, plus the location, province and category. Unlike an
  owner's change it leaves the status alone: a correction by the directory is a verification,
  not something to re-verify. Moving province clears the city, moving category clears the
  specialties and services, unless new ones are sent. Audited with both snapshots and the
  changed field names; the owners are notified. Both need the new `admin.facilities.edit`,
  granted to every role that already holds `admin.facilities.manage`.
* **Console lists are paged.** Reviews, facilities, users, reports and the audit trail return
  the contract's cursor envelope, 50 rows by default and up to 200, in the order the filters
  chose. They used to stop at 200 or 250 rows with no way to reach the rest.
* **Timeline titles are neutral.** Shift changes read «أُضيفت وردية مناوبة», not «أضافت
  الإدارة»: since DECISION-061 an owner's shifts are audited too, and the actor is named beside
  the title. Removed shifts and closures stay on the facility's timeline.

## DECISION-063 — A live facility is never taken down to review an owner's edit

**Date:** 2026-10-03 · **Phase 2.3 of the roadmap.** Supersedes the rule that an owner's edit to an
ACTIVE facility moves it to REVERIFICATION_REQUIRED.

**Why:** a facility waiting for re-verification is not public. A pharmacy that corrected its
address disappeared from the directory until an operator reached it, which punished exactly the
owners who keep their listing accurate.

**Decision:**

* **What waits, what does not.** On an ACTIVE facility, the name (Arabic and English), the address
  (both), the city, the neighbourhood and the map point are *reviewed*: they are what a fake or
  hijacked listing would change. Everything else (phone, WhatsApp, description, specialties,
  services, hours, photos) applies at once, as before.
* **How it waits.** The reviewed part becomes a `CHANGE` application with `proposed_changes`, sent
  as it is saved; the facility keeps its status and its published values. The owner's own view
  shows the proposed values with `pendingChange` naming them. A further edit is merged into the
  same application and bumps its `revision`; an edit back to the published values withdraws it.
  Submitting a live facility never takes it down: it answers with the waiting change, or 400.
* **Deciding.** Approval applies the proposal, refreshes `last_verified_at` and leaves the status
  alone; rejection leaves the facility exactly as approved. Either way the owners are told. The
  approval carries the `revision` the reviewer saw, and a proposal revised since is refused with
  409 `APPLICATION_CHANGED`, so nothing is published that nobody read. A proposal whose places no
  longer fit (a retired city) is refused with 409 `APPLICATION_NO_LONGER_VALID`.
* **What the reviewer sees.** For a waiting change, `snapshot` and `previous` are computed when
  the page is read — the facility as published, and the same with the proposal applied — so only
  the proposal shows as a difference, whatever changed live meanwhile. Documents are not asked
  for again. The task board counts these on their own.

## DECISION-064 — Claiming an ownerless facility, and inviting members by phone

**Date:** 2026-10-03 · **Phase 2.4 of the roadmap.**

**Claims («هذه منشأتي»).** Facilities the directory lists itself (DECISION-062), and those the
syndicate import will bring, have no owner. A signed-in person finds one among the published,
ownerless facilities where owner registration is open, and asks to own it with a `CLAIM`
application. The documents are the category's verification requirements, uploaded to the claim
itself: they belong to the claim (`VerificationEvidence.application`) and to nobody else until it
is approved, when they become the facility's. Approval makes the claimant the owner and refreshes
`last_verified_at`; it is refused with 409 `FACILITY_ALREADY_OWNED` if an owner appeared
meanwhile. Rejection and withdrawal delete the claimant's documents. The facility stays published
and unchanged throughout. One claim per facility is under review at a time (409 `CLAIM_PENDING`
for the next), and an account holds at most five open claims, so a squatter can delay a facility by
one review at most and cannot flood the queue.

**Invitations.** Members were added by account id, which no owner can know. An owner now invites a
Syrian mobile number as a manager or owner. The answer is the same whether or not the number has
an account, so the feature does not reveal who is registered; a person with an account is told at
once, and a person without one is told when they sign up with that number. Only the account with
that number can accept or decline (anyone else gets 404). An invitation lasts seven days, inviting
the same number renews it, an owner may revoke it, and accepting can raise a manager to owner but
never lowers anyone. Adding a member by account id stays for now, marked deprecated, until the
Android owner screens move to invitations in phase 5.

## DECISION-065 — The console needs a second sign-in step

**Date:** 2026-10-03 · **Phase 2.5 of the roadmap.**

**Why:** the console decides which facilities are public, reads verification documents and can
block accounts. A leaked password was enough to do all of it.

**Decision:**

* **Where it applies.** A password alone still opens a session, and the app, the owner screens and
  the account work on it as before. The console does not: every check of an admin permission
  (`HasAdminPermission`), and the console's realtime channel, also requires that the session passed
  the second step (`UserSession.mfa_verified_at`), for an operator who has an authenticator.
* **What it is.** TOTP as every authenticator app implements it (RFC 6238: SHA-1, 30 s, six digits,
  one step of drift), implemented in `accounts/mfa.py` and checked against the RFC's vectors. A
  code is accepted once; ten one-time recovery codes are issued at setup and only their digests
  kept; the secret is sealed with its own `MFA_ENCRYPTION_KEY`. Ten attempts an hour per account.
* **Policy.** `STAFF_MFA_REQUIRED` is on in production: an operator without an authenticator gets
  403 `MFA_ENROLLMENT_REQUIRED` from the console and is shown the setup screen, and cannot switch it
  off. Locally and in tests it is off, so nothing changes for development until someone enrols.
* **Recovery.** Whoever holds `admin.roles.manage` can clear a colleague's authenticator after
  verifying who they are; they set up a new one at their next sign-in. Enabling, disabling,
  verifying with a recovery code and resetting are all audited.

## DECISION-066 — Duty rosters arrive as files and rotations, previewed before they are written

**Date:** 2026-10-03 · **Phase 2.6 of the roadmap.**

**Why:** the night roster is published by the syndicate or the health directorate as a table, and
most provinces run it as a fixed rotation. Typing it shift by shift is slow and is where mistakes
come from.

**Decision:**

* **Import.** `POST /admin/duty/import/` reads a CSV (UTF-8) or XLSX (first sheet) of up to 2,000
  rows, with Arabic or English headers. A pharmacy is named by id, by name in the province (spelling
  folded as search folds it; an ambiguous name is an error that asks for the phone) or by phone; a
  shift by date with from/to in Damascus time (an end at or before the start is the next morning)
  or by ISO start and end.
* **Rotations.** A saved `DutyRotation` is an ordered list of pharmacies, fixed hours, how many are
  on duty a night and the day the first one is. It generates any period of up to three months.
* **Preview, then all or nothing.** Both answer with every row: what it would do (new, already
  stored) or why it cannot, including clashes with stored shifts and closures, each tried in its own
  savepoint and rolled back. Applying is refused while any row has a problem, writes every row in
  one transaction with source IMPORT, and changes nothing when repeated.
* **Who hears of it.** Each pharmacy that gains shifts has its owners told once, with the count.
  Imports, rotation changes and generated periods are audited.
* **Errors in the operator's words only.** A problem with the file's shape (empty, unknown headers,
  too many rows) or with a rotation's period is returned as a fixed message. Whatever a CSV or
  workbook parser raises gets a constant answer, so no exception's own text reaches a response.

## DECISION-067 — Readable facility links, advertisement numbers, notification preferences

**Date:** 2026-10-03 · **Phase 2.7 of the roadmap.**

**Why:** three small gaps that each block something visible. The website's facility links were
bare ids. The advertisements had no numbers, so a paying advertiser could not be shown what they
got. And the app's notification switches were a local filter: a muted kind still woke the phone.

**Decision:**

* **Slugs.** Every compact and detailed public facility carries `slug`, made from its Arabic name:
  NFKC, tashkeel and tatweel removed, anything that is not a letter or digit of any script becomes
  a single hyphen, at most 80 characters (`facilities/slugs.py`). It is computed, not stored. The
  id stays the address (`/f/{id}/{slug}`), so a renamed facility breaks no shared link; a site
  that receives an old or empty slug redirects to the current one.
* **Advertisement numbers.** `GET /admin/ads/stats/?from=&to=` (`admin.ads.read`) counts the apps'
  `ad_impression` and `ad_click` events per advertisement by Damascus day: last 30 days by default,
  at most a year. The click rate is clicks per impression, and null with no impressions. Android
  now sends both events. An impression is one per advertisement per province while the home page is
  in front of someone, counted when a slide settles; a pull to refresh does not count it again. A
  click carries the action type. The console's ads page shows views, clicks and rate beside each
  advertisement, for a period picked as on the analytics page.
* **Notification preferences on the server.** `GET`/`PATCH /account/notification-preferences/`
  holds three switches: `dutyReminders`, `provinceNews`, `applicationStatus`. All are on until
  changed. The categories are the app's own (`NotificationCategory.of`). They decide only whether
  the phone is woken: the message is still written to the inbox. A broadcast is not queued for
  accounts that turned province news off. A staff change to an owner's own shift
  (`duty.shift.admin_changed`) and any kind outside the three categories are always pushed: a
  switch hides news, never something addressed to the owner's own work.

## DECISION-068 — The end-to-end suites run on compose, in a stack of their own

**Date:** 2026-10-03 · **Phase 2.8 of the roadmap.**

**Why:** `scripts/e2e-admin.sh`, `scripts/e2e-android.sh` and `scripts/local-stack.sh` assumed
one Windows machine. They used hand-made containers (`p10pg`, `p10redis`, a pre-built
`directory-v3-p2dev:local`) and `netstat`/`taskkill`, and pulled MinIO images that are no longer
published. On a fresh machine, or any machine but that one, they could not start at all. So the
console's browser suite had not run since the two-step sign-in was added, and one of its
assertions had gone stale unnoticed.

**Decision:**

* **A separate stack.** `infrastructure/docker/compose.e2e.yml` layers the suites' own stack over
  the development compose file: project `daliini-e2e`, its own containers and volumes, wiped
  before every run. Only the API (`E2E_API_PORT`, 8021) and object storage (`E2E_S3_PORT`, 9021)
  are published, so the development stack keeps its ports and its data.
* **One library.** `scripts/lib/stack.sh` brings the stack up, runs `manage.py` in it, serves the
  console's production build, and frees a port with whatever the machine has: `lsof`, `fuser` or
  `taskkill`. The suites, `verify-all.sh` and `local-stack.sh` all use it. The storage-isolation
  check reads keys from the database rather than from a storage client.
* **One image.** The API, worker and beat share one image, `daliini-backend:local`, which
  `osm-boundaries.sh` also borrows for GDAL. `STACK_NO_BUILD=1` uses an image already built.
* **The phone session** (`local-stack.sh up`) is the development stack, seeded, with boundaries
  imported when present and one photograph uploaded through the owner API. The console runs with
  `pnpm dev:admin`.
* **`scripts/verify.sh`** runs the backend checks in the development stack's API container. It
  first syncs the dev group into the container's environment, and fails if it cannot. Before
  this, a missing mypy read as a pass, because the check only looked for error lines. Caches go
  to `/tmp`. The repository's `openapi/` is mounted at `/openapi`, so the contract tests run
  there instead of being deselected.
* Shell scripts are kept LF by `.gitattributes`: one had been committed with CRLF, which bash on
  Linux cannot run.

## DECISION-069 — The site's addresses read as words, and the reader picks light or dark

**Date:** 2026-10-03 · **Phase 3.1 of the roadmap.**

**Why:** categories were addressed by UUID (`/raqqa/8dbcc319-…`) and facilities by UUID alone,
which neither a person nor a search engine can read. The cards did not link to a facility's page
at all. The province picker always showed the first province on pages that name none. The tokens
had carried a dark theme since identity v2, but the site only followed the device setting.

**Decision:**

* **Categories** are `/<province>/<slug>` (`/raqqa/pharmacy`). The slug is the reference data's
  own, unique and immutable (`IMMUTABLE_CATEGORY_FIELDS`), and the public category now carries it.
  A UUID address still resolves and redirects permanently (308) to the slug, keeping its query.
* **Facilities** are `/f/<id>/<slug>`. The id stays the address, and the slug is the API's
  (DECISION-067). Any other words, or none (every link shared so far), redirect permanently to the
  current ones, so each facility has exactly one canonical page. The links are built in one place
  (`apps/web/lib/paths.ts`) for pages, the sitemap, JSON-LD and the share button alike.
* **Cards lead to the page:** a card's name links to its facility page; calling, WhatsApp and
  directions stay on the card.
* **The province** is read from the address (a province path, or `?p=` on the home page), then
  from the visitor's last choice (browser storage), then the first province. Choosing one on the
  home page keeps the home page; elsewhere it opens that province's page.
* **The theme:**
  * The choice is «تلقائي» (follow the device), «فاتح» or «داكن», set by a button in the bar.
  * It is stored in the browser and applied by a fixed inline script before the first paint, so
    there is no flash. The CSP already allows inline scripts, and this one interpolates nothing a
    visitor supplies.
* `robots.txt` no longer disallows `/_next/`. That rule kept crawlers from fetching the site's own
  scripts and styles.

## DECISION-070 — The home page answers questions, cards are rows, a facility page shows its photos

**Date:** 2026-10-03 · **Phases 3.2 and 3.3 of the roadmap.**

**Why:**
* On a phone, twelve facility cards were a scroll of several metres. Each card led with a
  photograph the width of the screen, and most of those photographs were the same empty frame.
* The home page opened on an advertisement and had no search box of its own.
* A facility page never showed the photographs its owner had uploaded, and it offered nothing to
  tap once the reader had scrolled past the top.

**Decision:**

* **The home page** answers, in order, what someone opens a health directory to ask:
  * **Search:** a search box, first, on the brand's band, with shortcuts to duty, to what is open
    now and to emergency numbers.
  * **On duty now** in the province: up to six pharmacies, and a link to the roster.
  * **Advertisements:** shown only when there are any.
  * **Categories:** tiles, each with its mark (from the category's `iconKey`) and linking to its
    own page. On a phone they are rows.
  * **One category's list:** with the open-now and on-duty filters.
  * **Two invitations:** the app, when there is a download link, and for owners.
* **A card is a compact row:**
  * **Top:** a 64-pixel thumbnail (the owner's photograph, or the category's mark), then the name
    as a link to the facility's page, then the status, category, area and rating on one line,
    then the street.
  * **Action row:** calling, which carries the number, then WhatsApp, the route, and a menu (⋯).
    The menu opens a dialog with the hours, sharing, the facility's page and reporting.
  * About 170 pixels tall instead of about 400. No horizontal overflow at 340, 360 and 390 pixels
    wide, nor on a desk.
* **The facility page:**
  * **Photographs:** every one, in the owner's order. On a phone they are a strip that snaps; on
    a desk, a mosaic.
  * **Opening hours:** today's row is marked, by the weekday in Damascus.
  * **The ways to reach it:** on a desk, a panel beside the page that stays in view; on a phone,
    a dock fixed along the bottom of the screen.
  * **One way of doing each thing:** the number is written as people write it locally, WhatsApp
    falls back to the phone number as it does on the cards, and corrections go through the contact
    form.
* The page claims nothing the product has not decided. Whether listing a facility is free is
  the owner's decision, so the page does not say it is.

## DEBT-001 — Ruff baseline

**Recorded:** 2026-09-17 · **Baseline:** 106 issues at `bc12f4d`, 104 after this batch. **99** after the Android binding batch (2026-09-19), and still 99 after the Android golden path batch. **Measured again 2026-09-26: 106**, after the OpenStreetMap batch cleared thirteen (its own eleven and three it found in a file it touched). The count had drifted upward between those two readings without anyone recording it, which is what this entry exists to prevent.

Concentrated in `directory/models.py`, which is written in a compressed style with semicolons and lines up to 249 characters against a 100 limit. Rule: **no new lint debt** — any file touched must not increase the count. Forbidden remedies: disabling rules broadly, blanket ignores, or weakening CI to make the result green.

**Closed 2026-09-29:** `uv run ruff check .` reports **0**. The fixes are formatting only (split
statements and long lines, import order, three unused imports, `raise … from`) plus a
`__str__` on the 23 models DJ008 named, built from codes, names or ids and never from a phone,
digest or token. No rule was disabled and no ignore was added.

## DEBT-002 — Mypy baseline

**Recorded:** 2026-09-17 · **Baseline:** 556 errors in 82 files. **Measured again 2026-09-26: 900 errors in 107 files.** It has grown by a third since it was recorded. New modules are still expected to be strict-clean — `locations/osm.py` and the boundary import are — but the rule has plainly not been held to across the whole backend, and the gap should be closed deliberately rather than by a later batch discovering it again.

**Correction, 2026-09-18:** the 556 figure was measured on the Windows host, where the
`django-stubs` plugin cannot import `directory_backend.settings.test` because GDAL is
absent, so it silently analyses less. Measured in the backend container, where the plugin
loads, the same tree at `c9a4cae` reports **797 errors in 100 files across 191 source
files**. That is the figure to compare against from now on; earlier reports are not
rewritten, and 556 is kept above so the record stays traceable. After the CONTRACT
ALIGNMENT batch the container figure is **789**. After the Android binding batch it is **727**: the touched
auth, realtime and contract-test modules were annotated in full. After the Android golden path batch it
is **645**: `core.openapi.protected()` gained a signature (78 at its call sites), and every touched
module was annotated.

`[tool.mypy] strict = true` is declared while the codebase is largely unannotated. Rule: **no new type debt**. Removing `strict` requires a recorded ADR; a blanket ignore is forbidden. New modules are expected to be strict-clean; every module added in the CONTRACT ALIGNMENT batch is.

**Closed 2026-09-29:** `uv run mypy .` reports **0 errors in 308 source files**, measured in the
backend environment with the django-stubs plugin loaded (824 before). `strict` and
`warn_unused_ignores` are unchanged. celery, channels and django-storages ship no type
information; one `[[tool.mypy.overrides]]` block lists exactly the modules imported from them.
Ten inline ignores remain, each with its error code, and eighteen older ones were removed. The
standing rule still holds: a new module is strict-clean.

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
