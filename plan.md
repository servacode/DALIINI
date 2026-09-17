# Implementation Plan

Updated: 2026-09-17T22:15:00+03:00

## Verification baseline

`RECEIPT-AUDIT-2026-09-17.md` is the accepted verification baseline. A gate is PASS only with a real
command exit code. Textual source assertions no longer qualify a gate on their own, because that method
allowed four P0 defects to pass every gate from P2 up to P21.

## Resume point correction

The earlier plan nominated P23 iOS Foundation as the next independently executable phase. That is
withdrawn. The owner accepted the audit's correction on 2026-09-17: P23 and all iOS work are deferred,
because the P10 generated-client boundary that P23 is required to build against does not yet carry a
usable contract.

## Completed in the current batch

FIX-P0 — the four P0 defects are closed:

1. FIX-001 `apps/backend/directory_backend/asgi.py` — ASGI initialization order.
2. FIX-002 `apps/backend/directory_backend/settings/base.py` — removed Django messages without sessions.
3. FIX-003 `apps/backend/sessions/migrations/0002_rotation_security_fields.py` — migration app label.
4. FIX-004 `apps/android/build-logic/src/main/kotlin/serva.android.compose.gradle.kts` and FIX-004b
   `apps/android/core/designsystem/build.gradle.kts` — AGP 9.4 API migration.

The backend now boots and serves. `gradle :app:assembleDebug` reaches dependency resolution.

## Current executable phase

**P2 — Backend Foundation, connected qualification.** The runtime path is proven; the phase cannot close
until the remaining backend gate items are green.

### P2 closing tasks, in order

1. **INT-009 migration drift.** `manage.py makemigrations --check --dry-run` still exits 1. Generate the
   missing migrations for `accounts` (the `PermissionsMixin` fields `groups`, `is_superuser` and
   `user_permissions` were never migrated), `business_hours`, `content_services`, `facilities`,
   `notifications` and `pharmacy_duty`. Review each generated operation rather than accepting it blindly;
   the `pharmacy_duty` change drops and recreates the duty exclusion constraint.
2. **INT-028 pytest collection.** `uv run pytest` aborts before running anything because
   `accounts/tests/`, `admin_console/tests/` and `search/tests/` have no `__init__.py` while
   `test_source_contract.py` and `test_source_security.py` basenames repeat. Add the three package markers
   or give the modules unique names.
3. **INT-007 test database engine.** `settings/test.py` uses SQLite while the models are spatial, so no
   database-backed test can ever pass. Point the test settings at PostgreSQL + PostGIS.
4. Re-run the full backend gate: `ruff`, `mypy`, `pytest`, `manage.py check`,
   `makemigrations --check --dry-run`, `migrate`, and the connected runtime smoke.

`ruff` reports 106 errors and `mypy` 556; decide explicitly whether they block P2 closure or move to a
separate quality batch with a recorded ADR.

## Gate

`P2 BACKEND FOUNDATION CONNECTED PASS` — NOT ACHIEVED.

Acceptance is execution-only:

```
manage.py check                          -> exit 0   [achieved]
manage.py showmigrations                 -> exit 0   [achieved]
manage.py migrate                        -> exit 0   [achieved]
manage.py makemigrations --check --dry-run -> exit 0 [OPEN, INT-009]
uv run pytest                            -> exit 0   [OPEN, INT-028 + INT-007]
GET /health/live/                        -> 200      [achieved]
GET /health/ready/                       -> 200 with database and redis ok [achieved]
no infrastructure 500 on the core HTTP path -> [achieved]
```

## Next phase after P2

**P10 — Contracts.** It must precede any further client work. The generated schema currently carries
72 paths with zero component schemas and zero request bodies, so every generated client would have no
DTOs. Admin, Android and iOS all depend on that boundary.

## Deferred, recorded, not started

P23 to P26 iOS. P11 to P13 Admin data binding. P4 province and taxonomy seed. `DutyShift` field
restoration. The ruff and mypy cleanups. Every item carries an INT identifier in
`RECEIPT-AUDIT-2026-09-17.md`.

## Parallel open gates

- P19 connected staging, still waiting on EXT-004 and EXT-005.
- P20 connected end-to-end, security, load and restore.
- P22 signed Android AAB and Play tracks, still waiting on EXT-002 and EXT-003.

## Android note

`gradle :app:assembleDebug` is `NOT_VERIFIED` because Google Maven does not serve this machine. Re-run it
from a network that can reach `dl.google.com` before making any claim about the Android build. Do not
record `BUILD_VERIFIED` until that command exits 0, and do not record `DEVICE_VERIFIED` without a real
device.
