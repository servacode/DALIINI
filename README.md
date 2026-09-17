# Directory Platform V3 — الدليل

Greenfield implementation of the Serva Code Directory Platform V3.

## Source of truth

The immutable implementation baseline is copied under `docs/spec/` from `SERVA-CODE-DIRECTORY-V3-COMPLETE-HANDOFF.zip`. The highest authority is `docs/spec/01-MASTER-SPECIFICATION.md`, followed by `docs/spec/02-BASELINE-DECISIONS.md` and the mandatory sequence in `docs/spec/24-IMPLEMENTATION-ROADMAP.md`.

## Architecture

- Backend: Python 3.13, Django 5.2 LTS, DRF, PostgreSQL 17 + PostGIS, Redis, Celery, Channels.
- Admin/Public Web: Next.js, React, TypeScript strict.
- Android: Kotlin + Jetpack Compose Native.
- iOS: Swift + SwiftUI Native.
- Maps: MapLibre Native; routing via provider abstraction with OSRM baseline.

## Governance

See `plan.md`, `PROJECT-STATUS.md`, `IMPLEMENTATION-LOG.md`, `DECISIONS.md`, `BLOCKERS.md`, `HANDOFF.md`, and `EVIDENCE.md`.

No production secret belongs in Git.
