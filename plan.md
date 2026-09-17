# Autonomous Build Plan

Last updated: 2026-09-17T16:34:00+03:00

## Current phase

**P13 — Admin Operations**

P10 remains IN_PROGRESS pending generated contracts. P11 and P12 are SOURCE_IMPLEMENTED with runtime web gates pending Node 24/pnpm. P13 source work may proceed behind the same typed API/RBAC boundaries.

## Goal

Implement staff operational surfaces for review, facilities, users, taxonomy, provinces, advertisements, audit, analytics, settings and system status without moving authorization truth out of Django.

## Tasks

- [ ] Define operational route shells and permission map.
- [ ] Build review queue/detail workflows and explicit reject reason UX.
- [ ] Build facility/user/taxonomy/province/verification operations.
- [ ] Build ads editor, audit filters, analytics and system status.
- [ ] Use generated client only once P10 is available; no duplicated transport DTOs.
- [ ] Add Playwright golden paths when runtime toolchain is available.

## Gate

`P13 ADMIN OPERATIONS PASS` — **NOT YET ACHIEVED**.
