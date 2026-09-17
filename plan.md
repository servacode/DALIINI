# Autonomous Build Plan

Last updated: 2026-09-17T16:28:00+03:00

## Current phase

**P12 — Admin Foundation**

P10 remains IN_PROGRESS pending real generated contracts. P11 is SOURCE_IMPLEMENTED; its runtime web gate is pending Node 24/pnpm. P12 source work may proceed independently.

## Goal

Bootstrap the custom Next.js Admin with RTL design system, secure browser-session/BFF boundary, generated API client boundary, navigation/layout and permission-aware UI foundations.

## Tasks

- [ ] Re-read Admin architecture/security/design requirements.
- [ ] Bootstrap `apps/admin` Next.js + strict TypeScript.
- [ ] Consume shared design tokens/components.
- [ ] Implement secure same-origin BFF/session boundary; no refresh token in browser storage.
- [ ] Add auth shell/layout/navigation and permission primitives.
- [ ] Add lint/type/build/test/Playwright configuration.
- [ ] Source qualify; runtime verify when Node 24/pnpm/backend contract is available.

## Gate

`P12 ADMIN FOUNDATION PASS` — **NOT YET ACHIEVED**.
