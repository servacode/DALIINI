# Project Status

Last updated: 2026-09-29

## Baseline

- Master Spec: V3
- Architecture: Django + Next.js + Kotlin + Swift
- Launch baseline: Raqqa / Pharmacy / Duty
- Repository mode: Greenfield
- Source of truth: `docs/spec/`
- Verification baseline: `artifacts/evidence/RECEIPT-AUDIT-2026-09-17.md` (accepted 2026-09-17)

## Qualification rule

A gate is PASS only with a real command exit code. Textual source assertions do not qualify a
gate on their own — that method let four P0 defects through every gate up to P21, and a text match
inside a migration file kept reporting PASS on an invariant the model had already stopped declaring.

## Quality debt

`DECISIONS.md` carries the register. DEBT-001 ruff (99 issues on 2026-09-19, from a 106 baseline
at intake), DEBT-002 mypy (645 in the backend container across 228 source files on 2026-09-19,
against a 797 baseline over 191 files measured the same way; the
earlier 556 was a host-side under-count and is corrected in DEBT-002), DEBT-003 the deferred
`PermissionsMixin` evaluation. All are mandatory before
staging or production closure and none of them blocks the Admin binding work. Standing rule: no new
lint or type debt in a touched file.

**2026-09-29: DEBT-001 and DEBT-002 are closed.** `ruff check .` reports 0 and `mypy .` (strict,
plugin loaded) reports 0 errors in 308 source files. The backend CI job's six commands all pass
locally, and `pytest` no longer needs object storage. DEBT-003 stands.

## Contract discipline

Django and DRF are the source of truth, `openapi/schema.yaml` is generated from them, and the three
client packages are generated from it. No consumer hand-writes a transport DTO. After any change that
touches a request or response, regenerate both and commit the artefacts with the source change; the
CI `contract-drift` job fails otherwise. See `openapi/README.md` and DECISION-010 to DECISION-026.

The boundary conventions are settled as of 2026-09-18: one error envelope `{code, message, details,
requestId}` from a single DRF exception handler, one cursor envelope `{items, nextCursor, hasMore}`
from a single pagination class, and camelCase everywhere. Column names are never exposed; a wire
name that differs from a column is translated with `source=` and never by a `RenameField`.

## Phase table

| Phase | Status | Implemented | Tested | Remaining | Evidence | Commit | Last update |
|---|---|---|---|---|---|---|---|
| P0 Governance | CLOSED | Repository initialized; verified spec preserved; governance/tooling/CI skeleton created | Source checksums, governance validator, JSON parse, whitespace, framework and secret-file hygiene passed | None | `artifacts/evidence/p0-governance-20260917.txt` | 871d4c7 | 2026-09-17 |
| P1 Design Tokens | CLOSED | Canonical tokens, generator outputs for TS/CSS/Kotlin/Swift, font policy, component specs | Token validation, WCAG contrast checks, deterministic drift check, governance regression | None | `artifacts/evidence/p1-design-tokens-20260917.txt` | 9c4031c | 2026-09-17 |
| P2 Backend Foundation | CONNECTED_VERIFIED | Django/GeoDjango settings, Channels ASGI, Celery, Redis cache/channel/broker, S3 boundary, request IDs/structured logs, health, Docker, MinIO init and CI definition. FIX-P0 restored ASGI boot and the request path. This batch closed INT-009 migration drift, INT-028 test collection, INT-007 test database engine and INT-030 (FOR UPDATE across a LEFT OUTER JOIN in submit_facility), and hardened the submitted-uniqueness qualifier from a migration text match to a model-state check. | Executed on PostgreSQL 17.5 + PostGIS from an image built with the unmodified Dockerfile: check/migrate/makemigrations --check all exit 0 with convergence on a second pass; fresh database and existing-database upgrade both clean with every pg_class.oid preserved; `uv run pytest` 49 passed exit 0; both business invariants proven rejected by the database; /health/live/ 200, /health/ready/ 200 with database and redis ok, /public/provinces/ 200, /public/facilities/ 400, /admin/dashboard/ 403. | Connected Celery, S3 and Channels qualification; INT-010 unauthenticated 500 on /owner/facilities/; INT-029 Redis error propagating after commit. Security, load and restore belong to P20. | `artifacts/evidence/p2-connected-20260917.txt`, `artifacts/evidence/fixp0-runtime-20260917.txt` | P2-connected | 2026-09-17 |
| P3 Auth/RBAC | SOURCE_IMPLEMENTED | Custom UUID user/canonical Syrian phone, OTP digest/challenge flow, registration/login, JWT access, opaque rotating refresh with grace/reuse compromise handling, session list/revoke/logout-all, recovery reset, profile GET/PATCH, account deletion request/anonymization with last-owner protection, Admin RBAC and redacted audit; Auth surface restored during P20 integrity review | 12 tests authored; governance/design regression, compileall, AST, line-length, persisted-secret invariants, phone executable smoke, YAML and whitespace passed. Django tests not executable in current environment. | `uv` dependency resolution; ruff/mypy/pytest; migration drift/system checks; PostgreSQL/Redis connected qualification | `artifacts/evidence/p3-auth-rbac-source-20260917.txt` | f9408d8 | 2026-09-17 |
| P4 Taxonomy | CONNECTED_VERIFIED | Cities/neighborhoods, dynamic groups/categories, capabilities, independent province switches, verification policies, specialties/service tags, public selectors/DTOs, audited mutation services. **Correction:** the 2026-09-17 entry listed a "14-province seed and Raqqa/Pharmacy/Duty launch baseline" as implemented; no seed existed in any form, which is INT-006 and is exactly the kind of claim a source-text gate cannot catch. The seed now exists: a frozen `launch_v1` dataset applied by `directory/0004_launch_baseline` and by `seed_launch_baseline`, sharing one implementation. INT-040 corrected `Category.Specialization` to the four values the specification names. | Executed on PostgreSQL 17.5 + PostGIS: 14 provinces with Raqqa active and 13 inactive, 1 group, 5 categories, 70 province switches with only Raqqa+Pharmacy on; deterministic UUIDv5 identity identical between a fresh seed and an upgraded database; the seed is idempotent across three runs and provably does not revert four operator changes; a deleted canonical row is restored at the same id; existing-database upgrade passes 10/10 with operator rows untouched; `/public/provinces/` serves Raqqa, Raqqa's taxonomy serves pharmacies only, owner onboarding is offered for Raqqa+Pharmacy alone; 25 seed tests inside 158 passing. | Not `CLOSED`: the Admin taxonomy is still read-only (INT-018), so `Cycle J` cannot be executed from the Admin, and pharmacy verification requirements are unresolved (LAUNCH_POLICY_PENDING). No city or neighborhood dataset exists. | `artifacts/evidence/launch-baseline-20260918.txt` | P4-launch-baseline | 2026-09-18 |
| P5 Owner Domain | SOURCE_IMPLEMENTED | Facility lifecycle, OWNER/MANAGER membership, draft/reverification applications, owner config/facility APIs, current-policy submission validation, admin review/suspend/reactivate/close services, public image boundary, private verification evidence, safe image re-encode/limits, audit hooks | 12 P5 tests authored; governance/design drift, full backend AST/compileall, P5 line-length, DTO storage-key privacy, preload image-dimension gate and whitespace passed. Django/PostGIS/S3 runtime tests unavailable locally | Run ruff/mypy/pytest, migration drift, PostgreSQL row-lock/concurrency, S3 media tests and admin lifecycle qualification before gate closure | `artifacts/evidence/p5-owner-domain-source-20260917.txt` | 11aa673 | 2026-09-17 |
| P6 Availability/Duty | SOURCE_IMPLEMENTED | Weekly hours, overnight-safe schedule validation, temporary closures, Damascus availability engine, pharmacy duty CRUD, PostgreSQL exclusion constraint + btree_gist, owner membership boundary, source migrations/tests | Static source qualification passed; runtime tests authored but not executable here | Run Django checks/migrations/pytest against PostgreSQL/PostGIS; prove duty overlap/concurrency and owner API integration before closure | `artifacts/evidence/p6-availability-source-20260917.txt` | 64b6fda | 2026-09-17 |
| P7 Public Discovery | SOURCE_IMPLEMENTED | Public provinces/cities/categories, home, list/detail, PostGIS nearest, bbox map, search, SQL availability filters, specialties/services, ratings | Static source qualification passed; runtime/API/PostGIS tests authored/partially authored but not executable here | Run Django checks/migrations/pytest; connected PostGIS nearest/bbox; EXPLAIN indexes; API/permission integration before closure | `artifacts/evidence/p7-public-discovery-source-20260917.txt` | 4c8f535 | 2026-09-17 |
| P8 Realtime | SOURCE_IMPLEMENTED | Event catalog/envelope, hashed groups, post-connect auth, province/user/admin scopes, RBAC admin gate, after-commit publisher, facility/taxonomy/availability/application hooks | Static source qualification passed; connected Channels/Redis tests unavailable | Run WebsocketCommunicator + Redis delivery + rollback/no-event + auth integration before closure | `artifacts/evidence/p8-realtime-source-20260917.txt` | 1b7212a | 2026-09-17 |
| P9 Content Services | SOURCE_IMPLEMENTED | First-party ads targeting/scheduling/action validation, public ads + Home integration, encrypted/digested device push tokens, notification persistence, Celery push delivery boundary, FCM interface/APNs placeholder, centralized privacy-minimized analytics registry/events/retention | Static qualification passed: compile/AST, P9 line length, analytics forbidden-field tests, push-token-at-rest invariants, ad HTTPS/storage privacy, governance/design regression and whitespace | Run Django migrations/pytest; Celery delivery; connected FCM with owned credentials; notification/device API integration during contract/mobile phases | `artifacts/evidence/p9-content-services-source-20260917.txt` | b63ce69 | 2026-09-17 |
| P10 Contracts | CONNECTED_VERIFIED | Canonical Django-generated OpenAPI with a real contract: 140 component schemas, 35 request bodies, 72 response schemas, an explicit bearerAccessToken security scheme and 84 hand-declared operation ids. Shared error, enum and value-object components. `openapi/schema.yaml` and `openapi/schema.sha256` are tracked. TypeScript, Kotlin and Swift clients generated with pinned openapi-generator 7.15.0 and committed. Drift gate rewritten; the previous one could not fail. Schema endpoint exposure gated by environment. | `manage.py spectacular` exits 0 with 0 errors and 0 warnings, down from 328 errors and 76 warnings. Generation is byte-identical across two independent runs. 12 contract tests pass. 17/17 runtime responses validated against the document on PostGIS and Redis, covering auth, public list and detail, owner mutation, admin mutation, validation failure and permission failure. TypeScript client compiles, `tsc --noEmit` exits 0. | Kotlin client compilation is NOT_VERIFIED on this network; Swift is a contract artefact only. INT-035, INT-036, INT-037 and INT-038 were closed by the CONTRACT ALIGNMENT batch on 2026-09-18 and the artefacts regenerated; INT-039, the `AdminCapabilities*` snake_case pair, remains open. | `artifacts/evidence/p10-contracts-20260918.txt` | P10-contracts | 2026-09-18 |
| P11 Public Web | SOURCE_IMPLEMENTED | Next.js RTL public site with landing, privacy, terms, support, delete-account resource, shared design tokens, env-only domain/contact config and security headers | Static source qualification passed: package/config parse, required routes, RTL/token usage, no hardcoded hex in app CSS, security headers, no invented production domain/email, whitespace | Install Node 24 + pnpm dependencies; lint/typecheck/build; Playwright/accessibility; finalize legal/company/contact text before release | `artifacts/evidence/p11-public-web-source-20260917.txt` | 4dc28eb | 2026-09-17 |
| P12 Admin Foundation | CONNECTED_VERIFIED | Same-origin BFF: login, refresh, logout through `authLogin`/`authRefresh`/`authLogout`; both tokens in HttpOnly cookies named from `ADMIN_PUBLIC_ORIGIN` (`__Host-` + Secure on https, bare on loopback, boot refused on http elsewhere); refresh coalesced per session, one retry after 401 only; fail-closed origin/CSRF; per-request CSP nonce in `proxy.ts`; `/admin/me/` drives a permission-aware shell; one operation registry over the generated client; central error mapper. | Playwright in real Chromium against a production build, Django, PostGIS and Redis: wrong and correct password, cookie attributes, tokens unreadable from the page, rotation, logout, tampered refresh, CSRF with no Origin and with a foreign Origin, mutation with no GET form, full and limited navigation, URL typing refused by the backend. Vitest: cookies, CSRF, CSP, coalescing, retry policy, error mapper, pre-hydration login HTML. No backend secret or API origin in the client bundle. | apps/web HSTS (INT-024) and tsconfig rewrite (INT-023) remain. | `artifacts/evidence/admin-binding-20260919.txt`, `artifacts/evidence/admin-bff-auth-20260918.txt` | P11-P13 | 2026-09-19 |
| P13 Admin Operations | CONNECTED_VERIFIED | Every operational page bound: dashboard, reviews and detail with approve/reject, facilities with suspend/reactivate/close, users with block/unblock and role replacement, taxonomy groups and categories with capabilities and province switches, provinces, verification requirements, ads with scheduling, audit with filters and diffs, analytics, typed settings, system status. Backend gained `/admin/me/`, group/category create and update, requirement update, ad update; INT-015/018/039/041/042/043/044 fixed. | 31 Playwright golden paths: review list and filters, rejection with audit, approval that reaches the public listing, refused second decision, facility suspend/reactivate in audit, user block/unblock in audit, Cycle J switch on and off reflected in the public API, duty refused off pharmacy, new category invisible until switched, Aleppo activation surviving a seed re-run, requirement lifecycle, ad schedule validation and activation, read-only screens, limited operator refused on direct mutation and evidence. 211 backend tests. Contract drift PASS. | Evidence streaming is now verified (2026-09-19, ANDROID GOLDEN PATH): an owner's evidence, stored in MinIO's private bucket, opened by an operator in the production Admin build — the owner's JPEG, typed, `no-store`, audited, no storage key anywhere (INT-066, INT-067) — then approved; the private object refuses anonymous reads (403). The phase stays CONNECTED_VERIFIED: raising it to CLOSED is the owner's decision. LAUNCH_POLICY_PENDING open. | `artifacts/evidence/admin-binding-20260919.txt`, `artifacts/evidence/admin-bff-auth-20260918.txt`, `artifacts/evidence/android-golden-path-20260919.txt` | db7ac98 | 2026-09-19 |
| P14 Android Foundation | CONNECTED_VERIFIED | Native Kotlin/Compose 27-module graph with the P10 client compiled into `:core:network`; one adapter, error mapper and mapper set; anonymous and authorized clients on separate dispatchers; session id kept with the refresh secret; refused-versus-transient refresh; local/staging/production flavors with cleartext only in local; core library desugaring; missing module dependencies declared (INT-055) | First real Android build, on GitHub Actions against Google Maven and Maven Central (run 35430793735): `assembleLocalDebug` PASS, 104 Android unit tests in 16 modules PASS, lint 0 errors, P14 and P21 qualifiers PASS; APK `a6f36ac8…` with no secret or fixture credential. It took eleven root-cause fixes (INT-070 to INT-080). Before that: the data layer connected on the JVM (27 connected, hand-off 7/7). Re-evaluated under §85 after the build; subject to the owner's approval | Instrumentation and Compose UI tests (none exist yet); the app has not been launched on an emulator or a device: DEVICE_VERIFIED = NOT_RUN | `artifacts/evidence/android-ci-build-20260919.txt`, `artifacts/evidence/android-golden-path-20260919.txt` | dc62840 | 2026-09-19 |
| P15 Android Public | CONNECTED_VERIFIED | Home, provinces, taxonomy, directory, search, detail, map, ratings and account bound to the generated client; cache-first flows; opaque-cursor paging; availability and hours from the backend; sign-in, registration and recovery screens | On a real Galaxy A52 (Android 14) against the local stack (approved 2026-09-19): startup; Raqqa and pharmacies only; list and page 2; detail with two-span hours and a MinIO photo; sign-in, session restored after force-stop, sign-out ending the session; location granted (the real device position, nearest-first over both pages equal to the backend) and denied (province discovery, no distance, no prompt loop); POST_NOTIFICATIONS never requested; no crash or ANR. Before that, on the JVM: the connected suite | INT-085 distance format and INT-086 heading; Full Device QA | `artifacts/evidence/android-device-smoke-20260919.txt` | a6f36ac8 (APK) | 2026-09-19 |
| P16 Android Owner | SOURCE_IMPLEMENTED | Owner configuration, drafts, one-field PATCH, location, multi-span and overnight hours, image and evidence upload, closures, duty (shown only when the capability is known), submission, bound to the generated client | Connected on the JVM: onboarding shows exactly the requirements the backend configured (a test requirement in the test database; the launch baseline has none), capabilities read from each owned facility (INT-056), refused selection, draft-to-submit with a real photo and evidence in MinIO, submit refused without evidence, unknown requirement NOT_FOUND; the hand-off to an Admin approval and back to public discovery. INT-068 found and fixed on the way | Android build, Photo Picker and device upload QA; LAUNCH_POLICY_PENDING | `artifacts/evidence/android-golden-path-20260919.txt` | db7ac98 | 2026-09-19 |
| P17 Navigation | SOURCE_IMPLEMENTED | Configurable MapLibre/OSRM/Nominatim provider boundaries, foreground location stream, route/geocode adapters, navigation state engine, off-route/reroute/arrival logic, Arabic TTS, native route rendering and Facility Directions integration | P17 + P14/P15/P16 source qualifiers PASS; `git diff --check` PASS; pure Kotlin navigation smoke PASS. Gradle/device/road unavailable. On the Galaxy A52 (2026-09-19, approved): MapLibre rendering, the initial camera policy, camera restore, facility focus navigation and the owner location picker DEVICE VERIFIED; the whole phase stays SOURCE_IMPLEMENTED | Route drawing, live navigation, voice guidance, rerouting and INT-096 during a navigation, on the device; Configure approved production providers; Gradle build/unit/Compose/instrumentation; physical-device road test for GPS drift, maneuver timing, reroute, lifecycle/screen lock, voice and network loss | `artifacts/evidence/p17-navigation-source-20260917.txt`, `artifacts/evidence/android-device-baseline-cleanup-20260919.txt` | c1c6c68 | 2026-09-19 |
| P18 Mobile Live Data | SOURCE_IMPLEMENTED | Realtime on the backend path `/ws/v1/directory/` (INT-050); invalidation rules as a tested object; events trigger REST refetch only. Push registration bound to `accountPushTokenRegister`/`Unregister` (INT-057); FCM integrated behind build configuration, with nothing committed (DECISION-038) | Connected on the JVM: socket authentication, refusal of a bad or revoked token, reconnect, a public event reaching the app's stream after an owner change; push registration idempotent, and a token stops with its session on sign-out and on revocation from another device | FCM_PROVIDER_DELIVERY = EXTERNAL_NOT_VERIFIED until a Firebase project exists; the Android 13 notification permission request; device lifecycle tests | `artifacts/evidence/android-golden-path-20260919.txt` | db7ac98 | 2026-09-19 |
| P19 Staging | SOURCE_IMPLEMENTED | Production-inheriting staging settings; Render Blueprint for isolated API/worker/PostgreSQL 17/PostGIS/Key Value/web/admin; monorepo Docker sources; provider-hostname staging path; smoke, deploy/rollback/backup/restore/DNS/TLS/monitoring runbooks; PostGIS extension migration | P19 qualifier, backend compileall, shell syntax, secret-marker scan, governance/design regression and whitespace PASS. Render/GitHub account inspection confirmed only V2 resources exist; no V3 repo, so connected runtime was not run | Create/authorize V3 GitHub repository; approve paid staging resources; inject owned S3/OTP/push/monitoring credentials; deploy and run migration/health/E2E/restore gates. Generate dependency lockfiles before release-quality closure | `artifacts/evidence/p19-staging-source-20260917.txt` | 1cc6cf1 | 2026-09-17 |
| P20 Release Quality | SOURCE_IMPLEMENTED | Executable release-quality manifest/orchestrator, source-security gate, strict connected fail-closed mode, staging golden-path runner, bounded load baseline, restore evidence validator, release-blocker ledger; restored missing Auth/Account API surface including OTP/session rotation/recovery/account deletion | Local source gate PASS; Auth source-contract 3/3 PASS; compileall and P14-P19 source regressions PASS; strict connected gate correctly FAILS when runtime/staging/restore inputs are absent | Run Django/PostGIS/Redis/Celery/S3 suites; Admin pnpm/Playwright; Android Gradle/device; connected golden path/load/restore. P20 gate remains open. | `artifacts/evidence/p20-release-quality-source-20260917.txt`, `artifacts/evidence/quality/p20-local.json` | 5e58355 | 2026-09-17 |
| P21 Play RC | SOURCE_IMPLEMENTED | Fail-closed release/signing validator, Play policy baseline, Data Safety inventory, app-content checklist, Arabic listing baseline, account-deletion surface, Internal/Closed testing runbook and RC evidence template | P21 source qualifier 7/7 PASS; P14 Android source regression PASS; governance/design-token drift/whitespace PASS | Signed AAB, Gradle/device/staging qualification, Play Internal/Closed testing, submitted policy forms/assets and production rollout | `artifacts/evidence/p21-play-rc-source-20260917.txt` | 9196f4d | 2026-09-17 |
| P22 Android Production | BLOCKED | Production runbook inputs are represented by P21 source; no store action has been performed | Not run | Requires P20 connected gate, owned upload signing material, Play account access, staging/device evidence and final policy/store assets | `BLOCKERS.md` EXT-002/EXT-003/EXT-004/EXT-005 | — | 2026-09-17 |
| P23 iOS Foundation | NOT_STARTED | — | — | All | — | — | 2026-09-17 |
| P24 iOS Parity | NOT_STARTED | — | — | All | — | — | 2026-09-17 |
| P25 iOS Production | NOT_STARTED | — | — | All | — | — | 2026-09-17 |
| P26 Expansion | NOT_STARTED | — | — | All | — | — | 2026-09-17 |

## Current external blockers

See `BLOCKERS.md`.

## Local execution limitation (not an external blocker)

Superseded on 2026-09-17. The earlier container had no PyPI access and no Docker/PostgreSQL/Redis/MinIO
runtime. The current workstation has Python 3.13, uv, Node 24.17.0, pnpm 10.17.1, Docker 29.5.2,
PostgreSQL 17.5 + PostGIS, Android SDK 36, JDK 25 and Gradle 9.6.0, so backend connected execution is now
possible and was used to qualify FIX-P0.

Two limitations remain, both the same network condition. **Google Maven (`dl.google.com` /
`maven.google.com`) does not serve this machine**, and the Gradle distribution download from
`services.gradle.org` resets mid-transfer, which is why the generated Kotlin client could not be
compiled either even though it needs only Maven Central.
Artifacts that certainly exist, including `androidx.annotation:annotation:1.0.0` and
`com.android.tools.build:gradle:9.4.0`, return HTTP 404 while Maven Central returns 200. Android
dependency resolution therefore cannot complete here and `gradle :app:assembleDebug` stays `NOT_VERIFIED`.
This is a local network limitation, not a project defect and not an external blocker, so the register in
`BLOCKERS.md` is unchanged.

**Re-measured 2026-09-19.** The Gradle distribution now downloads, and Maven Central and the
Gradle Plugin Portal answer; Google Maven still returns 404 from Google's download server. The
local Gradle cache holds Google artifacts fetched on this machine on 2026-09-11 and 2026-09-16,
so the machine does reach Google Maven on some network; the cache lacks the versions this app
pins (Room 2.8.5, DataStore 1.2.1, Navigation 2.10.1, Lifecycle 2.11.0). There is no git
remote, so no CI runner can stand in without the owner creating one. In the meantime
`apps/android/jvm-verification` compiles and tests the platform-free Android code with Maven
Central alone (DECISION-028).

**Diagnosed 2026-09-19** on this network, as the owner asked, without changing any dependency
or Gradle setting: DNS, proxies, hosts and TLS are all clean, and Gradle receives the same
answer curl does. Google's download server answers 404 for Android developer content, both the
Maven repository and the SDK, while serving other files from the same host. Classification:
NETWORK_ENVIRONMENT_FAILURE (`artifacts/evidence/google-maven-diagnosis-20260919.txt`).
Re-checked 01:01 UTC: still 404. `./gradlew :app:assembleLocalDebug` stops at the AGP plugin.
The Android build therefore runs on GitHub Actions in the private `servacode/directory-platform-v3`
repository, where Google Maven answers 200 (`artifacts/evidence/android-ci-build-20260919.txt`).

GDAL is not installed natively on this Windows host, so GeoDjango runs through Docker. That is a host
limitation only.

## Status semantics

Only these states are used: `NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `SOURCE_IMPLEMENTED`, `CONNECTED_VERIFIED`, `DEVICE_VERIFIED`, `STAGING_VERIFIED`, `CLOSED`.

## Complete product batch — 2026-09-24

The app stopped being a redesigned shell and became the product the specification describes.

**Backend.** Saved facilities, an inbox of one's own, a password that can be changed, the
platform's own published pages, and a coordinate resolver that turns a position into the place
this platform calls it — all end to end, with migrations, tests and a regenerated contract. 287
backend tests pass; the OpenAPI document has zero errors and zero warnings and the three
generated clients follow it.

**Android.** Home says where the user is instead of making them choose a province, carries the
advertisement slider the backend was already serving, and answers the three questions people
arrive with through the backend's own query. The map answers the same two questions and carries
the province's real taxonomy down its edge. Screens stop shrinking into squares: one motion
policy for the whole graph. The account area is a product — saved facilities, the inbox, editing,
a password change that says every session will end, settings and the published pages. A facility
can be saved, and the way to it is seen before it is followed.

**Navigation.** RahalGo's driver stack was audited rather than copied
(`docs/design/RAHALGO-NAVIGATION-AUDIT.md`). Two rules it learned on real roads were adapted into
this app's own engine: one reading never decides that the route was left, and a reading's own
accuracy is part of what it means.

**Tooling.** Four qualifier scripts had quietly stopped working — platform-codepage reads, walks
into Gradle's build output, and assertions about pre-P10 placeholders. Repaired and promoted:
all six run as CI gates, and so does the platform-free JVM harness.

CI run 35937791094 is green on every gate: assemble, APK and certificate, 388 Android unit tests,
191 harness tests, lint with zero errors, and six qualifiers. Evidence:
`artifacts/evidence/android-complete-product-20260924.txt`.

Unchanged and still unverified: INT-096 (device), INT-089 (external), INT-084 (open, with the
safe design recorded in `BLOCKERS.md`), FCM provider delivery, live navigation and voice guidance
(road test), and `LAUNCH_POLICY_PENDING`.

## One design system, a smart console, the site, Android phase 3 and push — 2026-09-29

**Design.** One package (`packages/design-tokens`) now carries everything a surface shows:
- colours, with a dark theme;
- the Tajawal face;
- one Arabic word and tone per state;
- 59 icons and 7 illustrations;
- the brand symbol.

The console, the site and the Android app all read it. `docs/design/DESIGN-SYSTEM.md` is the
guide; the console's «نظام التصميم» page is the live reference.

**Admin console.** Eleven sections with tabs:
- Operations: a task centre and alerts on the home page, a top-bar indicator and global
  search, a quality score and timeline per facility.
- Screens: the pharmacy duty roster, content (pages, FAQ, emergency numbers, the contact
  inbox), broadcasts, rejection templates in the reject dialog, province readiness, and
  analytics with periods, team performance and exports.
- The review queue filters by submission day and by missing documents.
- Ads upload their images from the browser and keep their target when edited.

**Public web.** Adds search, the owners' guide, «كيف نتحقق», duty now / today / tomorrow / the
week, published pages and FAQ, emergency numbers, a contact form, sharing, the trust line, and
«افتح في التطبيق» through verified App Links.

**Android.**
- Phase 3: the «المناوب الآن» widget, emergency numbers, App Links on the site's one host,
  optional Sentry, notification choices, recently viewed, data saver and owner tools.
- A Play release configuration that is complete now passes `validatePlayRelease` under the
  configuration cache, and the check requires the Firebase settings.

**Backend.**
- Push reaches phones: an FCM HTTP v1 transport, data-only messages, routing ids checked for
  shape.
- The inbox names the facility of an owner's notice.
- The quality debt is closed (above).

**Still open, and why** (`BLOCKERS.md`, 2026-09-29 review):
- OTP needs a provider chosen before owners can sign in in production.
- GitHub Actions runs no job (EXT-006), so CI evidence is local.
- Device checks are outstanding: the widget, App Link verification, and push delivery and taps.
- The seeded emergency numbers await an operator's confirmation.
