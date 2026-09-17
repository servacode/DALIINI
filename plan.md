# Implementation Plan

Updated: 2026-09-17T23:55:00+03:00

## Verification baseline

`RECEIPT-AUDIT-2026-09-17.md` is the accepted verification baseline. A gate is PASS only with a real
command exit code. Textual source assertions no longer qualify a gate on their own, because that method
allowed four P0 defects to pass every gate from P2 up to P21, and a text match inside a migration file
kept reporting PASS on an invariant the model had stopped declaring.

## Completed

**FIX-P0** — ASGI initialization order, Django messages without sessions, the sessions migration app
label, and the AGP 9 convention plugin plus source sets. The backend boots and serves; Android reaches
dependency resolution.

**P2 connected qualification** — migration convergence, both business invariants proven at the database
level, and the test suite running for the first time.

- INT-009 closed: drift reduced from 13 operations across 7 apps to 4 justified operations across 2.
- INT-028 closed: three missing `tests/__init__.py` markers added; collection works.
- INT-007 closed: test settings inherit PostgreSQL + PostGIS instead of pinning SQLite.
- INT-030 found and fixed: `submit_facility` used `FOR UPDATE` across a LEFT OUTER JOIN, which
  PostgreSQL refuses, so the owner submit endpoint would have failed in every environment.
- The submitted-uniqueness qualifier was hardened from a migration text match to a model-state check,
  and negative-tested.

## Current executable phase

**P10 — Contracts.** This is the next phase and it must precede any further client work.

The generated schema currently carries 72 paths with zero component schemas and zero request bodies, so
every generated client would have no DTOs. Admin, Android and iOS all depend on that boundary, and all
three are blocked behind it.

### P10 task order

1. Add serializers or `@extend_schema` to the endpoints so the schema carries real request and response
   models. 328 errors and 76 warnings are currently emitted by `manage.py spectacular`.
2. Register an `OpenApiAuthenticationExtension` for `BearerAccessTokenAuthentication`.
3. Resolve the five `operationId` collisions; `08-API-CONTRACT` requires stable operation ids.
4. Commit `openapi/schema.yaml` and `openapi/schema.sha256`, and repair the CI drift gate, which today
   diffs an untracked file and therefore can never fail.
5. Generate the TypeScript, Kotlin and Swift clients with the pinned generator.

## Gate

`P10 CONTRACT PASS` — NOT STARTED.

Acceptance is execution-only: `manage.py spectacular` emits a schema with populated
`components.schemas`, the three client packages generate from that schema, and CI reports zero drift
against the committed hash.

## Deferred, recorded, not started

P23 to P26 iOS. Admin data binding for P11 to P13. The P4 province and taxonomy seed. `DutyShift`
field restoration. Every item carries an INT identifier in `RECEIPT-AUDIT-2026-09-17.md`.

## Quality debt

`DECISIONS.md` carries the register. DEBT-001 ruff, DEBT-002 mypy and DEBT-003 the deferred
`PermissionsMixin` evaluation are mandatory before staging or production closure, and none of them
blocks P10. The standing rule is no new lint or type debt in any file that is touched.

## Parallel open gates

- P19 connected staging, waiting on EXT-004 and EXT-005.
- P20 connected end-to-end, security, load and restore.
- P22 signed Android AAB and Play tracks, waiting on EXT-002 and EXT-003.

## Android note

`gradle :app:assembleDebug` remains `NOT_VERIFIED`. Google Maven does not serve this machine, which is
an `ENVIRONMENT_LIMITATION` and not an internal defect. Do not downgrade AGP, change Compose, change
AndroidX versions, change SDK policy, or point the build at an untrusted mirror to make it pass. Android
stays `SOURCE_IMPLEMENTED` until `assembleDebug` exits 0 from a network that can reach the official
Google Maven, or from a trusted artifact proxy the project owns.
