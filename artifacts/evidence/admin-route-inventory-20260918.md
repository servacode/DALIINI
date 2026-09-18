# Admin route inventory — taken before any code was changed

**Date:** 2026-09-18 · **HEAD at inspection:** `8473bcf` · **Branch:** `main` · working tree
clean apart from the untracked handoff-zip artefacts that have been there since intake.

Required by §3 of the P11–P13 brief: every Admin route, what it renders today, which
generated operation backs it, the permission it needs, whether it mutates, whether it needs
confirmation, whether it touches private evidence, and whether it needs filtering.

Nothing in the repository was modified to produce this.

## Current state, in one line

Every route renders a static `OperationPage` that prints the endpoint it *would* call.
`backendRequest`, `setRefreshCookie`, `getRefreshCookie` and `clearRefreshCookie` exist and
are never called from anywhere. The only live route handler is `POST /api/session/logout`,
which clears a cookie nothing ever sets. The generated TypeScript client is a workspace
dependency of `apps/admin` and is imported nowhere.

## The contract surface available to bind against

33 admin operations. 19 reads, 14 mutations.

| Route | Renders today | Generated operation(s) | Permission | Kind | Confirm | Evidence | Filtering |
|---|---|---|---|---|---|---|---|
| `/login` | static copy, no form | `authLogin` | — | mutation | no | no | — |
| *(BFF)* | — | `authRefresh` | — | mutation | no | no | — |
| *(BFF)* | `/api/session/logout` clears a cookie nothing sets | `authLogout` | — | mutation | no | no | — |
| `/dashboard` | static panel | `adminDashboardRetrieve` | `admin.dashboard.read` | read | no | no | no |
| `/reviews` | `OperationPage` | `adminReviewsList` | `admin.reviews.read` | read | no | no | **yes — undeclared** |
| `/reviews/[id]` | `OperationPage` (same route object) | `adminReviewRetrieve`, `adminReviewApprove`, `adminReviewReject` | `admin.reviews.read` / `admin.reviews.decide` | read + mutation | **yes, both** | **yes** | no |
| *(evidence)* | — | `adminEvidenceContentRetrieve` | `admin.evidence.read` | read | no | **yes** | no |
| `/facilities` | `OperationPage` | `adminFacilitiesList` | `admin.facilities.read` | read | no | no | **yes — undeclared** |
| `/facilities/[id]` | `OperationPage` | `adminFacilityRetrieve`, `adminFacilitySuspend`, `adminFacilityReactivate`, `adminFacilityClose` | `admin.facilities.read` / `admin.facilities.manage` | read + mutation | **yes, all three** | no | no |
| `/users` | `OperationPage` | `adminUsersList` | `admin.users.read` | read | no | no | **yes — undeclared** |
| `/users/[id]` | `OperationPage` | `adminUserRetrieve`, `adminUserBlock`, `adminUserUnblock`, `adminUserRolesReplace` | `admin.users.read` / `admin.users.manage` / `admin.roles.manage` | read + mutation | **yes, block and role replace** | no | no |
| *(roles)* | — | `adminRolesList` | `admin.roles.read` | read | no | no | no |
| `/taxonomy/groups` | `OperationPage` | `adminCategoryGroupsList` | `admin.taxonomy.read` | read only | — | no | no |
| `/taxonomy/categories` | `OperationPage` | `adminCategoriesList`, `adminCategoryCapabilitiesReplace`, `adminCategoryProvinceReplace` | `admin.taxonomy.read` / `admin.taxonomy.manage` | read + mutation | **yes, both replaces** | no | no |
| `/provinces` | `OperationPage` | `adminProvincesList`, `adminProvinceUpdate` | `admin.provinces.read` / `admin.provinces.manage` | read + mutation | **yes — activation is a rollout decision** | no | no |
| `/verification` | `OperationPage` | `adminVerificationRequirementsList`, `adminVerificationRequirementCreate` | `admin.verification.read` / `admin.verification.manage` | read + create | yes on create | no | no |
| `/ads` | `OperationPage` | `adminAdsList`, `adminAdCreate`, `adminAdDelete` | `admin.ads.read` / `admin.ads.manage` | read + mutation | **yes on delete** | no | no |
| `/audit` | `OperationPage` | `adminAuditList` | `admin.audit.read` | read | no | no | **yes — undeclared** |
| `/analytics` | `OperationPage` | `adminAnalyticsRetrieve` | `admin.analytics.read` | read | no | no | no |
| `/settings` | `OperationPage` | `adminSettingsList`, `adminSettingWrite` | `admin.settings.read` / `admin.settings.manage` | read + mutation | **yes** | no | no |
| `/system` | `OperationPage` | `adminSystemStatusRetrieve` | `admin.system.read` | read | no | no | no |

Confirmation is marked **yes** wherever the action is irreversible from the Admin, changes
who can sign in, or changes what the public sees. Approving or rejecting an application,
closing a facility, blocking a user, replacing a role set, activating a province, flipping a
category switch, deleting an advertisement and writing a platform setting all qualify.

## Two findings that change what this batch can deliver

### INT-041 — four list endpoints filter on query parameters the contract never declares

The runtime reads 14 query parameters that appear nowhere in `openapi/schema.yaml`, so the
generated client's request interfaces are empty and the filters are unreachable through it:

| Operation | Parameters read by the view | Declared in the contract |
|---|---|---|
| `adminReviewsList` | `kind`, `status`, `province`, `category` | none |
| `adminFacilitiesList` | `status`, `province`, `category`, `q` | none |
| `adminUsersList` | `q`, `status` | none |
| `adminAuditList` | `actor`, `action`, `resource`, `requestId` | none |

Verified by parsing the committed schema: **0 of the 33 admin operations declare a query
parameter.**

This collides directly with §2 of the brief. Building a search box for facilities or a
filter for the audit trail would require hand-writing a request outside the generated
client, which §2 forbids. The fix belongs on the Django side — `@extend_schema(parameters=…)`
on four views, then regeneration — and is a contract-description change with no behavioural
effect, exactly like the rest of P10.

### INT-018 — the taxonomy is read-only, so "إدارة Taxonomy" is partly unbuildable

The contract offers no create, update or delete for `CategoryGroup` or `Category` rows
themselves. What it does offer is `adminCategoryCapabilitiesReplace` and
`adminCategoryProvinceReplace`. Related gaps: `VerificationRequirement` has create and list
but no update or delete; `Advertisement` has create, delete and list but no update.

So of §1's list, these are deliverable in full — login, refresh, logout, dashboard, reviews,
evidence, approve/reject, facilities, users, roles, provinces, CategoryProvince switches,
audit, analytics, settings, system status — and these are deliverable only in part:

* **Taxonomy management** — capabilities and province switches yes; creating, renaming or
  reordering a group or category no.
* **Verification requirements** — list and create yes; editing or retiring one no.
* **Advertisements** — list, create and delete yes; editing one no.

Adding those endpoints is backend product work with its own review. It is named here rather
than worked around, and the affected pages will state the limit on screen rather than
present a control that cannot work.

## Supporting state

* No Tailwind, no Radix, no Vitest, no Playwright (INT-022). Styling is plain CSS over the
  generated design-token custom properties in `packages/design-tokens/generated/tokens.css`,
  imported once by `app/globals.css` (28 lines).
* `next.config.ts` already sets a strict CSP with `connect-src 'self'`, which is consistent
  with a same-origin BFF and would block a browser→Django call outright.
* The refresh cookie is named `__Host-directory_admin_refresh` and sets `secure` only when
  `NODE_ENV === "production"`. A browser rejects a `__Host-` cookie without `Secure`, so the
  development login flow cannot work today. That is INT-012 and §4 of this brief fixes it.
* `POST /api/session/logout` compares `Origin` to `Host` but skips the check when `Origin`
  is absent, which is the fail-open behaviour §8 calls out.

---

## What was done after this inventory was taken

* **INT-041 fixed.** All 14 query parameters are now declared with
  `@extend_schema(parameters=…)` on the four views, the schema was regenerated and the
  three clients with it. The generated request interfaces are in the BFF evidence file.
* **INT-018 and INT-042 left open**, named rather than worked around. INT-042 is new: no
  endpoint returns the caller's own Admin permissions, so a permission-aware navigation
  cannot be built without either a new backend endpoint or probing every list route for a
  403. Registered in `RECEIPT-AUDIT-2026-09-17.md`.
