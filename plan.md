# Autonomous Build Plan

Last updated: 2026-09-17T16:22:00+03:00

## Current phase

**P11 — Public Web**

P10 tooling is implemented but its gate remains open pending real Django schema and generated clients. P11 is independent and may proceed.

## Goal

Build the public Next.js web surface required for landing, privacy, terms, support and account deletion with RTL-first shared design tokens and domain-neutral production configuration.

## Tasks

- [ ] Re-read public-web/legal/security/release requirements.
- [ ] Bootstrap Next.js + TypeScript app under `apps/web`.
- [ ] Consume shared design tokens; RTL Arabic baseline.
- [ ] Implement landing, privacy, terms, support and delete-account pages.
- [ ] Add security headers/metadata and ROOT_DOMAIN/API env boundaries.
- [ ] Add lint/type/build/test configuration.
- [ ] Run available source checks; runtime build when Node 24/pnpm is available.

## Gate

`P11 PUBLIC WEB PASS` — **NOT YET ACHIEVED**.
