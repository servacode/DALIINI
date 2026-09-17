# Implementation Plan

Updated: 2026-09-17T19:35:00+03:00

## Current phase

P20 — Full E2E / Security / Load / Restore Quality

## Goal

Create and execute every release-quality test that can run in the current environment, and prepare deterministic connected harnesses for staging. Do not mark the gate passed until the complete staging golden path, security suite, load baseline and restore drill have actually run.

## Tasks

1. Inventory existing backend/admin/Android tests against the critical suites in `17-TESTING-QA-EVIDENCE.md`.
2. Add a release-quality orchestrator that fails closed on missing required connected inputs.
3. Add backend security regression coverage for auth, RBAC, IDOR, evidence privacy, sessions/concurrency and account deletion where source/runtime permits.
4. Add golden-path staging test specification/harness: owner registration → draft → evidence → submit → admin approval → public discovery → duty → map/directions → rating → sensitive edit → reverification.
5. Add load-baseline scenarios for public discovery/search/map and authenticated owner/admin reads without destructive production behavior.
6. Add restore-drill validation/consistency checklist and evidence output format.
7. Add release blocker report enforcing no P0/P1 at RC.
8. Run all local/static/pure tests possible and record exact unavailable connected gates.
9. Keep P10 generated-contract gap and dependency lockfile gap visible; do not hand-author generated clients/lockfiles.
10. Update evidence, status, blockers and handoff.

## Acceptance criteria

- One command/harness can enumerate required P20 suites and fail closed when staging is absent.
- Security cases cover documented high-risk boundaries, not only happy paths.
- Golden path is executable against a supplied staging origin and test identities without embedded credentials.
- Load test is bounded and staging-only by default.
- Restore evidence captures backup identifier, disposable target, timestamps, consistency/smoke results and achieved RPO/RTO.
- No P20 PASS is claimed from source inspection.

## Required tests

- Backend unit/integration/security suites when Django dependencies become available.
- Admin lint/typecheck/build/Playwright when Node 24/pnpm dependencies are available.
- Android unit/build/instrumentation/device suites when Gradle/SDK/device are available.
- Connected staging golden path.
- Bounded staging load baseline.
- Timed staging restore drill.

## Expected files

- `infrastructure/quality/**`
- `apps/backend/tests/**` or domain test additions as required.
- `apps/admin/**` Playwright/config additions as required.
- `artifacts/evidence/**`
- root project-management files.

## Risks

- EXT-004: no V3 GitHub remote for Render deployment.
- EXT-005: production-like staging provisioning has external cost.
- P10 real OpenAPI generation is still blocked by backend runtime.
- `uv.lock` and `pnpm-lock.yaml` are not yet generated; release reproducibility cannot pass until real dependency resolution is available.

## Gate

Target gate: `P20 RELEASE QUALITY PASS`.

Current phase may reach source/harness qualification only; full gate requires connected staging and actual restore/load/security execution.
