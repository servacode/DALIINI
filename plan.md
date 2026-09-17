# Implementation Plan

Updated: 2026-09-18T02:40:00+03:00

## Verification baseline

`RECEIPT-AUDIT-2026-09-17.md` is the accepted verification baseline. A gate is PASS only
with a real command exit code. Textual source assertions no longer qualify a gate on their
own, and a schema is not qualified by reading its YAML: a representative sample of
operations must be exercised against the running service and validated against the
document.

## Completed

**FIX-P0** — ASGI initialization order, Django messages without sessions, the sessions
migration app label, and the AGP 9 convention plugin plus source sets.

**P2 connected qualification** — migration convergence, both business invariants proven at
the database level, and the test suite running for the first time. P2 is
`CONNECTED_VERIFIED`.

**P10 contracts recovery** — the generated document went from 84 operations with zero
component schemas, zero request bodies and no security scheme, to 140 component schemas,
35 request bodies, 72 response schemas, an explicit bearer scheme and 84 unique operation
ids, with zero generator errors and zero warnings. All three clients generate. The drift
gate is real. Twelve contract tests and a seventeen-case runtime smoke lock it in place.

Five defects were found and fixed on the way, three of which meant the authentication
surface had never worked: INT-031, INT-033, INT-034, INT-010 and INT-016.

## Current executable phase

**P11 to P13 — Admin data binding**, now that a usable TypeScript client exists.

The Admin is still a descriptive scaffold: `backendRequest`, `setRefreshCookie` and
`getRefreshCookie` exist but are never called, and every page renders a static
`OperationPage` describing the endpoint it would call. None of the operational components
that `10-DESIGN-SYSTEM-UX.md` requires exist yet: DataTable, FilterBar, Pagination,
FormSection, ConfirmDialog, DiffViewer, AuditTimeline.

### Suggested order

1. Implement the login and refresh BFF against `authLogin` and `authRefresh`, using the
   existing `__Host-` refresh cookie helpers. Fix INT-012 while doing it: the cookie sets
   `secure` only in production, and a `__Host-` cookie without `Secure` is rejected by the
   browser, so the development flow cannot work today.
2. Bind the review queue and the review detail page to `adminReviewsList`,
   `adminReviewRetrieve`, `adminReviewApprove` and `adminReviewReject`, including the
   audited evidence stream.
3. Bind facilities, users and roles.
4. Add the operational components the design system requires.
5. Install Playwright and Vitest and write the golden paths that `P13 ADMIN GOLDEN PATH
   PASS` depends on. Neither tool is currently declared, so the gate has no runner.

## Gate

`P13 ADMIN GOLDEN PATH PASS` — NOT STARTED.

Acceptance is execution-only: admin login, review approve and reject, taxonomy
activation, user role guard, ad creation and audit access all pass as Playwright golden
paths against a running backend.

## Deferred, recorded, not started

P23 to P26 iOS. The P4 province and taxonomy seed, which is why `/public/provinces/`
returns an empty list. `DutyShift` field restoration. Every item carries an INT identifier
in `RECEIPT-AUDIT-2026-09-17.md`.

## Quality debt

`DECISIONS.md` carries the register. DEBT-001 ruff at 101 issues, down from 106 at intake.
DEBT-002 mypy at 556, untouched. DEBT-003 the deferred `PermissionsMixin` evaluation. All
three are mandatory before staging or production closure and none blocks Admin binding.
The standing rule is no new lint or type debt in a touched file.

## Contract discipline

Django and DRF are the source of truth; `openapi/schema.yaml` is generated from them and
the three clients are generated from it. After any change that touches a request or
response, run `./scripts/generate-openapi.sh` and `./scripts/generate-api-clients.sh` and
commit the regenerated artefacts with the source change. No consumer may hand-write a
transport DTO. See `openapi/README.md` and DECISION-010 to DECISION-012.

## Parallel open gates

- P19 connected staging, waiting on EXT-004 and EXT-005.
- P20 connected end-to-end, security, load and restore.
- P22 signed Android AAB and Play tracks, waiting on EXT-002 and EXT-003.

## Android note

Unchanged. `gradle :app:assembleDebug` is `NOT_VERIFIED` and Android stays
`SOURCE_IMPLEMENTED`. Google Maven does not serve this machine, and the Gradle
distribution download from services.gradle.org resets as well; both are the same
`ENVIRONMENT_LIMITATION`. Do not downgrade AGP, change Compose or AndroidX versions,
change SDK policy, or use an untrusted mirror. The generated Kotlin client is committed
and ready to replace `UnboundGeneratedPublicApi` and `UnboundGeneratedOwnerApi` when
Android can build.
