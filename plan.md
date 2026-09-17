# Implementation Plan

Updated: 2026-09-17T20:27:00+03:00

## Current executable phase

P23 — iOS Foundation. P22 Android Production is blocked in parallel by external/store/connected-gate requirements.

## Immediate handoff goal

Continue the roadmap without weakening release gates: start native Swift/SwiftUI foundation from the V3 specification while preserving P20/P22 as open connected/release gates. Do not fabricate store submission, signing, staging, device or provider evidence.

## Before implementation

1. Read `HANDOFF.md`, `PROJECT-STATUS.md`, `BLOCKERS.md` and `docs/spec/00-START-HERE.md`.
2. Treat `docs/spec/` as immutable Source of Truth.
3. Verify `git status`, branch and HEAD. Expected handoff implementation lineage includes P21 commit `9196f4d`.
4. Re-run relevant source regressions before modifying shared contracts/tokens.

## P23 tasks

1. Create native Swift/SwiftUI repository modules according to the Android domain boundaries and official roadmap.
2. Consume shared design tokens; Arabic RTL first.
3. Keep access/refresh/session storage aligned with the security specification and platform Keychain capabilities.
4. Keep API integration behind the P10 generated-client boundary; do not hand-author transport DTOs.
5. Add cache/location/maps/realtime/provider abstractions according to the iOS specification.
6. Add source qualification and tests that can run in the available environment.
7. Record every unexecutable Xcode/device gate honestly.

## Parallel open gates

- P2/P3 backend connected qualification.
- P10 real Django-generated OpenAPI + TS/Kotlin/Swift clients.
- P19 connected staging.
- P20 connected E2E/security/load/restore.
- P22 signed Android AAB + Play Internal/Closed/production.

## Gate

`P23 IOS FOUNDATION PASS` — NOT STARTED / NOT ACHIEVED.
