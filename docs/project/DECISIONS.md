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

## DECISION-071 — The site's map is the app's style, drawn by MapLibre, and optional

**Date:** 2026-10-03 · **Phase 3.4 of the roadmap.**

**Why:** a directory of places with no map makes the reader translate addresses in their head. The
app already draws the province with MapLibre and its own style, so the website should show the
same map, not a second, different one.

**Decision:**

* **When it appears.** The map shows only when a style is configured
  (`NEXT_PUBLIC_MAP_STYLE_URL`, the style the app uses). Unset, there is no map anywhere and
  nothing else changes. The style is hosted with the map in phase 6, and `scripts/local-map.sh`
  puts a local one in the media store.
* **What it shows.** On the home page, the pins of the category list being shown. On a facility
  page, its own pin. A pin's colour is the facility's state (gold for on duty, as in the identity),
  and each pin is a link to the facility's page. Every pin is also in the list or the page beside
  it, so the map is never the only way to a place.
* **Gestures.** Cooperative: one finger scrolls the page and two move the map, so a map halfway
  down a page never traps a thumb.
* **MapLibre GL JS 6.11** (≥ 6.4.1, which fixes advisory GHSA-jrc7-96c5-q579).
  * It is loaded only when a map is shown.
  * Its worker module and the Arabic text-shaping plugin (`@mapbox/mapbox-gl-rtl-text`) are copied
    from `node_modules` into `public/vendor/maplibre-<version>/` on every `dev` and `build`
    (`apps/web/scripts/vendor-map.mjs`). The site serves them itself: no third-party CDN at run
    time, and a new version gets a new path.
* **CSP:**
  * The style's origin, plus `NEXT_PUBLIC_MAP_ORIGINS` for the hosts of its tiles, glyphs and
    sprites, go into `connect-src` and `img-src`.
  * `worker-src 'self' blob:` allows the worker.

**Verified** with a local style:
* The map draws on the home page (10 pins) and on a facility page (1 pin).
* Every pin leads to its readable address.
* The only browser messages are the headless browser's software-GL notices.

Arabic labels on real tiles are to be checked in phase 6, once the style and tiles are hosted;
the plugin is in place for them.

## DECISION-072 — Roles are edited in the console; the owner role and a role manager always exist

**Date:** 2026-10-03 · **Phase 4.1 of the roadmap.**

**Why:**
* Roles could only be assigned. A role was created by a fixture or a database shell, so the team
  could not make "reviewer" or "duty desk" without a developer.
* A new deployment had no way to appoint its first operator.
* Nothing stopped the last person able to grant roles from being blocked, or from losing that
  permission. Either would leave the platform with nobody to appoint anyone.

**Decision:**

* **The role editor** (`/users/roles`, under المستخدمون والصلاحيات).
  * Whoever holds `admin.roles.manage` can create a role, rename it, choose its permissions and
    delete it.
  * Permissions are chosen by area (التشغيل، المراجعات والبلاغات، المنشآت والمناوبات…), with an
    Arabic label for each. A permission the console does not know yet still appears, under «أخرى».
  * A change reaches every holder on their next request.
  * Every create, edit and delete is in the audit log (`admin_role.*`).
  * New endpoints: `GET admin/permissions/`, `POST admin/roles/`,
    `PATCH`/`DELETE admin/roles/<id>/`. Each role now also reports `holderCount` and `locked`.
* **The owner role.**
  * `owner` («مدير المنصة») is created by `migrate` and given every permission after each
    `migrate`, so a permission a release adds reaches it without anybody editing it.
  * The console shows it but cannot edit or delete it (`ROLE_LOCKED`).
* **The first operator.** `manage.py grant_operator <phone>` gives one account the owner role.
  * The account normally registered in the app first.
  * `--create --name` makes it instead, with a password typed at a prompt, never on the command
    line.
  * The grant is audited (`admin_role.granted_from_shell`).
* **Somebody can always grant roles.** These are refused with `LAST_ROLE_MANAGER` (409) when
  they would leave no active account holding `admin.roles.manage`:
  * replacing an operator's roles;
  * editing a role's permissions;
  * blocking an account.
* **A role somebody holds is not deleted** (`ROLE_IN_USE`, 409), so a role is never taken from
  anyone as a side effect.

## DECISION-073 — The system page asks each service; background services record their last outcome

**Date:** 2026-10-03 · **Phase 4.2 of the roadmap.**

**Why:** the system page asked the database and nothing else. It called Redis, Celery and
storage "configured" whenever a setting existed. Nothing showed whether a verification code had
reached anyone, whether the WhatsApp bot was still paired, whether the scheduler ran, or when
the last backup was taken.

**Decision:**

* **Ask directly, with short timeouts** (`admin_console/system_health.py`). Network probes run
  in parallel and time out after two seconds; a probe that crashes reads as failed and never
  breaks the page. Each service is checked as follows:
  * **Database:** `SELECT 1`, plus any migration not yet applied.
  * **Redis:** `PING`.
  * **Workers:** a Celery ping through Redis, reporting how many answered.
  * **Storage:** `HeadBucket` on both buckets.
  * **WhatsApp bot:** its own `/health` endpoint, which tells "connected" from "logged out:
    pair the number again", from "running but disconnected", and from "unreachable".
* **Record what does not answer requests** (`health.ServiceSignal`, one row per service: last
  success, last failure, a reason code, failures since the last success):
  * **Scheduler:** `health.tasks.heartbeat`, scheduled by beat every five minutes and run by a
    worker, so one fresh row proves both.
  * **Codes:** a sent code is a success. A channel failure is a failure; a wrong number is not,
    because it is the person's problem and not the channel's.
  * **Push:** a failure is recorded on a transient error or a misconfiguration. Successes are
    read from the delivery table, so a broadcast does not write a hot row per device.
  * **Backup:** `scripts/db-backup.sh` writes its outcome with `psql`, best-effort.
  * A recording error is logged and swallowed. It never fails the work.
* **Four statuses:**
  * `ok`;
  * `warning`: working, but someone should look;
  * `failed`;
  * `off`: not used by this deployment. A development stack does not alarm about backups,
    error reporting or the code channel.
* **Thresholds:**
  * the scheduler is down after 15 minutes without a heartbeat;
  * a backup is due within 26 hours and late after 50;
  * the code channel is `warning` after one failure since the last code that got through, and
    `failed` after three.
* **The dashboard's warnings** now include the scheduler, the backup and failing codes. These are
  database reads only, with no network calls.
* **Nothing leaves the backend** that is a host, a URL, a credential or exception text. A
  failure is described by what it means, and the detail stays in the server log.
* The contract's `AdminSystemStatus` changes:
  * it is now `overall` plus a list of `checks`, each with a status, a sentence, latency,
    last success and failure, and metrics;
  * the old "configured" fields are removed (only the console read them);
  * enum names are pinned, so a later field named `key` cannot rename `KeyEnum` again.

## DECISION-074 — Console tables sort from their headers and remember the operator's view

**Date:** 2026-10-03 · **Phase 4.3 of the roadmap.**

**Why:**
* A table could not be sorted; the facility list hid its order in a filter box.
* Columns could not be chosen, a page was always 50 rows, and the header row scrolled away.
* Only some lists kept their filters in the address. Following a link to the same list with other
  filters showed the old ones.

**Decision:**

* **Sorting from the header** (`components/ui/data-table.tsx`):
  * A paged list asks the backend: `sortKey`, written to the address as `ordering`. Sorting one
    page in the browser would misstate the rest.
  * A list sent whole sorts in place: `sortValue`. Numbers sort as numbers, text as Arabic text,
    and empty values always last.
  * A press cycles ascending, descending, then the list's own order. `aria-sort` says which.
  * The facility list sorts by quality (weakest first) and by last update. The user list gained
    `ordering` on the backend: name or creation date, each ending in the key so pages never
    repeat a row.
  * Provinces, groups, pages, roles, events and team performance sort in place.
* **The operator's view, per table, in this browser:**
  * which columns show (the name and the actions cannot be hidden; the facility owner starts
    hidden);
  * a compact density;
  * the page size (25, 50 or 100; the backend allows up to 200).
  * Without storage it all still works until the page is left.
* **The header row stays in view.** The wrapper scrolls within a height bound.
* **Counts:** a cursor list has no total, so the line above it says what the page holds and
  whether more follow.
* **The address is the only copy of the filters.** The facilities, users, audit, reviews and
  reports lists read their filters from the URL on every render. A link to the same list with
  other filters shows those filters, in the boxes too.
* **Exports follow the filters** on the facility and report lists, as they already did on the
  audit log.

## DECISION-075 — The console draws its own charts and shows facilities on the platform's map

**Date:** 2026-10-03 · **Phase 4.4 of the roadmap.**

**Why:** analytics gave totals and arrows; a total cannot show a bad Tuesday or a slow start.
The console had no map at all, so "where are our pharmacies, and which are suspended" had no
answer, and a facility without a location was invisible.

**Decision:**

* **Daily series:** `GET admin/analytics/series/` returns the analytics period one Damascus day
  at a time.
  * It covers the four event counts plus new accounts, approvals and reports.
  * Every day is present: a quiet day is zeros, not a gap.
* **Charts drawn by hand in SVG** (`components/charts.tsx`, no library):
  * a line over days and a set of bars, in the page's tokens, so both themes follow;
  * time runs right to left like the page;
  * the width is measured, so text keeps its size on a phone;
  * a hover card shows each day;
  * the same numbers are always present as a table for screen readers, and the legend carries
    each series' total.
* **Where the charts appear:**
  * the analytics page draws usage (searches, views, directions, empty searches) and activity
    (new accounts, approvals, reports) for the chosen period;
  * the dashboard draws the last two weeks for whoever may read analytics;
  * the dashboard's facility statuses are bars, each a link to that list.
* **The facility map** (`/facilities/map`, a tab beside the list):
  * `GET admin/facilities/map/` takes the list's own filters and returns points (name, state,
    coordinates), at most 5,000, and says when there were more.
  * It counts the selected facilities that have no location; the page links to them so they can
    be fixed.
  * Dots are coloured by status. Pressing one opens a card built from text nodes, never markup,
    because the name comes from the owner, with a link to the record.
* **The same map as the site and the app:**
  * The style is `ADMIN_MAP_STYLE_URL`, read on the server per request, so it is set without a
    rebuild. Without it the page says so and still gives the counts.
  * MapLibre loads only on that page. Its worker and Arabic shaping are vendored into
    `public/vendor` by `scripts/vendor-map.mjs`, as on the site.
  * The CSP gains the style's origin and `ADMIN_MAP_ORIGINS` only when a map is configured,
    plus `worker-src 'self' blob:`.
* **Local development:** `scripts/dev-app.mjs` now runs each app's own `dev` script, so the
  vendoring step runs before `next dev` on both apps. Before, it ran only on a build.

## DECISION-076 — The Android app invites by phone, claims listed facilities and shows edits under review

**Date:** 2026-10-03 · **Phase 5.1 of the roadmap.**

**Why:** phase 2 gave the backend three owner features the Android app could not reach. An owner
added a manager by typing a raw account id (INT-084). An owner whose facility was already listed
had no way to say «هذه منشأتي». An edit to a live facility's name or address went to review, but
the app showed the proposed values as if they were published.

**Decision:**

* **Edits under review:** the management screen's first card says which fields wait for review
  (name, address, city, neighbourhood, map location), from `pendingChange.proposedFields`, and
  that people still see the published values meanwhile. A field the app does not know is left out
  of the sentence rather than shown as a code.
* **Invitations, the owner's side:** the members card invites a Syrian mobile number as a manager.
  * The backend validates and normalises the number; a refusal shows as the card's error.
  * Sent invitations are listed, still-waiting first, with their date and state. A waiting one can
    be revoked.
  * Only the owner gets invitations back from the backend, so a failed read means "not the owner":
    the invite form and the remove-manager buttons are hidden, and the page still loads.
  * The manager-by-account-id form is removed from the app. The endpoint stays in the contract.
* **Invitations, the invitee's side:** «دعوات الإدارة» lists the invitations waiting for this
  account (`account/invitations/`).
  * Accepting opens the joined facility's management page in place of the list.
  * Declining removes the card.
  * The account page links to it whether or not the person owns a facility, because an
    invitation is addressed to the number.
  * The push `facility.invitation.received` opens it, signing in first if needed. It belongs to
    no muting category: it is addressed to this person, not news.
* **Claims («هذه منشأتي»):**
  * **Search:** the owner types a name, and the app asks `owner/claimable-facilities/` after a
    350 ms pause, once there are two letters. Picking a result starts a claim, or reopens the open
    one the account already has.
  * **The claim:** it shows the documents the category requires, uploaded from the photo picker
    and removable while it is a draft. Sending is enabled once the required ones are there, the
    same rule the backend applies.
  * **Withdrawing and refusal:** an open claim can be withdrawn after a confirmation. A refused
    claim shows the reason and offers a new claim on the same facility.
  * **Where claims appear:** in «منشآتي» under their own heading, approved ones excepted, since
    they are facilities by then. The page's empty state offers both ways in: add a facility, or
    claim a listed one. The account page offers the claim to someone who owns nothing yet.
* **Testing:** the view models that need a route's `id` read it from the saved state by name,
  which is where type-safe navigation keeps it. `toRoute` needs Android's `Bundle` and cannot run
  in a plain JVM test.

## DECISION-077 — The account keeps the notice switches; a newer build is offered once

**Date:** 2026-10-03 · **Phase 5.2 of the roadmap.**

**Why:** the notice switches in Settings lived only on the phone. The backend could not honour
them when it sent (it has had `account/notification-preferences/` since phase 2.7), a second
phone did not share them, and the app's comments still said no endpoint existed. The backend
also reported a newer build (`latestVersionCode`), but the app only acted on the minimum, so
nobody learned of an update until they were locked out.

**Decision:**

* **Signed in, the switches are the account's.** `NotificationPreferencesSync` keeps the device
  in step with the account:
  * It reads the account's choices when Settings opens, and when a session starts if push is
    configured. They replace the device's copy.
  * A change shows at once, then is sent: only the switches that moved. The backend's answer
    becomes the device's copy.
  * A refused change puts the switch back and says so under the switches. Changes go one at a
    time, so two quick taps cannot overwrite each other.
* **Where the device copy still matters:** a push is still checked against it when it arrives,
  to catch one sent just before a change. Signed out, the device copy is all there is.
* **Settings says where the choices live:** «في حسابك فتسري على كل أجهزتك» when signed in,
  «على هذا الجهاز فقط» when not.
* **A newer build is offered, never imposed.** `VersionCheck` reports `Newer` when this build is
  above the minimum and below `latestVersionCode`, but only if a store address is configured,
  since an offer nobody can act on is noise.
  * The gate shows a dialog with «تحديث» and «لاحقاً», and the backend's notice when it sent one.
  * Either answer is stored as the offered build (`updateOfferedVersionCode`). That build is not
    offered again; a later one is.
  * Nothing shows until the stored answer has been read, so a build already set aside never
    flashes up.

## DECISION-078 — A trip keeps running with the screen locked

**Date:** 2026-10-03 · **Phase 5.3 of the roadmap.**

**Why:** guidance read the location every second from its screen. Once the phone was locked or
put in a pocket, Android stopped those readings within minutes and could end the process, so
the voice fell silent on the road, where it is needed most.

**Decision:**

* **A foreground service of the `location` type runs while a trip is under way, and only then.**
  * The navigation view model starts it the first time a trip is under way (navigating or
    rerouting), and stops it on arrival, on failure, and when the screen is left.
  * It is started from the trip's screen, which is the only time Android allows a location
    service to start. If Android refuses (no permission, or started from the background), the
    trip goes on as before, on screen only.
* **The ongoing notice:**
  * It says how much is left, rounded to fifty metres, and about how long in minutes (Arabic
    plurals), or «يُعاد حساب الطريق…» while rerouting.
  * It is silent (the voice is the sound) and is redrawn only when the rounded text changes.
  * Tapping it brings the app's own task back, as the launcher would.
* **Who owns what:**
  * The feature defines the port, `NavigationKeepAlive`, and the notice's rounding
    (`guidanceNotice`, tested on the JVM).
  * The app holds the service (`NavigationForegroundService`), because the notice uses the app's
    icon and colour.
* **The screen stays lit while a trip is under way**, as navigation apps do. Locked anyway, the
  trip continues in the background.
* **Permissions:**
  * The manifest gains `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION`.
  * There is still no background location permission. The location permission granted for use
    while the app is open is what the service runs on.
  * Play Console needs the foreground-service declaration ("Navigation"); see
    `apps/android/play/app-content-checklist.md`.

## DECISION-079 — Builds number themselves, the app's code is compiled ahead, motion follows the system

**Date:** 2026-10-03 · **Phase 5.4 of the roadmap.**

**Why:** every build was version 1, "0.1.0", so two APKs could not be told apart and Play
would refuse the second upload. The app's own code ran interpreted on its first launches. The map
glided even for someone who had turned animations off. The example environment files named the
realtime URL with a setting the build does not read and paths the backend does not serve.

**Decision:**

* **Version numbers come from the build.**
  * `versionCode` is `DIRECTORY_VERSION_CODE`. The Android workflow sets it to its run number,
    so no two CI builds share one.
  * `validatePlayRelease` refuses a Play build without a number above 1.
  * `versionName` is `DIRECTORY_VERSION_NAME`, kept in `gradle.properties` and bumped by hand
    for a release, in review like any change.
  * A machine that sets neither builds 1 and the checked-in name.
* **Baseline profile.**
  * `app/src/main/baseline-prof.txt` covers every method of the app's own packages, so it is
    compiled when the app is installed rather than after the first launches.
  * `profileinstaller`, which Compose already brought, is named in the catalog so it cannot
    drop out. It applies this profile and the libraries' own on devices that do not get Play's
    cloud profiles.
  * The profile is hand-written and broad. A generated one (Macrobenchmark on a device) would
    narrow it to what startup and scrolling touch; that waits for the device check at the end
    of phase 5.
* **Motion.**
  * Screen changes stay without animation, as decided with device evidence (the "screenshot"
    cross-fade; see the comment on the `NavHost`).
  * Compose's own animations already follow the system's animation scale. MapLibre's camera
    did not: with animations off, the camera now jumps instead of gliding, both for its moves
    and for framing a route (`systemAnimationsOff`).
* **Realtime URL setting.** `.env.example` and `play/release.env.example` now name
  `DIRECTORY_REALTIME_WS_URL` with the backend's path `ws/v1/directory/`, as the build and the
  backend do.
* **Dependency locking is not adopted.** Every dependency is pinned to one version in the
  catalog and nothing floats, so the same commit already resolves the same libraries. Instead,
  the Play qualifier fails on any floating version (`+`, `latest.*`, a range) in the catalog or
  a build script.
* **Accessibility.**
  * An audit of undescribed icons found each to be decorative beside the text that says the
    same thing; the meaningful ones (verified, unread) already speak.
  * The menu rows of the account and settings pages now announce themselves as buttons.

## DECISION-080 — Production is one server running the compose stack behind Caddy

**Date:** 2026-10-03 · **Phase 6.1 of the roadmap.**

**Why:** the owner decided against Render (EXT-005); production is a VPS (EXT-007). The
repository still described Render: two blueprints, a test comparing the settings with them, and
runbooks built around its dashboard. The admin blueprint also lacked `ADMIN_PUBLIC_ORIGIN`, so
the console would not have started.

**Decision:**

* **`infrastructure/production/compose.yml` is the platform on one server.**
  * Services: Caddy, PostgreSQL 17 with PostGIS, Redis, a one-shot `migrate`, the API (Uvicorn),
    the worker, beat, the site, the console and the WhatsApp bot. A `backup` tool is run by a
    systemd timer at 02:17 UTC.
  * Only Caddy publishes ports. Everything restarts by itself and rotates its log (20 MB × 5).
  * `migrate` runs `check --deploy` and the migrations, and the API starts only after it
    succeeded. Redis refuses writes rather than evicting work.
  * The settings module follows `ENVIRONMENT` (production or staging). Staging is the same file
    under another project name, env file and domain.
* **Not on the server, on purpose.** Photographs and verification documents are in an S3
  service elsewhere. Backups go to a second bucket with its own key. A lost disk loses neither.
* **Caddy is the front door (`Caddyfile`).**
  * Names: `<ROOT>` (the site), `www.<ROOT>` redirected to `<ROOT>`, `api.<ROOT>` (API, realtime
    socket, health) and `admin.<ROOT>`.
  * Certificates are its own (Let's Encrypt, renewed by itself). Cloudflare is set to
    Full (strict), with Always Use HTTPS off so renewals can answer over HTTP.
  * Headers: HSTS is sent and the `Server` header removed. No access log is written, because
    a request line can carry a position.
  * **The visitor's address.** Caddy takes `CF-Connecting-IP` only from Cloudflare's published
    ranges and hands the API exactly one address, so `DRF_NUM_PROXIES=1` names the visitor and
    no one can choose their own.
* **The site and the console call the API through Caddy.** `api.<ROOT>` is a network alias of
  Caddy, so a server-side call keeps its Host, arrives as HTTPS under the real certificate and
  never leaves the server. `http://api:8000` would be refused by `ALLOWED_HOSTS` and redirected
  by `SECURE_SSL_REDIRECT`.
* **Every setting** is in `production.env.example`, with placeholders only. The filled copy lives
  on the server, mode 600.
* **What holds it.**
  * `test_production_stack.py` (18 tests) replaces the blueprint test. It checks that:
    * every required production setting reaches the backend;
    * the four backend services read the same settings, and no secret is written into the
      stack;
    * every insisted value is offered in the example;
    * only Caddy publishes ports, and the visitor's address and the internal routing are as
      described above;
    * no access log is written.
  * `qualify-production-stack.py` checks the stack's shape in the governance job.
  * A new CI job, `production-stack`, builds every image, boots the whole stack with throwaway
    values under `daliini.localhost`, and asks each name through Caddy, TLS included. Raqqa's
    page must show its name, which proves the site read the API through Caddy.
* **Retired:** `render.yaml`, `render.production.yaml`, the staging qualifier that read them,
  and the Render steps in the runbooks. `staging-deploy.md` became `deploy.md`. The DNS plan
  now names the bare domain as the site's address, as the site and App Links already do.

## DECISION-081 — Releases are images, deploys are started by a person, backups are hourly and drilled

**Date:** 2026-10-03 · **Phase 6.2 of the roadmap.** Builds on DECISION-080.

**Why:** DECISION-080 put the stack on one server, but a deploy still meant building on that
server by hand. Backups were nightly, which promised to lose up to a day, and nothing proved
they restore. A full disk, which stops the database, would have shown nowhere.

**Decision:**

* **A release is a set of images.**
  * `.github/workflows/release-images.yml`, started by hand for `staging` or `production`,
    publishes five images to GHCR: backend, web, admin, whatsapp-bot and backup. Each is tagged
    `<environment>-<commit>`.
  * The site's public addresses are fixed when its image is built. They come from the GitHub
    environment's variables, and a test holds them to the same build arguments the stack
    uses.
  * The compose file runs `${IMAGE_REGISTRY}daliini-<name>:${RELEASE}`. With `IMAGE_REGISTRY`
    empty, the server builds the images itself as before.
* **A deploy is one command on the server.** `infrastructure/production/deploy.sh <env-file>
  <commit> [release]` does the whole deploy:
  * it checks out the commit, writes `RELEASE`, and adds a history line to the env file;
  * it pulls only the project's own images, so Docker Hub's limits never stall a deploy;
  * it brings the stack up and waits up to five minutes until
    `https://api.<ROOT>/health/ready/` answers through Caddy, its certificate included;
  * it then removes that environment's images older than the release it replaced.

  Going back is the same command with the previous commit.
* **`.github/workflows/deploy.yml` runs that command over SSH.**
  * It is started by hand. The GitHub environment can require a reviewer.
  * The server's host key must match `DEPLOY_KNOWN_HOSTS`, and the commit must be a full SHA.
  * Nothing deploys on merge. Until a server exists (EXT-007), neither workflow has anything
    to run against.
* **Backups run hourly.** The timer fires at minute 17 of every hour, so the target is an
  RPO of 60 minutes.
  * The system page warns after two hours without a backup and fails after 26.
* **A restore drill runs every month.** It runs on the first Sunday at 03:40 UTC
  (`restore-drill.sh`, `daliini-restore-drill.timer`):
  * it restores the newest backup into a throwaway PostGIS container, on its own network;
  * it checks the schema against the release's migrations, counts the data, sees that a sample
    of photographs is still in the media bucket, and finds Raqqa;
  * it writes evidence that `restore_evidence.py` holds to RPO ≤ 60 minutes and RTO ≤ 4 hours.
  * It never touches the running stack.
* **The disk is on the system page.**
  * A new check, `disk`, reads how full the filesystem Docker keeps everything on is, from
    inside the API container.
  * It warns at 80% and fails at 90%. The numbers are shown, and a development machine's disk
    is reported but not judged.
* **Outside watching stays outside.** An uptime service outside the server
  (`docs/runbooks/monitoring.md`) watches `/health/live/`, `/health/ready/` and the site. A
  check that runs on the server cannot report that the server is down.

## DECISION-082 — The map and its routing are served from the platform's own host

**Date:** 2026-10-03 · **Phase 6.3 of the roadmap.** Builds on DECISION-071 and DECISION-081.

**Why:** the style named another project's hosts for its glyphs and icons, its tiles were served
only on a developer's machine, and routing ran only there too. The style also carried an
expression that MapLibre on the web refuses outright. `poi-dot`'s radius chose between two zoom
interpolations inside a `case`. Measured in a browser on 2026-10-03, the site's map did not draw
at all, not merely that one layer.

**Decision:**

* **One more name, `maps.<ROOT>`, behind the same Caddy.**
  * Martin 1.16.1 serves:
    * the PMTiles archive as XYZ tiles (`/syria/{z}/{x}/{y}`);
    * glyphs generated from the brand's IBM Plex Sans Arabic (`/font/...`), which covers Arabic
      Presentation Forms-B in full and most of A;
    * the sprite drawn from `maps/sprite/daliini` (four icons in the brand's colours);
    * the bound style (`/style/daliini`).
  * Valhalla 3.9.0 answers routes under `/routing/`. Caddy forwards only `POST /route` and
    `GET /status` to it. Valhalla logs each request line, so a GET would put both positions
    in its log; the other actions are heavy and unused.
  * Tiles, glyphs and icons are cached for a day and the style for an hour. Routes are never
    cached.
* **Built on the server from OpenStreetMap.** `infrastructure/production/map/build-map.sh` runs
  monthly from a systemd timer (second Sunday) and does the following:
  * it downloads Geofabrik's Syria extract and checks its MD5;
  * Planetiler builds OpenMapTiles-schema tiles with Arabic and English names;
  * Valhalla builds its graph from the same file;
  * `bind-style.py` writes the style, with the tiles, glyphs and icons on `maps.<ROOT>`, the
    fonts swapped for Plex, and the zoom range and bounds read from the archive;
  * it switches `$MAP_DIR/current` to the new build and asks both services through Caddy,
    switching back if they do not answer;
  * two builds are kept.
* **The reference style stays the one source.** `maps/raqqa.style.json` keeps its layer ids,
  because the app inserts its route and markers at its anchors. The `poi-dot` radius now
  interpolates by zoom at the top, with the class chosen at each stop; the values are
  unchanged.
* **What holds it.**
  * `fixture.py` writes a one-tile PMTiles archive of central Raqqa in the same schema, with
    Arabic names on a city, a town, a street and a pharmacy.
  * The stack smoke asks every map path through Caddy. It also checks that a GET route is
    refused.
  * A CI job, `map-labels`, draws the fixture with the site's own MapLibre and RTL plugin in
    Chromium. It fails unless:
    * the plugin loaded from the site's own path (a check of the site's setup: MapLibre 6
      joins Arabic letters by itself, measured with the plugin missing);
    * glyphs in the presentation-form ranges were requested, which happens only once letters
      have been joined;
    * every expected name was placed;
    * the map reported no error.
  * The **Map build** workflow builds the real map of Syria. It runs on changes to the map, monthly
    and by hand. It draws Raqqa, Damascus and Aleppo the same way, and asks for a route in Raqqa
    and in Damascus for car, motorcycle and walking.
  * The system page gains a `map` check: the tiles, the routing graph, and the graph's age (a
    warning after 45 days).
* **The app** reads `https://maps.<ROOT>/style/daliini` and routes through
  `https://maps.<ROOT>/routing/`. A test holds the adapter to `POST /routing/route`. No screen
  calls a geocoder: place names come from the API. The unused geocoding setting stays as it is.

## DECISION-083 — The public API is load-tested at launch scale, and what it found is fixed

**Date:** 2026-10-03 · **Phase 6.4 of the roadmap.**

**Why:** every list had been measured over the thirty facilities a development database holds.
A query that is fast over thirty can be slow over five thousand, and a server that answers one
visitor can fail forty.

**Decision:**

* **Load data, never real.** `manage.py seed_load_directory --facilities N` writes made-up
  facilities across the fourteen provinces, with opening hours and a duty roster.
  * It refuses any database that holds a facility it did not make.
  * In production it also needs `--disposable`.
* **Visitors, per screen.** `infrastructure/load/public.js` (k6) walks Home, a list and its
  next page, a facility, the map, a search and the duty roster, with pauses between them. Each
  screen has its own p95 limit, so a slow map cannot hide behind fast lists:
  * Home 800 ms, list 600, facility 500, map 1000, search 800, duty 600;
  * across all screens, p99 under 2 s and fewer than 1% failed requests.
* **In CI** the `production-stack` job runs it after the smoke: 5,000 facilities and 40
  visitors for a minute, through Caddy and TLS. The per-address limits are raised for that
  throwaway stack only, since every visitor comes from one address.
* **What it found, and the fixes:**
  * **The map asked the availability engine per marker:** two queries each, a thousand for a
    full viewport, 1.1 s. Search did the same, four queries per row.
    * Both now carry the open and duty flags in their one query, as the list already did.
    * The map builds its markers from five columns and the flags (`state_from_flags`) instead
      of whole facilities with their joins, hours and ratings: 172 ms became 32 ms, in one
      query.
    * Tests hold all three endpoints to a query count that does not grow with the page.
  * **Connections ran out.** Under ASGI each request runs in its own thread, and
    `CONN_MAX_AGE=60` kept each thread's connection open after its request. At 120 visitors
    PostgreSQL refused new clients and half the requests failed.
    * Django's own pool (psycopg_pool) now holds the connections per process, with
      `CONN_MAX_AGE` 0.
    * `DB_POOL_MAX_SIZE` (8) bounds each process, so the two API workers hold at most 16 of
      PostgreSQL's 100.
    * Celery 5.6 closes the pool in each forked child.
    * Measured again at 120 visitors (34 requests a second): nothing failed and PostgreSQL held
      16 connections. The two workers' CPU is now the limit, and `WEB_CONCURRENCY` raises it on
      a larger server.
* **Not changed.** Server-side parameter binding was measured and did not help. Steady-state
  query planning was a few milliseconds; the 40 ms plans seen at first were a cold
  connection's catalogue.

## DECISION-084 — Launch content: what the code can supply is drafted, the owner approves it

**Date:** 2026-10-03 · **Phase 7.1 of the roadmap.**

**Why:** the pages people are shown at launch were a first draft from before guidance, routing,
claims, invitations and the platform's own map existed. The site's privacy fallback carried an
engineering note. The store listing was placeholders, and the feature graphic did not exist.
Approving them is the owner's; drafting them so that every sentence matches the code is not.

**Decision:**

* **Legal and help pages, second draft.** Migration `content_services/0007` publishes version 2
  of privacy, terms, instructions and FAQ, re-read from the code. The privacy text now covers:
  * a route's two ends going to the platform's own routing engine, unlogged;
  * a trip reading the location with the screen locked, under a visible notice, until arrival;
  * sign-in codes by WhatsApp;
  * invitations by phone number;
  * notification settings;
  * usage statistics without coordinates, with a one-way device identifier, deleted after
    180 days;
  * crash reports carrying only the crash;
  * no data shared with advertisers.

  The terms add route guidance and road law, false claims, the platform's own ads, and the
  OpenStreetMap attribution (ODbL). It replaces a page only while the active version is the
  first draft exactly as seeded; a version an operator published stays, and the migration
  reverses cleanly. It is still a draft for legal review.
* **The site's fallback texts**, shown only while the API cannot answer, now say the same
  things to a visitor, without the internal note.
* **The Play listing in Arabic** (`apps/android/play/store-listing/ar.json`): name, short and
  full description, each claim checked against the code. It is marked
  `DRAFT_FOR_OWNER_APPROVAL`. Every URL is now the bare domain, the site's real address; www
  only redirects.
* **The feature graphic** (`docs/design/brand/play-feature-graphic.png`, 1024 × 500) is
  rendered from `mark-on-dark.svg` and the colour tokens by a script. It carries no name or
  words, by the brand's rule. The 512 store icon already existed.
* **Data safety** names the map and routing as the platform's own servers, which receive the
  route's two ends unlogged, not a third-party provider.
* **`docs/runbooks/launch.md`** covers four stages, each step naming who does it: before
  anyone outside sees the platform, Play's internal and closed tracks, the public release, and
  the first week.

**Left to the owner:** approving the texts and the graphic, the support and privacy addresses,
the domain, the Play account (EXT-001, EXT-003), and screenshots from the release build on a
real phone. Screenshots are never mocked.

## DECISION-085 — The first shared layers: a multiplatform convention, observability and analytics

**Date:** 2026-10-03 · **Phase 8.1 of the roadmap; carries out DECISION-051.**

**Why:** DECISION-051 moves the Android app's platform-free layers into Kotlin Multiplatform so
the iPhone app reuses them. The move needs a build convention first, and proof on each step that
the Android app is unchanged and that the shared code really compiles and runs on iOS. It starts
now, while the Android release waits on the owner's accounts (EXT-001, EXT-003), because none of
it changes what the Android app does.

**Decision:**

* **`serva.kmp.library`** in `build-logic`: Kotlin Multiplatform with AGP's multiplatform
  Android library plugin (`com.android.kotlin.multiplatform.library`), the form AGP 9 supports
  for a multiplatform module. The Android side keeps the Android libraries' levels (compileSdk
  37, minSdk 24, JVM 17). The iOS targets are `iosArm64` (phones) and `iosSimulatorArm64` (the
  simulator on Apple-silicon Macs); no Intel simulator. Android lint comes from `com.android.lint`
  (`lintAndroidMain`) and the JVM tests run as `testAndroidHostTest`.
* **The first two modules are shared:** `core:observability` and `core:analytics`. They were
  already platform-free, and every feature depends on them. Their sources moved to
  `src/commonMain`, and the analytics tests to `src/commonTest`, on `kotlin.test` rather than
  JUnit. Kotlin/Native refuses a comma in a function name, so a shared test's backticked name
  has none; two were reworded.
  * `QueuedAnalyticsTracker` loses `@Inject` and `@Singleton`, which do not exist on iOS. The
    Android app already built it by hand in `AnalyticsModule`, so nothing about how it is made
    changes.
  * The module now needs `kotlinx-coroutines-core`, not the Android artifact; the app still
    brings the Android dispatcher.
* **Where it is proven:**
  * The same analytics tests run on the JVM, as the module's host tests and in the JVM harness,
    and on the iPhone simulator.
  * The Android workflow adds `testAndroidHostTest` to its unit gate and `lintAndroidMain` to
    its lint gate.
  * A new `ios-shared` job on a macOS runner runs `iosSimulatorArm64Test` and compiles for
    devices.
  * The iOS klibs also compile on Linux, so daily work needs no Mac; only linking and running
    iOS code do.
* **The JVM harness** compiles the shared modules' `commonMain` and runs their `commonTest`. It
  no longer lists a `directory` feature that has not existed for some time.

**Next, in order, each step keeping the Android app green and unchanged:**

1. `core:model`, whose dates move from `java.time` to `kotlinx-datetime`.
2. The network layer and the repositories.
3. The screens, on Compose Multiplatform.
4. The iPhone shell: MapLibre iOS, CoreLocation, the Keychain for the session, and APNs.
5. The App Store. It needs the owner's Apple developer account.

## DECISION-086 — The models are shared: dates on kotlinx-datetime, links read in common code

**Date:** 2026-10-03 · **Phase 8.2 of the roadmap; the second step of DECISION-085.**

**Why:** `core:model` is what every other layer speaks. Three things kept it on the JVM: Damascus
clock conversions on `java.time`, the duty presets built on them, and deep links read with
`java.net.URI`.

**Decision:**

* **`core:model` is a multiplatform module** (`serva.kmp.library`). Its sources moved to
  `src/commonMain` and its tests to `src/commonTest`, on `kotlin.test`. Test names lost their
  commas, which Kotlin/Native refuses.
* **Dates are `kotlinx-datetime` 0.8.0**, exposed as `api` because the module's own functions
  take and return them:
  * `DamascusTime` and `DutyPresets` keep their behaviour exactly. The zone rules still decide,
    not a fixed offset.
    * A time skipped in spring still moves forward by the gap.
    * A time repeated in autumn still means its first occurrence.
    * The existing tests for 2021's summer time pass unchanged.
  * `kotlinx-datetime` uses `java.time` on Android, with the app's core-library desugaring as
    before. On iOS it reads the system's zone database.
  * Instants are `kotlin.time.Instant`.
* **The Android code that hands dates to the model** now passes `kotlinx.datetime` values: the
  date and time field, the duty roster and its view model. The Android-only screens that format
  a weekday keep `java.time`, which is theirs to use.
* **Deep links** are read by a small `HttpsLink` in common code instead of `java.net.URI`. It
  accepts and refuses what `URI` did for every link the app cares about:
  * only absolute `https` with a host;
  * escapes decoded as UTF-8, a broken `%` refused;
  * a space, a quote or a control refused;
  * a port, a query or a fragment ignored.

  New tests pin those cases.
* **The JVM harness** compiles the shared model and runs its tests.

## DECISION-087 — The core layers are multiplatform, with Android's parts in androidMain and Hilt as before

**Date:** 2026-10-03 · **Phase 8.3 of the roadmap; the third step of DECISION-085.**

**Why:** the session, the preferences, the cache and the location contract are what every
repository is built on. Each mixed platform-free code with Android code (the Keystore, DataStore,
Room, the location manager) and with Hilt, which does not exist on iOS.

**Decision:**

* **`core:auth`, `core:database`, `core:datastore` and `core:location` are multiplatform
  modules.** Each keeps one module, split by platform:
  * `commonMain` holds the contracts and the rules: the session coordinator and token stores,
    the cache-first rule and the stores' interfaces, the preferences' shape, the location
    provider. Their tests are in `commonTest`.
  * `androidMain` holds the Android implementations and their Hilt modules: the Keystore vault,
    Room and its DAO, the DataStore, the Android location provider.
  * The iPhone app will add `iosMain` implementations of the same contracts.
* **`serva.kmp.hilt`** is the convention for such a module. It runs Hilt's compiler (and Room's,
  where there is one) through KSP on the Android target, and turns on that target's Java
  compilation (`withJava()`), because Hilt's processor writes Java. Without it the Hilt modules
  compiled but the app's graph never saw them (`MissingBinding`).
* **`core:inject`: `@Inject` and `@Singleton` that shared code can carry.**
  * They are `expect` annotations. On Android they are `actual typealias`es of javax.inject's
    own, so a shared class compiles with `javax.inject.Inject` on its constructor and Hilt
    builds it like any other class; Dagger writes the missing factory in the app. On iOS they
    are empty annotations.
  * This is what lets the repositories and use cases move later without each losing its
    `@Inject constructor` to a hand-written provider.
  * Proven before it was adopted: a probe class in common code was injected through the app's
    graph, and Dagger generated its factory.
  * `-Xexpect-actual-classes` acknowledges that expect/actual classes are Beta.
* **`MemoryAccessTokenStore`** keeps its token in a `@Volatile` field instead of an
  `AtomicReference`, which is JVM-only. It is only ever read and replaced whole.
* **The JVM harness** compiles the common code of the shared modules and runs their common
  tests. Its own `typealias Inject = javax.inject.Inject` stands in for the expect declarations
  it cannot compile, and its Android-only exclusions for these modules are gone.

**Not yet:**
* `core:network` waits on its own step. It needs Ktor in place of Retrofit and OkHttp, and a
  client generated for Kotlin Multiplatform in place of the JVM one, which carries `java.util.UUID`
  and `java.time` throughout.
* `core:maps` waits for the screens' step, because it is part Compose.

## DECISION-088 — The network's contracts are shared; Android keeps its transport in androidMain

**Date:** 2026-10-03 · **Phase 8.4 of the roadmap; the fourth step of DECISION-085.**

**Why:** every repository talks to the backend through `core:network`'s boundaries, so they have
to be in common code before any repository can be. The transport behind them is another matter.
It is Retrofit and OkHttp on a client generated for the JVM, with `java.util.UUID` and
`java.time` in 270 places, and replacing it is its own step.

**Decision:**

* **`core:network` is a multiplatform module** (`serva.kmp.hilt`, with the serialization
  plugin).
  * **`commonMain`** holds what a repository depends on, all with `@Inject` from `core:inject`:
    * the public, owner, auth and push boundaries and their inputs;
    * the realtime models, invalidation rules, bus, deduplicator, reconnect policy and the
      stream's contract;
    * the maintenance state, envelope, retry policy and coordinator;
    * push registration and the push payload;
    * sign-out;
    * the place-name resolver;
    * the network monitor's contract;
    * the API environment.
  * **`androidMain`** holds Android's transport:
    * the generated JVM client, compiled from `packages/api-kotlin` as before;
    * Retrofit and its adapters and mappers;
    * the OkHttp interceptors (token, request id, refresh, maintenance) and the realtime socket;
    * Valhalla, OSRM and Nominatim;
    * the Android network monitor and upload reader;
    * the Hilt modules.
  * Nothing about how the Android app talks to the server changes.
* **What moving to common code changed:**
  * `Retry-After` dates are read with kotlinx-datetime's RFC 1123 format instead of
    `java.time`'s, and the envelope's clock is a `kotlin.time.Instant`. The tests for both forms
    of the header pass unchanged.
  * The realtime deduplicator's `@Synchronized` became an atomicfu lock. atomicfu 0.26.1 is
    already in the graph through kotlinx-coroutines; the catalog now names it.
  * Push registration holds the last token in a `@Volatile` field instead of an
    `AtomicReference`.
  * The push payload's day is a `kotlinx.datetime.LocalDate`. Its only reader, the notice
    intent, uses its ISO text, which is the same.
  * The place-name resolver's clock is `kotlin.time.Clock` instead of `System.currentTimeMillis`.
  * Multiplatform modules now enable core-library desugaring on their Android target, as the
    Android libraries always have. Without it lint refused the generated client's `java.time`
    at minSdk 24.
* **Tests:**
  * Those whose subjects are common moved to `commonTest` on `kotlin.test`, so they also run on
    the iPhone simulator: reconnect, realtime config and invalidation, deduplication, push
    payload and registration, notification permission.
  * Those that drive OkHttp's mock server stay JUnit tests in `androidHostTest`.
  * 119 tests before, 119 after.
* **The JVM harness** compiles the module's common code and, since it is plain JVM code, its
  Android transport as well, with their tests. The qualifiers read the files where they now
  live.

**Next:** a shared transport. Ktor on a client generated for Kotlin Multiplatform (openapi-generator's
`multiplatform` library, with dates mapped to `kotlin.time.Instant`, since kotlinx-datetime 0.8
no longer has its own `Instant`), implementing these same boundaries for iOS. Android can move
to it once it is proven.

## DECISION-089 — The features' repositories and use cases are shared; their screens stay Android's for now

**Date:** 2026-10-03 · **Phase 8.5 of the roadmap; the fifth step of DECISION-085.**

**Why:** with the models, the core layers and the network's contracts in common code, the
repositories and use cases are the last layer an iPhone screen needs below it. They live in the
feature modules beside the screens. The map's rules, which the map and navigation features
build on, live in `core:maps` beside MapLibre.

**Decision:**

* **`serva.kmp.feature`** is the convention for a module whose Android side draws screens. It is
  `serva.kmp.hilt` plus:
  * the Compose compiler, limited to the Android target, since nothing on iOS is composable yet;
  * Android resources on the Android target, so the strings and drawables keep the module's own
    `R` as before.
* **All fourteen feature modules, `core:maps` and `core:testing` are multiplatform.**
  * A feature's repositories, use cases and plain rules are in `commonMain`, with `@Inject` from
    `core:inject`. Its screens, view models, copy, Hilt modules and `res/` are in `androidMain`.
  * A conversion script made the split, by rule: a file is common unless it is a screen, view
    model, copy or module, imports Android, the JVM, Compose or the design system, or uses a
    declaration that is itself Android's.
  * A few files the rule kept on Android only because of a name they share with an Android
    declaration can move later, one at a time.
  * `core:maps` shares its camera, route, pack, geometry and navigation models. MapLibre, the
    offline packs and the composables that host the map stay in `androidMain`.
    * `java.lang.Math`'s degree conversions became the same multiplications by the same
      constants, so the bearings and the tile maths are what the JVM gave, to the last bit.
    * `RoutingException` is an `Exception` rather than a `java.io.IOException`. Nothing caught
      it as one; its one reader asks for its `failure`.
  * `core:testing`'s fakes are common, so a shared module's tests can use them on every
    platform. Its JUnit rule for the main dispatcher stays in `androidMain`. One fake's
    `toSortedMap()` became a sort by key.
* **The tests did not change.** They moved from `src/test` to `src/androidHostTest` and still run
  on the JVM against the shared code, as JUnit tests, through `testAndroidHostTest`. Moving them
  to `commonTest`, so they also run on the iPhone simulator, is a later step, module by module.
* **The JVM harness** compiles each feature's `commonMain` and `androidMain`, less its Android-only
  list, which is what it compiled before. The qualifiers read the files where they now live, and
  their test exclusions cover every test source set.

## DECISION-090 — A second client, generated for Kotlin Multiplatform, for the iPhone's transport

**Date:** 2026-10-03 · **Phase 8.6 of the roadmap.**

**Why:** the iPhone app needs a transport behind the boundaries `core:network` already shares
(DECISION-088). The JVM client cannot be that transport: it is Retrofit, OkHttp and `java.time`
throughout. Writing DTOs by hand is a contract violation here. So the transport needs a client
generated from the same schema, for every platform.

**Decision:**

* **`packages/api-kotlin-multiplatform`** is generated by `scripts/generate-api-clients.sh`
  beside the other three, from `openapi/config/kotlin-multiplatform.json`. It uses:
  * openapi-generator 7.15.0's `multiplatform` library: Ktor for the transport and
    kotlinx.serialization for the bodies;
  * kotlinx-datetime for days and times;
  * **`kotlin.time.Instant` for every moment.** The generator's default,
    `kotlinx.datetime.Instant`, no longer exists in kotlinx-datetime 0.8. kotlinx-serialization
    1.9 serializes the standard library's own `Instant`.
* **Its own package**, `com.servacode.directory.api.multiplatform`. The JVM client's classes
  are `com.servacode.directory.api.*`; the same names in both would collide in any Android
  module that sees the two, which a transport module beside `core:network` does.
* **Only the sources are generated** (`--global-property apiTests=false,…`). The docs and the
  test stubs the generator would write beside them are not, which keeps the package at 369 files
  instead of about 1,000.
* **The CI drift gate** (`contract-drift`) covers the new package as it covers the other three.
  Regenerating left the three existing clients byte for byte as they were.
* **`:core:api`** compiles the generated sources, where they are, for Android and iOS.
  * Ktor is **3.4.3**, the newest release that needs neither a newer kotlinx-coroutines (1.10.2)
    nor a newer standard library than the app has, so nothing on Android moves.
  * Its tests drive the client through Ktor's mock engine, in `commonTest`, so they run on the
    JVM and on the iPhone simulator alike. They check:
    * that the status request goes to `/api/v1/platform/status/`;
    * that a moment with an offset and microseconds, as Django writes it, is the right instant;
    * that a day is a `LocalDate`.
* **Android does not use it yet.** The app still talks to the server through the JVM client.

**Next:** the Ktor transport behind the shared boundaries, built on this client: the same
mapping to the app's models as `core:network`'s Android adapters, the bearer token, the one
refresh at a time, the request id and the maintenance envelope.

## DECISION-091 — The iPhone's transport: the shared boundaries on Ktor, as Android's adapters do them

**Date:** 2026-10-03 · **Phase 8.7 of the roadmap.**

**Why:** the iPhone app reaches the backend through the same boundaries as Android
(DECISION-088). It needs an implementation of them that runs on iOS, built on the
multiplatform client (DECISION-090), and behaving as Android's adapters behave. Two
implementations that disagree would mean two apps that disagree.

**Decision:**

* **`:core:transport`** implements every shared boundary over Ktor and `:core:api`, in common
  code:
  * the public API, the owner API and the auth API, with its refresh gateway;
  * push registration, the maintenance probe and the analytics sender.

  Each is a port, call for call, of the Android adapter it mirrors. The tests are ports of
  Android's, and they drive a Ktor mock engine.
* **`TransportClients`** gives the transport two Ktor clients that keep Android's rules:
  * **every request:** an `X-Request-ID`;
  * **a 503:** a maintenance envelope puts the app into maintenance, and the next normal answer
    takes it out;
  * **the signed-in client:** carries the in-memory access token. A 401 asks the session
    coordinator for one refresh and sends the request once more. Requests that fail together on
    one expired token spend the secret once; a test sends eight at the same moment.
* **`TransportErrors`** turns failures into the app's errors exactly as `ApiErrorMapper` does:
  * the backend envelope, or the status and the header id when there is none;
  * an IOException, on the JVM or Darwin, is offline;
  * a body that breaks the contract is unexpected.
* **What Android's adapters got from the JVM, reproduced in common code:**
  * a UUID's lenient parsing and lower-case text;
  * `java.net.URI`'s answer to "is this a safe https address";
  * coordinate rounding to four decimals with a point;
  * OkHttp's quoting of an upload's file name.

  Each was checked against the JDK and OkHttp themselves, over a million cases or more, before
  those temporary checks were removed.
* **`ClientPlatform`.** The platform a session, a push token and the release check are
  recorded under is a parameter, `ANDROID` or `IOS`. The backend accepts both, and a test pins
  that the iPhone sends `IOS`.
* **Where it differs from Android, because the multiplatform client or Ktor works otherwise:**
  * Opening hours are times in this client, not strings. Text that is not a time fails before
    any request, where Android let the backend refuse it.
  * Multipart part names go unquoted, and the id part carries no `text/plain`. The values are
    the same.
  * Every request says `Accept: application/json`.
  * Moments go out with their seconds (`00:00:00Z`, not `00:00Z`). It is the same instant.
  * Ids and image addresses in answers are kept as the backend sent them, not re-parsed.
* **Android does not use it yet.** The Android app keeps Retrofit until this transport has run
  in the iPhone app. Moving Android onto it, and removing the JVM client, is a later step.

**Next:** the iPhone app's shell.
* It wires this transport with Ktor's Darwin engine.
* It provides the platform implementations the shared contracts need: the Keychain for the
  session, preferences, location and the cache.
* It shows its first screens.

## DECISION-092 — The iPhone's platform parts: Android's storage shared whole, and the rest on Apple's frameworks

**Date:** 2026-10-03 · **Phase 8.8 of the roadmap.**

**Why:** the shared repositories stand on four platform contracts: the preferences, the cache
and the device's own stores, the position, and the network's state. On Android each has an
implementation in androidMain (DECISION-087). The iPhone needs the same four, and the transport
(DECISION-091) needs an HTTP engine. For storage there were two ways to get them: write iPhone
versions beside Android's, or make Android's own code run on both. Two versions of a database's
queries, or of the preferences' keys, would be two places to keep in step for the life of the app.

**Decision:**

* **The preferences are one implementation.**
  * `PreferencesRepository` and `StoredAnonymousId` move from androidMain to common code, on
    DataStore's multiplatform core. They keep the same class names and the same keys, and
    a test pins the keys Android has always written.
  * They share one `DirectoryDataStore`, because DataStore allows one instance per file.
    * Android makes it from its Context: the same file as before, `directory_preferences`.
    * The iPhone makes it under Application Support.
  * The anonymous id is still a random version-4 UUID in lower case, now from `kotlin.uuid`.
* **The database is one implementation.**
  * Room's entities, both DAOs, the database and the three stores built on them
    (`PublicCacheDataSource`, `RoomRecentlyViewedStore`, `RoomEmergencyNumbersCache`) move to
    common code.
  * Room generates them for Android and for both iPhone targets.
  * The schema is unchanged; `schemas/…/2.json` is byte for byte the same.
  * Each platform only opens the database:
    * Android, in androidMain, with its framework SQLite and the 1→2 migration (only Android
      ever had a version 1).
    * The iPhone, in iosMain, with the system's SQLite (`NativeSQLiteDriver`, linked with
      `-lsqlite3`). Its driver is androidx.sqlite 2.7 on the iPhone alone: the 2.6 that Room
      brings refers to `sqlite3_load_extension`, which Apple's SQLite is built without, and
      nothing links (b/434324365).
* **Nothing the app stores on the iPhone goes into its backups.** The folders that hold the
  database and the preferences are excluded from iCloud backup. This matches Android's
  `allowBackup="false"` and Apple's rule for anything an app can download again.
* **On Apple's frameworks, with Android's rules:**
  * **`IosLocationProvider` (Core Location):**
    * It never asks for the permission; without it, every call answers PermissionDenied.
    * It waits for a fresh fix up to the timeout, then falls back to the last known position.
    * `precise` means the reader allowed the precise position.
    * A stream sends at most one position per interval, never faster than twice a second.
    * Only a denial ends a stream.
  * **`IosNetworkMonitor` (the Network framework's path monitor):** "unmetered" means neither
    expensive (mobile data, a hotspot) nor constrained (Low Data Mode).
  * **`darwinEngine()`:** Ktor on NSURLSession. A refused connection reaches the app as offline,
    as OkHttp's does on Android.
* **Tested on the iPhone simulator,** by the macOS job that already runs every shared module's
  tests there:
  * the cache, the recently viewed list and the emergency numbers on the system's SQLite;
  * the preferences written to a real file by one instance and read by the next;
  * the folders' backup exclusion;
  * the location and network rules;
  * a refused connection through the real engine.

  The preferences' common tests also run on Android. The job selects Xcode 26, because Kotlin
  2.3's iOS libraries are built against its SDK and CoreLocation does not link with an older one.
* **The JVM harness** compiles from Maven Central alone, so it leaves out the files that need
  Room or DataStore, which come from Google Maven. It compiles exactly what it compiled while
  those files sat in androidMain.

**Not here:** the session's refresh secret in the Keychain. The simulator lets only a process
inside an app reach the Keychain, and the shared modules' tests run as plain processes. The
vault arrives with the app, tested inside it.

**Next:** the iPhone app's shell.
* An Xcode project that CI builds and tests on the simulator.
* The Keychain vault, tested inside the app.
* The shared layers wired by hand.
* The first screens.

## DECISION-093 — The iPhone app's shell: an Xcode project from a file, one Kotlin framework, tests inside the app

**Date:** 2026-10-03 · **Phase 8.9 of the roadmap.**

**Why:** everything below the screens is shared and runs on the iPhone (DECISIONS 085 to 092).
What was missing is the app itself:
* a project Xcode builds;
* the shared layers wired together without Hilt;
* somewhere to keep the session's refresh secret;
* screens.

It also needs a check in CI, so the iPhone app cannot break unnoticed while all the work happens
in Kotlin.

**Decision:**

* **`apps/ios` is generated, not committed.**
  * `project.yml` is the project; XcodeGen writes `Daliini.xcodeproj` from it.
  * A change to the project is a readable diff, never a pbxproj merge.
  * The Swift is one file that gives the shared screens a window, plus the app's tests.
* **One static framework, `DaliiniKit`, from `:ios-framework`.**
  * It is iOS-only, on its own convention `serva.ios.framework`.
  * Xcode builds it in a build phase before the app, with Gradle's
    `embedAndSignAppleFrameworkForXcode`.
  * Static because it is linked into the app and needs no embedding or signing of its own. The
    app links the system SQLite the database needs (`-lsqlite3`).
* **The graph is wired by hand.** `ShellGraph` builds what Hilt builds on Android: the
  transport on the Darwin engine with `ClientPlatform.IOS`, the session coordinator, and the
  shared stores, repositories and use cases. Every class in it is shared; only the platform parts
  are the iPhone's.
* **The refresh secret is in the Keychain.** `KeychainRefreshTokenVault` keeps one
  generic-password item, readable after the first unlock and on this device only.
  * It is never restored to another phone from a backup, as Android's Keystore key never leaves
    the phone.
  * A source check requires the this-device-only attribute.
* **Tests inside the app.** The simulator gives the Keychain only to an app. So the vault's
  tests are an XCTest bundle hosted in the app:
  * a secret is kept and replaced;
  * it outlives the object that wrote it;
  * it can be cleared twice;
  * Arabic text survives exactly.

  The same bundle checks the build's configuration, that every word the screens use is in the
  strings file, and that the shared screens load.
* **Two first screens in Compose Multiplatform:** the province, and that province's home
  (on duty now, open now, nearest, categories).
  * They read the shared use cases as Android's view models do, and show cached data first,
    marked when it is stale.
  * Their colours come from the same generated tokens as the site and Android.
  * Their words live in the app's `ar.lproj/Shell.strings` under Android's own keys and
    sentences, not in Kotlin. The rule that keeps words out of Kotlin now covers this module.
  * Android's screens replace these two when they move to Compose Multiplatform.
* **Configuration from the build.**
  * Debug points at a backend on the same Mac (`http://localhost:8000/`), the only cleartext
    App Transport Security allows.
  * Release carries a placeholder, which the app refuses and reports as not connected to a
    server.
  * The source check forbids background location and arbitrary cleartext in the project.
* **CI.** A new `ios-app` job on macOS:
  1. It generates the project.
  2. It builds the app with its framework.
  3. It runs the hosted tests on a simulator.

  `ios-shared` runs the framework's Kotlin tests with the other modules'. Those tests use the
  real wiring against a mock backend: provinces fetched through the shared transport and kept in
  the shared cache, a choice kept in the preferences, and the home asking for a province.

**Not yet:**
* Android's screens in Compose Multiplatform, with the brand's fonts.
* The map (MapLibre on iOS).
* Notices (APNs).
* Asking for the location from a screen.
* Signing and the App Store, which need the owner's Apple developer account.

**Next:** the screens move to Compose Multiplatform for both apps.

## DECISION-094 — The design system is shared: Compose Multiplatform, with its pictures drawn from the token package

**Date:** 2026-10-03 · **Phase 8.10 of the roadmap.**

**Why:** the screens move to Compose Multiplatform next (DECISION-051), and every screen is
built from the design system: its theme, components, words, icons and illustrations. That
module was an Android library, Android through its resources rather than its code. Its words,
fonts, icons and illustrations were `R` ids, and its pictures took their dark colours from
Android's night resources.

**Decision:**

* **`:core:designsystem` is a multiplatform module on Compose Multiplatform 1.12.1**, on a new
  convention, `serva.kmp.compose`.
  * On Android, its Compose is androidx Compose itself, at the app's versions: Compose UI 1.12.1,
    material3 1.4.0 and lifecycle 2.11.0. The app's classpath is unchanged.
  * On the iPhone, it is JetBrains' build, with material3 1.9.0 (what its Gradle plugin pairs
    with 1.12).
* **Icons and illustrations are drawn, not looked up.**
  * The token package now also writes the shared set's path data as Kotlin
    (`DirectoryVectors.kt`), beside the web's TypeScript and Android's vector drawables.
  * The design system builds that path data into vectors in the theme's colours:
    * an icon is a `DirectoryGlyph`, a path with a colour of the theme;
    * an illustration has three layers: soft ground, lines, accent.
  * So a picture follows the theme the reader chose, light or dark, on both platforms, without
    Android's night configuration. The site and the console draw the same paths.
  * The fourteen icons this module drew itself keep their exact paths, strokes and colours
    (`ModuleGlyphs`).
  * A tinted icon wears its tint, as before. The one drawn untinted, the red close mark, wears
    the theme's danger and on-primary colours.
* **Words, faces and the brand symbol are Compose resources**, read the same way on both
  platforms.
  * They are assembled at build time from their owners, so no file is copied into the
    repository:
    * the module's own words and its brand symbol;
    * the vocabulary the token package generates;
    * the brand's faces from the token package.
  * Compose resources keep a string exactly as written, where Android's resources had trimmed and
    folded it. Two differences came out:
    * **The list separator.** Android had trimmed "، " to "،", so lists were joined without a
      space. The space now stays, which is what was written.
    * **One sentence wrapped across two lines in the file.** It is now one line, and a test
      forbids a wrapped word.
  * `@ReadOnlyComposable` comes off the screens' word getters that read the design system's
    words, because Compose Multiplatform's `stringResource` is not read-only.
* **What only Android has stays in androidMain:**
  * The theme's night configuration for Android resources, and the system bars' appearance
    (`PlatformTheme`). The iPhone's is empty for now.
  * `SystemBarsColor`.
  * The Android resources that Android's XML, widget, notifications and map pins name by id:
    * the brand colours, `brand_mark` and the symbols;
    * the token package's colours and drawables;
    * the five words the widget and notifications read;
    * the three category drawables MapLibre paints into pins.

  Tests hold the copied words and the category drawables to the shared ones, key by key.
* **Pictures from the network are Coil on both platforms.** Android keeps its Coil (3.6). The
  iPhone's is 3.4, the newest built with Kotlin 2.3: 3.5 and later are built with 2.4, whose
  libraries this compiler cannot read on iOS.
* **The iPhone shell draws with the design system:** the theme, the top bar, menus, the loading,
  error and offline states, and the icons. Its errors use the shared sentences.
* **Every Gradle module but the app is now multiplatform.** CI's unit tests and lint are
  `testAndroidHostTest`, `:app:testLocalDebugUnitTest`, `lintAndroidMain` and
  `:app:lintLocalDebug`; `testDebugUnitTest` and `lintDebug` no longer name any module.
* **Tests:**
  * Android host tests: the copied words, the category drawables against the glyphs, the status
    tones.
  * Common tests, on Android and on the simulator: colours, glyphs in both themes,
    illustrations, one mark per category.
  * A simulator test draws the theme with a shared word, an error's sentence, an icon and an
    illustration, in light and in dark.

**Next:** the screens. Android's screens and view models move from each feature's androidMain to
common code, one feature at a time, and replace the shell's two.

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
