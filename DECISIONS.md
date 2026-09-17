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
