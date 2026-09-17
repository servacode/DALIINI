# Implementation Plan

Updated: 2026-09-17T19:10:00+03:00

## Current phase

P19 — Staging Production-Like Deploy

## Goal

Prepare a production-like staging environment and deployment/runbook source for Django/PostGIS/Redis/Channels/Celery/S3-compatible storage plus public/admin web, without inventing real domains or credentials.

## Tasks

1. Read infrastructure/environment/domain/operations specifications.
2. Audit existing Docker/runtime smoke and production settings.
3. Add staging environment contract using `ROOT_DOMAIN` placeholders only.
4. Define services: API/ASGI, worker, PostgreSQL/PostGIS, Redis, object storage and web/admin.
5. Add readiness/liveness and deployment smoke sequence.
6. Add secret inventory with placeholders only.
7. Add migration/deploy/rollback runbooks.
8. Add backup/restore staging rehearsal commands.
9. Add monitoring/logging expectations and failure signals.
10. Run all static/source checks possible and document external staging blockers.

## Acceptance criteria

- No production/staging secret is committed.
- No real domain is invented; `ROOT_DOMAIN` remains configurable.
- API/worker/realtime/database/cache/storage responsibilities are explicit.
- Staging uses production-like settings, not Django dev server assumptions.
- Migration, rollback, backup and restore steps are documented and scriptable.
- Runtime smoke fails closed when dependencies or secrets are absent.

## Required tests

- YAML/config parse and source validation.
- Secret-marker scan.
- production/staging settings fail-closed source checks.
- deployment script syntax checks.
- connected staging runtime smoke when hosting credentials/services exist.

## Expected files

- `infrastructure/**`
- `.github/workflows/**` if staging CI is appropriate.
- environment/runbook docs and evidence.
- project management files.

## Risks

- Real staging hosting/domain/object-storage credentials may be external blockers.
- Current container has no Docker/PostgreSQL/Redis and cannot prove runtime deployment.

## Gate

Target gate: `P19 STAGING RUNTIME PASS`.

Source/runbook qualification can be completed locally; connected staging runtime cannot pass without hosting/services.
