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

Reviewed against the receipt audit (`RECEIPT-AUDIT-2026-09-17.md`) and the FIX-P0 execution results.
**No external blocker was added, removed or changed.** EXT-001 to EXT-005 all still stand as written.

Two conditions found during this batch are deliberately **not** recorded here, because this register is
only for genuine external blockers and not for bugs or local toolchain issues:

- The four P0 defects closed by FIX-P0, and every remaining `INT-*` defect, are internal bugs. They are
  tracked in `RECEIPT-AUDIT-2026-09-17.md` and `PROJECT-STATUS.md`.
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
  INT-005, INT-006 and INT-008 remain open internal bugs, tracked in `RECEIPT-AUDIT-2026-09-17.md`.
- Google Maven not serving this workstation is classified `ENVIRONMENT_LIMITATION`, recorded under
  "Local execution limitation" in `PROJECT-STATUS.md`. It does not block the backend or P10.
