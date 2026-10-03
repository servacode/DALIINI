# دليني — Daliini

A directory of places in Syria: find the nearest pharmacy, see whether it is open or on duty, call
it and get directions. It launches in Raqqa with pharmacies and pharmacy duty, and every other
province and category is switched on from the console without changing an app.

## What is in this repository

| Part | Path | Built with |
|---|---|---|
| Backend API | `apps/backend` | Python 3.13, Django 5.2, DRF, PostgreSQL 17 + PostGIS, Redis, Celery, Channels |
| Admin console | `apps/admin` | Next.js, React, TypeScript (strict) |
| Public website | `apps/web` | Next.js, React, TypeScript (strict) |
| Android app | `apps/android` | Kotlin, Jetpack Compose, MapLibre |
| iPhone app | — | Kotlin Multiplatform, after the Android release (DECISION-051) |
| WhatsApp code sender | `services/whatsapp-bot` | Node.js |
| Design system | `packages/design-tokens` | one source for colours, type, words and icons on every platform |
| API clients | `packages/api-*` | generated from `openapi/schema.yaml`, never written by hand |

## Start here

```bash
pnpm install
pnpm stack:up      # database, storage, API and workers in Docker
pnpm stack:seed    # local operators and sample data
pnpm dev:admin     # http://localhost:3000
pnpm dev:web       # http://localhost:3001
```

Details: `infrastructure/docker/README.md`. Before every merge: `bash scripts/verify.sh`.

## Where things are decided

- **The plan we work from:** `docs/project/ROADMAP.md`.
- **Decisions and their reasons:** `docs/project/DECISIONS.md`.
- **The original specification:** `docs/spec/`, led by `01-MASTER-SPECIFICATION.md`. It is kept
  unchanged; a later decision that departs from it is recorded in `DECISIONS.md`.
- **Status, blockers and evidence:** `docs/project/PROJECT-STATUS.md`, `BLOCKERS.md`,
  `EVIDENCE.md`.

No production secret belongs in Git.
