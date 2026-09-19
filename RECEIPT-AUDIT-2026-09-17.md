# Receipt and Verification Audit — Serva Code Directory Platform V3

**Audit date:** 2026-09-17
**Audited HEAD:** `bc12f4df93c8893b2cae47bcd92624809d3e5c4b`
**Branch:** `main`
**Package:** `SERVA-CODE-DIRECTORY-V3-CLAUDE-HANDOFF-2026-09-17.zip`
(SHA-256 `1f9385705d3d5d09da98c5f9ec27b1b8d065a331b213b6a9b1802733f6e2fc8a`)

**Status of this document:** ACCEPTED by the project owner on 2026-09-17 as the new
verification baseline. Phase status in `PROJECT-STATUS.md` is corrected against the
evidence recorded here, not against earlier text-only source qualification.

**Method:** every claim below is backed by an executed command. No gate is marked PASS
from source inspection. Findings that could not be executed are recorded as
`NOT_VERIFIED` with the reason.

---

## 0. Why this audit exists

Prior phase qualification relied on *textual* source checks (for example
`assert "hmac.compare_digest" in services`). Textual checks cannot detect that a server
does not boot, that a migration graph does not load, or that Kotlin does not compile.
This audit executed the real toolchain for the first time and found four P0 defects that
every previous "source qualification" had passed over.

From this point on, a gate is PASS only with a real command exit code.

---

## A. Repository Integrity

| Item | Result |
|---|---|
| Package files vs extracted tree | **582/582 identical byte-for-byte** (`diff -rq`, zero differences) |
| `PACKAGE-SHA256SUMS.txt` | **5/5 OK** |
| Git restore | `git init` + fetch from `directory-platform-v3.git.bundle` + `git reset` (mixed) |
| HEAD | `bc12f4df93c8893b2cae47bcd92624809d3e5c4b` — **matches the expected handoff HEAD** |
| Commits | 26 |
| Tracked files | 578 |
| Uncommitted tracked changes at audit start | **0** |

### History gap

The oldest commit is `ce9bc5c chore: recover latest source snapshot before P9`.
**No commits exist for P0–P8.** `HANDOFF.md § WORKSPACE RECOVERY` acknowledges this.
`origin/main` is recorded at `287accb` (P12); local `main` is 17 commits ahead.

### Dead SHA references in status documents

`871d4c7` · `9c4031c` · `f9408d8` · `0048719` · `11aa673` · `64b6fda` · `4c8f535` ·
`1b7212a` · `ff60f96` — nine cited commits do not exist in the delivered repository.

---

## B. Documentation Integrity

| Item | Result |
|---|---|
| `docs/spec/SHA256SUMS.txt` | **35/35 OK** — specification corpus is intact |
| Specification set | Complete 00→29 plus ALL-IN-ONE, MANIFEST, BASELINE-DECISIONS.json |
| Internal contradictions between specs | **None found** |

### Missing referenced artefacts

| Reference | State |
|---|---|
| `artifacts/evidence/p3-auth-rbac-source-20260917.txt` | missing |
| `artifacts/evidence/p4-taxonomy-source-20260917.txt` | missing |
| `artifacts/evidence/p5-owner-domain-source-20260917.txt` | missing |
| `artifacts/evidence/p13-admin-operations-source-20260917.txt` | missing (recovery file exists) |
| `artifacts/evidence/quality/p20-local.json`, `p20-connected-required.json` | missing — and structurally uncommittable: `.gitignore` excludes `artifacts/evidence/**/*.json` |
| `docs/adr/ADR-001-P3-IDENTITY-SESSION-RBAC.md` | missing, although DECISION-002 cites it |

---

## C. Phase Verification Matrix (as audited, before FIX-P0)

| Phase | Recorded | Audited actual | Evidence |
|---|---|---|---|
| P0 Governance | CLOSED | CLOSED | `node scripts/check-governance.mjs` → PASS |
| P1 Design Tokens | CLOSED | CLOSED | `validate.mjs` → 4 WCAG contrast checks PASS; `generate.mjs --check` → no drift |
| P2 Backend Foundation | IN_PROGRESS | **BLOCKED** | `manage.py check` PASS, but daphne aborts (INT-001) and every HTTP request is 500 (INT-002) |
| P3 Auth/RBAC | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED | rotation logic correct on review; zero DB tests executable |
| P4 Taxonomy | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED (incomplete) | no province/taxonomy seed exists (INT-006) |
| P5 Owner Domain | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED | IDOR covered on 11/11 owner views |
| P6 Availability/Duty | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED (incomplete) | exclusion constraint created on real PostGIS; model missing 3 spec fields (INT-008) |
| P7 Public Discovery | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED | `/public/facilities/` returns correct 400 |
| P8 Realtime | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED | 8/8 spec events defined and emitted; `transaction.on_commit` used |
| P9 Content Services | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED | analytics forbidden-field registry; push tokens encrypted + digested |
| P10 Contracts | IN_PROGRESS | **BLOCKED (structural)** | `spectacular` → 72 paths, **0 schemas, 0 requestBodies**, 328 errors (INT-005) |
| P11 Public Web | SOURCE_IMPLEMENTED | **BUILD VERIFIED** | typecheck + lint + build PASS; 6 routes, all 5 required legal routes prerendered |
| P12 Admin Foundation | SOURCE_IMPLEMENTED | **SCAFFOLD ONLY** | build PASS (18 routes) but `backendRequest` is never called |
| P13 Admin Operations | SOURCE_IMPLEMENTED | Backend yes / UI no | 34 admin views with correct RBAC; every Admin page renders static descriptive text |
| P14–P18 Android | SOURCE_IMPLEMENTED | **BLOCKED — does not compile** | `gradle :app:assembleDebug` → `build-logic:compileKotlin FAILED` (INT-004) |
| P19 Staging | SOURCE_IMPLEMENTED | BLOCKED | `qualify-staging-source.py` PASS, but deploy would fail on INT-001/INT-003 |
| P20 Release Quality | SOURCE_IMPLEMENTED | FAIL | `release_quality.py` → `{"overall":"FAIL",...}` (false positives, INT-014) |
| P21 Play RC | SOURCE_IMPLEMENTED | SOURCE_IMPLEMENTED | qualifier 7/7 PASS; `validatePlayRelease` is correctly fail-closed |
| P22 Android Production | BLOCKED | BLOCKED | correct |
| P23–P26 | NOT_STARTED | NOT_STARTED | `apps/ios/` contains only `.env.example` and `.gitkeep` |

---

## D. Backend Audit

### Audit environment

Python 3.13.14 · Django 5.2.17 · uv 0.11.26 · Node 24.17.0 · pnpm 10.17.1 ·
Docker 29.5.2 · PostgreSQL 17.5 + PostGIS · Android SDK + JDK 25 + Gradle 9.6.0.

This is materially stronger than the environment described in `HANDOFF.md`, which is why
gates previously recorded as "environment limitation" became executable — and failed.

### Verified correct

- `uv sync --dev` succeeded and produced `uv.lock` (resolves a documented blocker).
- `manage.py check` → `System check identified no issues (0 silenced)`.
- **`migrate` against real PostGIS 17.5 applies all 20 migrations**, including
  `CreateExtension('postgis')`, `BtreeGistExtension`, and the duty `ExclusionConstraint`
  using `TSTZRANGE ... OVERLAPS` — after the single-line INT-003 fix.
- Refresh rotation: `transaction.atomic` + `select_for_update()` +
  `hmac.compare_digest` + previous-digest grace window + revocation of **all** user
  sessions on post-grace reuse.
- Admin RBAC is fail-closed: `HasAdminPermission` denies when a view declares no
  permission code; `is_superuser` grants nothing; all 34 views covered; every mutating
  handler re-checks a `.manage` permission.
- Owner IDOR: 11/11 facility-scoped views call `require_facility_member` /
  `require_facility_owner`; `business_hours` and `pharmacy_duty` call
  `require_facility_manager`.
- Upload security: PIL decode, dimension and pixel limits enforced **before**
  `image.load()`, JPEG re-encode, metadata stripped, random `uuid4` storage key.
- Evidence privacy: permission-gated, audited, `Cache-Control: private, no-store`,
  `X-Content-Type-Options: nosniff`.
- Realtime: envelope matches the specification exactly; authentication happens after
  connect (no token in URL); all 8 catalogued events have emission sites.
- Production settings fail closed with 12 explicit validations.
- Analytics privacy: `FORBIDDEN_FIELD_NAMES` blocks latitude/longitude/phone/token.
- Push tokens stored as ciphertext plus digest, never raw.

### Code quality

| Tool | Result |
|---|---|
| `ruff check .` | **106 errors** (50 of them in `directory/models.py`, longest line 249 chars against a 100 limit) |
| `mypy .` (strict) | **556 errors in 82 files** |

---

## E. Admin Audit

`typecheck`, `lint` and `build` all PASS; 18 routes, covering all 17 routes required by
`09-ADMIN-NEXTJS.md`. Security headers present (CSP, nosniff, Referrer-Policy,
X-Frame-Options); HSTS absent. `server-only` boundaries and a central `can(permission)`
exist, and no independent DTOs are hand-authored.

However `backendRequest`, `setRefreshCookie` and `getRefreshCookie` are **never called**.
Every page renders `<OperationPage>`, which displays the route title, description,
permission code and endpoint path as static text. None of the operational components
required by `10-DESIGN-SYSTEM-UX.md` (DataTable, FilterBar, DiffViewer, AuditTimeline,
ConfirmDialog, FormSection, Pagination) exist. Playwright and Vitest are neither
installed nor declared.

---

## F. Android Audit

27 modules, exactly matching the list in `11-ANDROID-KOTLIN.md`. No React Native or
Flutter traces anywhere in 578 tracked files. MapLibre native, no WebView map, no
`ACCESS_BACKGROUND_LOCATION`. `validatePlayRelease` is a well-designed fail-closed task.

**The build fails.** `gradle :app:assembleDebug` stops at `build-logic:compileKotlin`.
Root cause verified against the locally resolved AGP jar: AGP 9.4.0 declares
`public interface CommonExtension extends ExtensionAware` with **no type parameters**,
while the convention plugin uses the AGP 8.x form `CommonExtension<*, *, *, *, *, *>`.

Consequence: 27 modules and 127 Kotlin files have never been compiled. Additionally every
API boundary is bound to `UnboundGeneratedPublicApi` / `UnboundGeneratedOwnerApi` /
`UnboundPushRegistrationBoundary`, each of which throws
`GeneratedClientRequiredException`.

---

## G. iOS Audit

`apps/ios/` contains `.env.example` and `.gitkeep` only. `NOT_STARTED` is accurate.

---

## H. Security Audit

578 tracked files scanned against 17 credential patterns.
**Zero production secrets, zero private keys, zero real credentials.**
All 18 pattern matches are legitimate: placeholder `.env.example` files, explicit local
development values in `compose.yml`, 15 `sync: false` entries in `render.yaml`, and the
scanner scripts themselves. No secret value is reproduced in this report.

Threat-model coverage from `15-SECURITY-PRIVACY.md` is strong on passwords, OTP,
sessions, IDOR, admin escalation, evidence leakage, upload attacks, location privacy and
log hygiene. Gaps found: no `DEFAULT_PERMISSION_CLASSES` (INT-010), publicly reachable
`/api/schema/` (INT-016), Admin origin check bypassable when the `Origin` header is
absent, dual authorization surface from `PermissionsMixin`, missing HSTS, and no
dependency or secret scanning in CI.

---

## I. Test Audit

| Invocation | Result |
|---|---|
| `uv run pytest` (as CI runs it) | **collection fails entirely — 0 tests run**; `import file mismatch` on duplicate `test_source_contract` module names |
| `pytest --import-mode=importlib` on SQLite | 30 passed / 14 errors (all `NodeNotFoundError`) |
| `pytest` on real PostGIS 17.5, INT-003 patched | **38 passed / 6 failed** (the 6 are caused by INT-002) |

44 test functions exist in total; 18 of them (41%) assert on source text rather than
behaviour. Of the 17 critical backend cases required by
`infrastructure/quality/manifest.json`, 7 have any coverage and several of those are
textual or partial. Missing entirely: phone-normalization, otp, sessions,
refresh-concurrency, lifecycle, postgis-nearest, admin-concurrency, account-deletion.

### Not verifiable in this environment

| Gate | Reason |
|---|---|
| Admin Playwright E2E, Admin/Web Vitest | `NOT_VERIFIED — tooling absent from the project` |
| Android unit / Compose / instrumentation | `NOT_VERIFIED — build fails at build-logic (INT-004)` |
| Physical device QA | `NOT_VERIFIED — no APK can be produced` |
| Connected Celery / FCM / S3 | `NOT_VERIFIED — provider credentials absent (EXT-002)` |
| Staging runtime, load baseline, restore drill | `NOT_VERIFIED — EXT-004/EXT-005 plus INT-001/INT-003` |
| Native GeoDjango on Windows | `NOT_VERIFIED — GDAL absent; worked around with Docker` |

---

## J. Infrastructure Audit

`render.yaml` defines a correct isolated staging topology (Key Value with `noeviction`
and `journal-snapshot`, API, worker, www, admin, PostgreSQL 17, Frankfurt, 15 secrets as
`sync: false`). `compose.yml` provides PostGIS 17-3.5, Redis, MinIO with health checks.
Six runbooks exist.

Defects: the backend `Dockerfile` copies only `pyproject.toml` and runs `uv sync` with no
lockfile, so image builds are not reproducible; `preDeployCommand` runs `migrate`, which
fails on INT-003; `healthCheckPath: /health/ready/` can never answer while INT-001 and
INT-002 stand. CI would fail four of six steps in `backend-source`, the `contract-drift`
job is a no-op (it diffs an untracked file), and there are no Android or Admin/Web jobs.

---

## K. Google Play Audit

`targetSdk = 36` matches the policy baseline. Policy materials, Data Safety inventory,
app-content checklist, closed-testing runbook and store-listing scaffolding are present,
and `validatePlayRelease` is bound to `bundleRelease`/`assembleRelease` and fails closed
on placeholders, non-HTTPS endpoints, localhost and missing signing material. A signed
AAB is impossible while INT-004 stands. `P21 PLAY RC PASS` is not achieved.

---

## L. Legacy / Dead / Duplicate Files

The tree is exceptionally clean: zero React Native or Flutter remnants, zero
TODO/FIXME/XXX/HACK markers in source, zero duplicate migrations, no stale prototypes.
All empty tracked files are legitimate `__init__.py` and `.gitkeep` markers.

Cleanup candidates, none removed: Gradle build output under `apps/android`
(`.kotlin/` is not covered by `.gitignore`), the handoff bundle and package metadata files
once no longer needed, and three small dead imports flagged by ruff.

---

## M. Blockers

### External (unchanged, correctly recorded in `BLOCKERS.md`)

EXT-001 root domain · EXT-002 provider credentials · EXT-003 store and signing access ·
EXT-004 missing V3 GitHub remote · EXT-005 paid staging approval.

None of these block FIX-P0, P2 connected qualification, P10, the P4 seed, or local
Android builds.

### Internal bugs

| ID | Sev | Introduced in | Defect |
|---|---|---|---|
| INT-001 | P0 | P2 | ASGI does not start — routing imported before `get_asgi_application()` |
| INT-002 | P0 | P2 | every HTTP request returns 500 — `MessageMiddleware` without session middleware |
| INT-003 | P0 | P3 | migration graph invalid — dependency uses module name `sessions` instead of app label `directory_sessions` |
| INT-004 | P0 | P14 | Android does not compile — AGP 8.x `CommonExtension` signature under AGP 9.4 |
| INT-005 | P1 | P10 | generated OpenAPI carries 0 schemas and 0 request bodies |
| INT-006 | P1 | P4 | no province or taxonomy seed exists |
| INT-007 | P1 | P2 | `settings/test.py` uses SQLite while models are spatial |
| INT-008 | P1 | P6 | `DutyShift` missing `created_by`, `cancelled_at`, `ended_early_at`; hard delete; constraint has no cancelled condition |
| INT-009 | P1 | P3/P6/P9 | migration drift in `accounts`, `notifications`, `pharmacy_duty` |
| INT-010 | P2 | P5 | `/owner/facilities/` unauthenticated returns 500 (no `DEFAULT_PERMISSION_CLASSES`) |
| INT-011 | P2 | P12/P13 | Admin UI is a descriptive scaffold with no data binding |
| INT-013 | P2 | P14 | all Android API boundaries throw `GeneratedClientRequiredException` |
| INT-014 | P3 | P14–P20 | qualifier scripts use `read_text()` without encoding and scan `node_modules`/`build` |
| INT-015 | P2 | P13 | 8 unguarded `objects.get` in `admin_console/views.py` return 500 instead of 404 |
| INT-016 | P2 | P2 | `/api/schema/` publicly reachable in all environments |
| INT-017 | P2 | P3/P5 | `PUT/DELETE /account/profile-image/` not implemented |
| INT-018 | P2 | P13 | Admin taxonomy is read-only; `Cycle J` cannot be executed |
| INT-019 | P2 | P14 | no Gradle wrapper, contrary to `26-REPOSITORY-STRUCTURE.md` |
| INT-020 | P3 | P0 | CI contract-drift gate is a no-op; no Android or Admin/Web jobs |
| INT-021 | P3 | P0 | no lockfiles committed; Dockerfile does not copy a lockfile |
| INT-022 | P3 | P11/P12 | Tailwind, Radix, Vitest and Playwright absent despite `02-BASELINE-DECISIONS.md` |
| INT-023 | P3 | P11 | `apps/web/tsconfig.json` uses `jsx: "preserve"`, rewritten on every Next 16 build |
| INT-024 | P3 | P11/P12 | HSTS header missing |
| INT-025 | P3 | P4 | `directory/models.py` written in a compressed style violating `.editorconfig` and ruff |
| INT-026 | P3 | P3 | missing indexes on `User.status`, `UserSession(user, revoked)`, `last_seen_at` |
| INT-027 | P3 | P3 | non-UUID refresh token yields 500 instead of 401 |

### Environment limitations (not project defects)

GDAL absent natively on Windows (worked around with Docker); DNS unavailable inside
Docker BuildKit (worked around with `--network=host`); no emulator or attached device.

---

## N. Documentation Mismatches

1. Nine commit SHAs cited as evidence do not exist.
2. P3 claims "12 tests authored"; `accounts/tests/` contains 3 textual assertions.
3. P5 claims "12 P5 tests authored"; `facilities/tests/` contains 4.
4. P13 claims "11 executable source-contract tests passed"; `uv run pytest` does not collect.
5. P4 claims a 14-province seed and the Raqqa/Pharmacy/Duty launch baseline; **no seed exists**.
6. P4/P5 claim "line length passed"; 14 real E501 violations exist, up to 249 characters.
7. P2/P3 claim static checks passed; `ruff` and `mypy` were never included.
8. P20 evidence JSON files are missing and cannot be committed under current `.gitignore`.
9. Four phase evidence files are missing.
10. DECISION-002 cites `ADR-001`, which does not exist.
11. DECISION-003 refers to `User.profile_province`; the field is `province`.
12. DECISION-004 defines permission codes that do not match the implemented `admin.*` codes.
13. DECISION-002 requires nullable refresh material on concurrency grace; the implementation always issues a new token.
14. P13 claims taxonomy mutations; categories and groups are read-only.
15. P12/P13 status conflates the implemented Admin backend API with a non-existent Admin UI.
16. `HANDOFF.md` describes an environment without pnpm, Gradle, Android SDK, Docker or PostgreSQL; all are present.
17. `HANDOFF.md` states `uv.lock` cannot be generated; it was generated successfully.
18. `HANDOFF.md` states Node is 22.16.0; Node 24.17.0 is available.
19. P19 attributes the open gate to external blockers only; deployment would also fail on INT-001 and INT-003.
20. P8 status understates the implementation — duty and province hooks also exist.

**Assessment of documentation honesty:** the separation between `SOURCE_IMPLEMENTED` and
connected verification is methodical and was consistently respected. The defect is not
integrity but method: source qualification never invoked a compiler, a migration loader
or a server, so four P0 defects passed every gate.

---

## O. Recommended Corrections

### Batch 1 — unblock (P0)

1. Move `get_asgi_application()` before any routing import — `directory_backend/asgi.py`.
2. Remove `django.contrib.messages` and `MessageMiddleware` — `settings/base.py`.
3. Correct the migration dependency app label — `sessions/migrations/0002_rotation_security_fields.py`.
4. Correct the `CommonExtension` usage for AGP 9 — `build-logic/src/main/kotlin/serva.android.compose.gradle.kts`.

Fixes 1–3 were verified experimentally to restore: full `migrate` on PostGIS 17.5,
`/health/live/` → 200, `/api/v1/public/provinces/` → 200,
`/api/v1/public/facilities/` → 400 (correct), `/api/v1/admin/dashboard/` → 403
(correct), and 38 of 44 tests passing.

### Batch 2 — make the project verifiable (P1)

5. Point test settings at PostGIS instead of SQLite.
6. Add the three missing `tests/__init__.py` files and give test modules unique names.
7. Generate the missing migrations so `makemigrations --check` succeeds.
8. Add a Gradle wrapper to `apps/android`.
9. Commit `uv.lock` and `pnpm-lock.yaml`; make the Dockerfile copy the lockfile and use `--frozen`.
10. Fix qualifier script exclusions and use `read_text(encoding="utf-8")`.

### Batch 3 — complete P10 (P1, the heaviest and most consequential)

11. Add serializers or `@extend_schema` to all 72 endpoints.
12. Register an `OpenApiAuthenticationExtension` for `BearerAccessTokenAuthentication`.
13. Resolve the five operationId collisions.
14. Commit `openapi/schema.yaml` and `schema.sha256`; make the CI drift gate able to fail.
15. Generate the TS, Kotlin and Swift clients with the pinned generator.

### Batch 4 — functional gaps (P1/P2)

16. Seed the 14 provinces, Health/Pharmacy taxonomy, Raqqa activation and duty.
17. Restore the three `DutyShift` fields, add a constraint condition, replace hard delete with soft cancel, add audit.
18. Set `DEFAULT_PERMISSION_CLASSES` and mark public endpoints `AllowAny` explicitly.
19. Restrict `/api/schema/` by environment or permission.
20. Implement `PUT/DELETE /account/profile-image/`.
21. Add category and group create/update operations to the Admin API.
22. Replace the eight unguarded `objects.get` calls with `get_object_or_404`.
23. Always set `secure` on the `__Host-` cookie.

### Batch 5 — quality and governance (P2/P3)

24. Clean `directory/models.py` and drive ruff to zero.
25. Plan mypy remediation or relax `strict` through a recorded ADR.
26. Add Android and Admin/Web CI jobs, Playwright, and secret/dependency scanning.
27. Install Tailwind, Radix, Vitest and Playwright, or record an ADR departing from the baseline.
28. Write the missing ADR-001 and reconcile DECISION-003 and DECISION-004 with the code.
29. Correct `PROJECT-STATUS.md`: remove dead SHAs, correct test counts, correct the seed claim.
30. Allow quality evidence JSON in `.gitignore` and add `.kotlin/`.

---

## P. Resume Point at the time of this audit

**Last genuinely closed phase:** P1 — Design System Foundations, the only phase whose
closure was proven by execution.

**Real current state:** P2 is reopened as `BLOCKED` on internal defects; everything above
it is suspended behind it.

**First task:** `FIX-P0` — the four unblocking fixes. No external input, architectural
decision or budget approval is required for any of them.

**Gate to target:** `P2 BACKEND FOUNDATION CONNECTED PASS`, with execution-based
acceptance criteria only.

### Correction to the recorded resume point

`plan.md` and `HANDOFF.md` nominate **P23 iOS Foundation** as the next independent phase
while simultaneously requiring that API integration stay behind the P10 generated-client
boundary. These two instructions are incompatible in the current state: the generated
schema carries no models, and the Android client boundaries throw on every call.

Starting P23 now would add a third client built against a contract that does not exist.
**P10 must precede P23.**

---

*Audit performed with execution evidence only. No project file was modified during the
audit; `git status` reported zero tracked changes and HEAD was unchanged at completion.*


---

# Canonical Defect Register

**Added:** 2026-09-18. This section supersedes the scattered identifier usage in earlier
reports. Nothing above is rewritten; the corrections are stated here explicitly.

## Correction notice

Three identifier faults were found while reviewing the P10 report and are corrected here.

1. **`INT-035` and `INT-036` were used for two different things.** The P10 error-contract
   narrative used them for the *error envelope* divergence, while the bug list used them
   for *pagination* and *Admin casing*. The canonical meanings are the bug-list ones, kept
   unchanged so nothing has to be renumbered, and the error envelope — which never had an
   identifier of its own — is now **`INT-038`**.
2. **`INT-012` was referenced but never registered.** `HANDOFF.md` and `plan.md` cite it
   for the Admin `__Host-` refresh cookie, but the audit register skipped from `INT-011`
   to `INT-013`. It is defined below.
3. **`INT-032` was never assigned.** It appeared once in conversation and in no committed
   document. It is retired permanently and must not be reused, so the record of what was
   said stays traceable.

From this point every identifier below is canonical and stable.

## Register

Status values: `OPEN`, `FIXED (<batch>)`, `RETIRED`.

| ID | Sev | Introduced | Defect | Status |
|---|---|---|---|---|
| INT-001 | P0 | P2 | ASGI does not start; routing imported before `get_asgi_application()` | FIXED (FIX-P0) |
| INT-002 | P0 | P2 | every HTTP request returns 500; `MessageMiddleware` without session middleware | FIXED (FIX-P0) |
| INT-003 | P0 | P3 | migration graph invalid; dependency uses the module name `sessions` instead of the app label | FIXED (FIX-P0) |
| INT-004 | P0 | P14 | Android does not compile; AGP 8 `CommonExtension` signature under AGP 9.4 | FIXED (FIX-P0) |
| INT-005 | P1 | P10 | generated OpenAPI carries no component schemas and no request bodies | FIXED (P10) |
| INT-006 | P1 | P4 | no province or taxonomy seed exists | FIXED (LAUNCH-BASELINE) |
| INT-007 | P1 | P2 | `settings/test.py` uses SQLite while the models are spatial | FIXED (P2) |
| INT-008 | P1 | P6 | `DutyShift` missing `created_by`, `cancelled_at`, `ended_early_at`; hard delete; constraint has no cancelled condition | OPEN |
| INT-009 | P1 | P3/P6/P9 | migration drift across seven apps | FIXED (P2) |
| INT-010 | P2 | P5 | owner, media and rating views declare no `permission_classes`; anonymous caller receives 500 | FIXED (P10) |
| INT-011 | P2 | P12/P13 | Admin UI is a descriptive scaffold with no data binding | OPEN |
| INT-012 | P2 | P12 | the Admin `__Host-` refresh cookie sets `secure` only when `NODE_ENV=production`, and browsers reject a `__Host-` cookie without `Secure`, so the development login flow cannot work | FIXED (ADMIN-BFF) |
| INT-013 | P2 | P14 | every Android API boundary throws `GeneratedClientRequiredException` | FIXED (ANDROID-BINDING) — public, owner and auth boundaries bound to the generated client; push registration has no operation to bind to (INT-057) |
| INT-014 | P3 | P14–P20 | qualifier scripts use `read_text()` without an encoding and scan `node_modules` and `build` | OPEN |
| INT-015 | P2 | P13 | eight unguarded `objects.get` calls in `admin_console/views.py` return 500 instead of 404 | FIXED (ADMIN-BINDING) — nine lookups and two service paths; the contract said 404 and now the runtime does |
| INT-016 | P2 | P2 | `/api/schema/` publicly reachable in every environment | FIXED (P10) |
| INT-017 | P2 | P3/P5 | `PUT`/`DELETE /account/profile-image/` not implemented | OPEN |
| INT-018 | P2 | P13 | Admin taxonomy is read-only, so `Cycle J` cannot be executed | FIXED (ADMIN-BINDING) — group and category create/update, requirement update, advertisement update; no delete, by design |
| INT-019 | P2 | P14 | no Gradle wrapper, contrary to `26-REPOSITORY-STRUCTURE.md` | FIXED (ANDROID-GOLDEN-PATH) — the official Gradle 9.6.0 wrapper; jar and distribution checksums match gradle.org |
| INT-020 | P3 | P0 | CI contract-drift gate is a no-op; no Android or Admin/Web jobs | FIXED (P10) |
| INT-021 | P3 | P0 | no lockfiles committed; Dockerfile does not copy a lockfile | PARTIAL — lockfiles committed in P2; the Dockerfile still resolves at build time |
| INT-022 | P3 | P11/P12 | Tailwind, Radix, Vitest and Playwright absent despite `02-BASELINE-DECISIONS.md` | PARTIAL — Vitest and Playwright installed and running in `apps/admin`; Tailwind and Radix deliberately not adopted, the console uses the generated design tokens directly |
| INT-023 | P3 | P11 | `apps/web/tsconfig.json` uses `jsx: "preserve"`, rewritten on every Next 16 build | PARTIAL — `apps/admin` now commits the form Next 16 writes, so its build no longer dirties the tree; `apps/web` is unchanged |
| INT-024 | P3 | P11/P12 | HSTS header missing | PARTIAL — the Admin sends HSTS on https deployments; `apps/web` does not yet |
| INT-025 | P3 | P4 | `directory/models.py` written in a compressed style violating `.editorconfig` and ruff | OPEN |
| INT-026 | P3 | P3 | missing indexes on user status, `UserSession(user, revoked)` and `last_seen_at` | OPEN |
| INT-027 | P3 | P3 | a non-UUID refresh token yields 500 instead of 401 | OPEN |
| INT-028 | P1 | P3/P13 | pytest collection aborts; three `tests/` packages lack `__init__.py` and two module basenames repeat | FIXED (P2) |
| INT-029 | P2 | P8 | a Redis connection error propagates after the transaction has committed; `publish_after_commit` guards `channel_layer is None` but not a connection failure | OPEN |
| INT-030 | P1 | P5 | `submit_facility` applies `FOR UPDATE` across a LEFT OUTER JOIN, which PostgreSQL refuses | FIXED (P2) |
| INT-031 | P1 | P3 | `Province` filtered on `is_active` at three sites; the field is `active` | FIXED (P10) |
| INT-032 | — | — | never assigned; retired permanently and must not be reused | RETIRED |
| INT-033 | P0 | P3 | registration completion, login and password reset return 500; views pass `**validated_data` into services while the serializers declare camelCase fields with no `source` | FIXED (P10) |
| INT-034 | P1 | P5 | `facility_detail` orders business hours by `sequence`, a field that does not exist on the model | FIXED (P10) |
| INT-035 | P2 | P7 | cursor pagination emits `{next, previous, results}`; `08-API-CONTRACT.md` specifies `{items, nextCursor, hasMore}` | FIXED (CONTRACT-ALIGNMENT) |
| INT-036 | P2 | P13 | several Admin list endpoints answer from `QuerySet.values()` and return snake_case keys; `08-API-CONTRACT.md` requires one convention at the boundary | FIXED (CONTRACT-ALIGNMENT) |
| INT-037 | P3 | P6 | `06-DATA-MODEL.md` names the business-hour ordering field `sequence`; the model and the wire contract use `sort_order` | FIXED (CONTRACT-ALIGNMENT) — wire name only; the column keeps `sort_order` |
| INT-038 | P2 | P2/P13 | the runtime emits three unrelated error shapes; `08-API-CONTRACT.md` specifies a single envelope carrying `code`, `message`, `details` and `requestId` | FIXED (CONTRACT-ALIGNMENT) |
| INT-039 | P3 | P13 | `AdminCapabilitiesRequestSerializer` and `AdminCapabilitiesSerializer` declare `supports_*` on the wire, the last snake_case pair left in the Admin API; it is not a list endpoint, so it was outside the CONTRACT-ALIGNMENT scope | FIXED (ADMIN-BINDING) |
| INT-040 | P2 | P4 | `Category.Specialization` declared GENERIC, PHARMACY, DOCTOR, NURSING and MEDICAL_SUPPLIES; `01-MASTER-SPECIFICATION.md` section 5 names GENERIC, PHARMACY, MEDICAL_CLINIC and NURSING_CENTER. Found while seeding, because the baseline could not be expressed in the declared values | FIXED (LAUNCH-BASELINE) |
| INT-041 | P2 | P13 | four Admin list views filter on 14 query parameters the contract declares nowhere, so the generated clients cannot express a search box or a filter | FIXED (ADMIN-BFF) |
| INT-042 | P2 | P13 | no endpoint returns the caller's own Admin permissions. `accountProfileRetrieve` omits them, and reading them through `adminUserRetrieve` plus `adminRolesList` requires `admin.users.read` and `admin.roles.read`, which most operators will not hold. Permission-aware navigation cannot be built without either a new endpoint or probing every route for a 403 | FIXED (ADMIN-BINDING) — `GET /api/v1/admin/me/` |
| INT-043 | P2 | P13 | `AdminVerificationRequirement.id` declared as a UUID while the model key is a `BigAutoField`; DRF's `UUIDField` stringifies without validating on output, so the contract claimed `format: uuid` for a field returning `"2"` | FIXED (ADMIN-BINDING) |
| INT-044 | P2 | P13 | the advertisement create contract omitted `startsAt` and `endsAt`, so a generated client stripped the schedule and every advertisement was created unscheduled; the end-before-start rule could never fire on creation | FIXED (ADMIN-BINDING) |
| INT-045 | P1 | P11/P12 | the Admin CSP was a static `script-src 'self'`, which blocks the App Router's inline RSC scripts; the console rendered and never hydrated, so every control was inert. Invisible to HTTP-level checks, found by the first run in a real browser | FIXED (ADMIN-BINDING) — per-request nonce with `'strict-dynamic'` in `proxy.ts` |
| INT-046 | P1 | P12 | the login form had no method, so a submit before hydration went out as GET with the phone number and password in the URL; reproduced under load by Playwright | FIXED (ADMIN-BINDING) — `method="post"` and the submit button disabled until hydration |
| INT-047 | P3 | P12 | the login form printed the backend message directly instead of going through the central error mapper | FIXED (ADMIN-BINDING) |
| INT-048 | P1 | P10 | the evidence and image upload parts were described as `format: uri`, so the Kotlin and Swift clients typed them as a URI and sent its text; no mobile client could upload a file | FIXED (ANDROID-BINDING) — binary upload field; contract test over every multipart request |
| INT-049 | P3 | P10 | duty shift and temporary closure creation reused their response component, whose read-only `id` is required, so the Kotlin client could not build a request without inventing an id | FIXED (ANDROID-BINDING) — input serializers; contract test over every request body |
| INT-050 | P2 | P18 | the Android realtime default URL was `/ws/events/`; the backend serves `/ws/v1/directory/` | FIXED (ANDROID-BINDING) |
| INT-051 | P1 | P3 | access tokens carried only the user: logging out or revoking a session left its access token working for up to fifteen minutes, and a WebSocket authenticated with it stayed authenticated | FIXED (ANDROID-BINDING) — `sid` claim resolved against the live session on REST and WebSocket; open sockets closed before a private event reaches a dead session |
| INT-052 | P1 | P10 | the Kotlin client did not compile: nullable choice fields were described as `oneOf: [<Enum>, NullEnum]` and the generator rendered `NullEnum` as an enum with no entries. P10 generated the client but never compiled it | FIXED (ANDROID-BINDING) — `ENUM_ADD_EXPLICIT_BLANK_NULL_CHOICE = False`; contract test refuses an enum without a value |
| INT-053 | P1 | P10 | free-form JSON fields were generated as `kotlin.Any`, which kotlinx.serialization cannot serialize; the Kotlin compiler crashed in its back end | FIXED (ANDROID-BINDING) — Kotlin type mapping to `JsonElement` |
| INT-054 | P1 | P3 | a replayed refresh secret was refused, but `rotate_refresh` raised inside its atomic block, so the compromise marking and the revocation of every session were rolled back with it | FIXED (ANDROID-BINDING) — the replay is recorded in a transaction that commits before the refusal |
| INT-055 | P2 | P14 | thirteen feature modules imported `hiltViewModel`, four imported `toRoute`, and the app imported MapLibre, without declaring the dependencies; the first real compile would have failed | FIXED (ANDROID-BINDING) — dependencies declared; not yet proven by an Android compile |
| INT-056 | P3 | P5 | owner facility responses do not carry their category's capabilities, so the app cannot tell from the facility whether it supports duty or closures | FIXED (ANDROID-GOLDEN-PATH) — summaries and details carry the eight flags from one presenter; the app reads them from the facility |
| INT-057 | P2 | P18 | the contract has no push-registration operation and the Android app has no FCM integration, so the push boundary cannot be bound | FIXED (ANDROID-GOLDEN-PATH) — `accountPushTokenRegister`/`Unregister`, tokens tied to the session; the app is bound and FCM is integrated behind build configuration. Delivery through FCM is EXTERNAL_NOT_VERIFIED: no Firebase project is configured |
| INT-058 | P1 | P7 | the second page of any nearest-first facility list answered 500: the cursor stored GeoDjango's `Distance` measure as text (`"123.4 m"`) and the next query could not compare it | FIXED (ANDROID-BINDING) — ordering and cursor on a float distance in metres |
| INT-059 | P1 | P7 | public facility detail served opening hours without the `id` and `sequence` the contract requires, so a generated client could not decode any facility that had hours | FIXED (ANDROID-BINDING) |
| INT-060 | P2 | P7 | every public facility image pointed at `/api/v1/public/media/images/<id>/`, a route that does not exist | FIXED (ANDROID-BINDING) — the storage URL, as the owner endpoint serves it |
| INT-061 | P3 | P7 | public image URLs are pre-signed and expire, so an image a cache-first screen stored goes dark after the signature lapses | FIXED (ANDROID-GOLDEN-PATH) — public media is addressed from `S3_PUBLIC_MEDIA_BASE_URL`, unsigned; read anonymously from MinIO in the golden path |
| INT-062 | P2 | P10 | the generated Kotlin serializer uses `encodeDefaults = true`, so a PATCH naming one field sent every other optional field as null, which the backend refuses or applies as a clear | FIXED (ANDROID-BINDING) — the app configures `encodeDefaults = false` and `explicitNulls = false` before first use |
| INT-063 | P2 | P14 | the Android session coordinator cleared the session on any refresh failure, so being offline at the moment a token expired signed the user out | FIXED (ANDROID-BINDING) — only a refused secret ends the session |
| INT-064 | P3 | P14 | the Android refresh authenticator reported the token in the store, not the one the failed request carried, as the failed token, so a request that failed on the old token after another had refreshed spent the secret again | FIXED (ANDROID-BINDING) |
| INT-065 | P2 | P9 | public advertisement images pointed at a route that does not exist | FIXED (ANDROID-GOLDEN-PATH) — the public media address, as facility photos use |
| INT-066 | P3 | P13 | verification evidence was streamed as `application/octet-stream`, so the operator's browser was told nothing about what it received | FIXED (ANDROID-GOLDEN-PATH) — typed from the stored name; evidence is always re-encoded to JPEG |
| INT-067 | P2 | P13 | the evidence response named its download after the stream, and an S3 file is named by its object key, so the key's file name left the backend in `Content-Disposition`. The Admin BFF replaces that header, so no browser received it | FIXED (ANDROID-GOLDEN-PATH) — a neutral name from the evidence id |
| INT-068 | P1 | P5 | a verification requirement's id was declared a UUID in the owner and review contracts while the model keys it with an integer — INT-043 fixed the Admin component alone. The launch baseline configures no requirement, so nothing noticed: with the first one configured, the Android client could not decode the owner configuration and the upload serializer refused every id an owner could send, so no evidence could be uploaded. Found by the first hand-off run | FIXED (ANDROID-GOLDEN-PATH) — integer on the wire and in the contract; test over every `requirementId` component |
| INT-069 | P3 | P15–P18 | the public, owner, navigation and live-data source qualifiers in `apps/android/scripts` still assert the pre-binding stubs — `GeneratedClientRequiredException`, `PublicCacheDataSource`, `HourDraft`, an inline user-scope check — that INT-013 and the Android binding replaced by design, so they fail on correct source | OPEN — kept out of CI rather than run and ignored; rewrite or retire them. Text qualifiers do not qualify a gate on their own |
| INT-070 | P1 | P14 | the application and library conventions looked the version catalog up inside `dependencies { }`, where `extensions` is the dependency handler's and holds only `ExtraPropertiesExtension`, so no Android module could be configured. Added with desugaring (DECISION-033) and never compiled; found by the first real Android build (CI run 35425273117) | FIXED (ANDROID-CI) — looked up on the project |
| INT-071 | P2 | P14 | `distinctUntilChanged()` applied to a `StateFlow` in `AccountViewModel` and `PushSetup`; kotlinx.coroutines deprecates it at error level because a StateFlow already emits only changes, so neither compiled. Android-only sources the JVM harness does not compile; found by the first real Android build (CI run 35425452454) | FIXED (ANDROID-CI) — operator removed; behaviour unchanged |
| INT-072 | P1 | P15 | `AccountViewModel.deleteAccount()` called `deleteAccount()`, meant for the injected use case of that name; inside the function it resolves to the function itself, which returns `Unit`, so the module did not compile — and had it compiled as a recursion, account deletion would never have reached the backend. Found by the first real Android build (CI run 35425452454) | FIXED (ANDROID-CI) — the use case is named `accountDeletion` |
| INT-073 | P1 | P14 | the project compiled against API 36 while 21 pinned libraries (Navigation 2.10.1, Compose 1.12, Lifecycle 2.11.0, Core 1.19.0, Coil 3.6.2) require API 37, so `checkLocalDebugAarMetadata` refused the app. Found by the first real Android build (CI run 35425779550) | FIXED (ANDROID-CI) — compileSdk 37, targetSdk 36 unchanged (DECISION-040) |
| INT-074 | P2 | P15 | `feature:home` imports `androidx.activity.compose` for the location permission request without declaring it, the same omission INT-055 fixed in other modules. Found by the first real Android build (CI run 35425779550) | FIXED (ANDROID-CI) — declared |
| INT-075 | P2 | P17 | `NavigationViewModel.state`, public, exposed the internal `NavigationUiState`, which Kotlin refuses. Found by the first real Android build (CI run 35425779550) | FIXED (ANDROID-CI) — the property is internal; only the module's screen reads it |
| INT-076 | P2 | P16 | `stepLabel`, which names each onboarding step on screen, was deleted by 474cbd5 while `OnboardingScreen` still called it. Found by the first real Android build (CI run 35425779550) | FIXED (ANDROID-CI) — restored exactly as it was |
| INT-077 | P1 | P18 | no manifest declared `ACCESS_NETWORK_STATE`, which `AndroidNetworkMonitor` needs for `getActiveNetwork` and `registerNetworkCallback`; both throw `SecurityException` without it, and the realtime coordinator starts the monitor when the app comes to the foreground, so the app would most likely have crashed on start. Found by Android lint in the first real build (CI run 35429821238) | FIXED (ANDROID-CI) — declared by core:network, the module that uses it |
| INT-078 | P2 | P15 | the location provider guarded its calls with a helper and `runCatching` inside lambdas, which lint cannot see, and `core:location` declared none of the permissions it uses. Behaviour was safe; the protection was not explicit. Found by Android lint (CI run 35429821238) | FIXED (ANDROID-CI) — the module declares both location permissions; each protected call handles `SecurityException` explicitly |

## Rules

- An identifier is never reused, even after the defect is fixed or retired.
- A defect is registered here before it is referenced anywhere else.
- The next free identifier is `INT-079`.
