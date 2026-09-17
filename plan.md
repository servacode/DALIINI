# Autonomous Build Plan

Last updated: 2026-09-17T16:18:00+03:00

## Current phase

**P10 — OpenAPI Generated Clients**

P2-P9 connected/runtime gates remain pending where the current container lacks Django dependencies, PostGIS/Redis/Celery or external provider credentials. P9 source qualification is complete.

## Goal

Make the backend OpenAPI schema the canonical client contract and establish deterministic generation/drift controls for TypeScript, Kotlin and Swift without hand-written duplicate DTOs.

## Tasks

- [ ] Re-read API/backend/repository/roadmap contract requirements.
- [ ] Add canonical schema generation command and stable output locations.
- [ ] Add deterministic schema hash/drift checker.
- [ ] Define generated-package boundaries for TS/Kotlin/Swift.
- [ ] Add CI contract job.
- [ ] Generate real schema/clients when Django runtime and generators are available.
- [ ] Update evidence/status/handoff and commit logically.

## Acceptance criteria

- `openapi/schema.yaml` is generated from Django, not manually authored.
- Stable operation IDs and camelCase API boundary are preserved.
- TS/Kotlin/Swift generated code is read-only output.
- CI regenerates and fails on drift.
- No client manually duplicates backend contract DTOs.

## Risks

- Current container cannot import Django/DRF, so real schema generation cannot execute locally.
- Generator toolchains may require network/package installation.
- Hand-authored placeholder schema is prohibited.

## Gate

`P10 CONTRACT PASS` — **NOT YET ACHIEVED**.

## Pending earlier gates

P2-P9 remain below `CONNECTED_VERIFIED` until their documented runtime/connected tests execute successfully.
