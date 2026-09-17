# Autonomous Build Plan

Last updated: 2026-09-17T13:55:00+03:00

## Current phase

**P9 — Content Services / Ads / Push / Analytics**

P2 remains `IN_PROGRESS` because exact dependency locking and connected qualification cannot run in this container. P3-P7 are `SOURCE_IMPLEMENTED`; their runtime gates remain pending for the same environment limitation.

## Goal

Implement advertisements, notification persistence/provider boundaries, push interfaces and analytics registry without coupling external providers to domain truth.

## Tasks

- [ ] Re-read realtime, security, API and testing documents for P8.
- [ ] Define event envelope and stable event names.
- [ ] Implement Channels consumers and scope authorization.
- [ ] Implement after-commit event publisher/service.
- [ ] Integrate taxonomy/facility/availability invalidation hooks without coupling domain writes to transport internals.
- [ ] Add connected tests for Redis/Channels when runtime is available.
- [ ] Add source tests for sanitization, authorization boundaries and transaction-on-commit behavior.
- [ ] Update evidence/status/handoff and commit logically.

## Acceptance criteria

- Events are sanitized invalidation/change signals, not private entity dumps.
- No event is emitted before the transaction commits.
- Province/facility/user scopes enforce authorization server-side.
- Clients can always refetch authoritative REST state.
- No private evidence, storage keys, reviewer notes or secrets can enter public realtime payloads.

## Expected files

- `apps/backend/realtime/**`
- domain integration hooks/services only where required.
- tests/evidence/governance updates.

## Risks

- Redis/Channels connected behavior cannot be proven in the current container.
- Realtime fan-out must not create cross-province or cross-owner data leakage.
- P8 must not duplicate domain truth already owned by REST/services.

## Gate

`P9 CONTENT SERVICES PASS` — **NOT YET ACHIEVED**.

## Pending earlier gates

- `P7 PUBLIC DISCOVERY PASS` — source implemented; PostGIS/runtime qualification pending.
- `P6 AVAILABILITY DUTY PASS` — source implemented; PostgreSQL overlap/concurrency/runtime qualification pending.
- `P5 OWNER DOMAIN PASS` — source implemented; runtime/tooling qualification pending.
- `P4 TAXONOMY PASS` — source implemented; runtime/tooling qualification pending.
- `P3 AUTH RBAC PASS` — source implemented; runtime/tooling qualification pending.
- `P2 BACKEND FOUNDATION CONNECTED PASS` — pending exact `uv.lock` + runtime/tooling qualification.
