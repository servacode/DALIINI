# External Blockers

Only genuine external blockers are recorded here. Bugs and local toolchain issues are not blockers.

| ID | Description | Why internal work cannot resolve it | Owner input/action required | Work completed despite blocker | Waiting on blocker |
|---|---|---|---|---|---|
| EXT-001 | Final purchased `ROOT_DOMAIN` not yet available in this execution environment | Domain ownership and registrar/DNS authority are external assets | Provide/authorize owned domain at production cutover | Domain-neutral config and hostname conventions can be fully built | Production DNS/TLS cutover, final legal/store URLs, app-link association |
| EXT-002 | Owned provider credentials not available (OTP, object storage, Sentry, FCM/APNs) | Credentials must come from owned external accounts and cannot be invented | Supply through secure environment when reaching connection/release gates | Provider abstractions, env contracts, local/staging-compatible implementation can proceed | Provider-connected staging/production qualification for each external service |
| EXT-003 | Store/developer account credentials and signing secrets not available | Google Play/Apple access and signing secrets are external/irreversible assets | Authorized account access and secure secret injection at release phases | Build/release automation and policy materials can be prepared | Play/App Store track submission and production verification |
| EXT-004 | No GitHub repository for Directory Platform V3 is available to the connected GitHub/Render accounts | Render needs a reachable Git remote; the connected GitHub tooling can inspect/write existing repositories but cannot create the missing V3 repository | Create/authorize a dedicated `servacode/directory-platform-v3` repository (or equivalent V3 remote) and grant Render access | Complete isolated staging Blueprint, Docker source, smoke/runbooks; verified existing Render resources are V2 and left untouched | Connected P19 deployment and CI-triggered staging qualification |
| EXT-005 | Production-like staging plans in `render.yaml` incur external service cost | Financial provisioning cannot be performed implicitly; free Redis lacks persistence and is not production-like | Approve the staging budget/plans before provisioning Render DB/Key Value/API/worker/web/admin resources | Paid-plan staging topology is fully specified but not provisioned | P19 staging runtime/restore/load qualification |

## Register review — 2026-09-17 (FIX-P0 batch)

Reviewed against the receipt audit (`artifacts/evidence/RECEIPT-AUDIT-2026-09-17.md`) and the FIX-P0 execution results.
**No external blocker was added, removed or changed.** EXT-001 to EXT-005 all still stand as written.

Two conditions found during this batch are deliberately **not** recorded here, because this register is
only for genuine external blockers and not for bugs or local toolchain issues:

- The four P0 defects closed by FIX-P0, and every remaining `INT-*` defect, are internal bugs. They are
  tracked in `artifacts/evidence/RECEIPT-AUDIT-2026-09-17.md` and `PROJECT-STATUS.md`.
- Google Maven (`dl.google.com` / `maven.google.com`) does not serve the current workstation, so Android
  dependency resolution cannot complete and `gradle :app:assembleDebug` is `NOT_VERIFIED`. That is a local
  network limitation, recorded under "Local execution limitation" in `PROJECT-STATUS.md`.

Note on EXT-004: the audit confirmed that no V3 remote exists for the connected GitHub account. Any repo
chosen for V3 must be a dedicated one; `servacode/directory` previously held an unrelated V1 NestJS
codebase and is not part of this project's lineage.

## Register review — 2026-09-17 (P2 connected qualification batch)

Reviewed again. **No external blocker added, removed or changed.** EXT-001 to EXT-005 stand as written.

Two conditions from this batch are deliberately kept out of the register, which is for genuine external
blockers only:

- INT-009, INT-028, INT-007 and INT-030 were internal bugs and are now closed. INT-010, INT-029,
  INT-005, INT-006 and INT-008 remain open internal bugs, tracked in `artifacts/evidence/RECEIPT-AUDIT-2026-09-17.md`.
- Google Maven not serving this workstation is classified `ENVIRONMENT_LIMITATION`, recorded under
  "Local execution limitation" in `PROJECT-STATUS.md`. It does not block the backend or P10.

## Register review — 2026-09-18 (P10 contracts recovery)

Reviewed again. **No external blocker added, removed or changed.** EXT-001 to EXT-005 stand.

EXT-004 note: the generated clients are now committed, so a consumer can build against the
contract without the V3 remote. The remote is still required for connected staging.

Kept out of this register, as before, because it is for genuine external blockers only:

- INT-031, INT-033, INT-034, INT-010 and INT-016 were internal bugs and are now closed.
  INT-005 is closed by this batch. INT-035, INT-036 and INT-037 are new internal
  divergences between the runtime and the specification, tracked in
  `artifacts/evidence/RECEIPT-AUDIT-2026-09-17.md`.
- Kotlin client compilation is blocked by the same local network condition as Google Maven:
  the Gradle distribution download from services.gradle.org resets. Classified
  `ENVIRONMENT_LIMITATION` under "Local execution limitation" in `PROJECT-STATUS.md`.

## Register review — 2026-09-24 (complete product batch)

**No external blocker added, removed or changed.** EXT-001 to EXT-005 stand.

One product decision is recorded here because implementation is genuinely waiting on it, not on
anything technical:

### INT-084 — adding a manager still takes a raw account id

**State:** OPEN, deliberately, and a workaround was not built.

An owner adds a manager by typing that person's account id, which nobody knows by heart. The
obvious fix — type their phone number instead — cannot be built as a lookup: an endpoint that
answers "does an account exist for this number" is account enumeration, and this platform's
whole identity is phone numbers.

The safe shape is an **invitation**, not a lookup: the owner enters a phone number, the backend
records a pending membership against that number and answers the same way whether or not an
account exists, and the membership is granted when someone signs in as that number. Nothing is
revealed, and the owner gets the flow they expect.

That needs product decisions no specification answers yet:

- May an owner invite someone who has no account, and for how long does the invitation stand?
- Who can see and revoke a pending invitation, and does the invitee see it before accepting?
- Does accepting require anything beyond signing in as that number?
- What does the admin console show, given that a pending invitation is a claim about a person
  who may never have used the platform?

Until those are answered, the field stays as it is and now says plainly that it takes an account
id and that the server offers no lookup by phone. The alternative — shipping a search endpoint —
would trade a UX annoyance for an enumeration vulnerability.

## Register review — 2026-09-29 (design system, smart console, site, Android phase 3, push)

EXT-001, EXT-003 and EXT-005 stand as written. EXT-004 is superseded in practice: the work now
lives in `servacode/DALIINI`, and every CI job there is blocked by EXT-006 below.

**EXT-002, narrowed.** What each credential now unlocks, with the code already in place:

- **FCM**: the HTTP v1 transport exists (`notifications/providers/fcm_http.py`). Push reaches
  Android phones as soon as `PUSH_PROVIDER=fcm`, `FCM_PROJECT_ID` and `FCM_SERVICE_ACCOUNT_JSON`
  are set on the API and worker, and the four `DIRECTORY_FIREBASE_*` values are set for the
  Android build (`docs/runbooks/staging-deploy.md`). Production refuses to start half-configured.
- **OTP**: still the one blocker for owner sign-in in production. The provider is not chosen
  (SMS gateway, WhatsApp, or both), and `accounts/otp.py` has no adapter for any real provider,
  because its request format, sender registration and delivery receipts depend on the choice.
  Once chosen, the adapter is a single function behind `deliver_otp`.
- **Sentry**: optional; the backend and the Android app both stay silent without a DSN.

### EXT-006 — GitHub Actions does not run any job for this repository

**State:** OPEN. Every job of every workflow (CI, CodeQL, security) fails within three to four
seconds, before its first step, with empty logs — on `main` as well as on the working branch.
That points at the account (Actions billing, spending limit or runner access), not at the code.
**Owner action:** check Settings → Billing and plans → Actions, and the repository's Actions
permissions. **Meanwhile:** every gate is run locally before each push, with the same commands
the workflows use.

### Operator actions before launch (not blockers, recorded so they are not forgotten)

- **Emergency numbers**: 110 (ambulance), 113 (fire) and 112 (police) were seeded from commonly
  cited lists and carry `OPERATOR_VERIFICATION_REQUIRED`. The console shows them first, under a
  warning, until someone confirms each against an official source («تأكيد صحة الرقم»).
- **Contact form**: the site's form posts from the visitor's browser, so the API's
  `CORS_ALLOWED_ORIGINS` must include the site's origin.
- **App Links**: set `DIRECTORY_APP_LINK_HOST` to the site's host and `ANDROID_CERT_SHA256` on the
  site to the Play signing key's fingerprint (`apps/android/play/app-links.md`).
