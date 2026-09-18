# Implementation Plan

Updated: 2026-09-19 (Android binding)

## Verification baseline

`RECEIPT-AUDIT-2026-09-17.md` is the accepted verification baseline. A gate is PASS only
with a real command exit code. Textual source assertions no longer qualify a gate on their
own, and a schema is not qualified by reading its YAML: a representative sample of
operations must be exercised against the running service and validated against the
document. A web screen is not qualified until it has run in a real browser — the Admin's
CSP fault (INT-045) was invisible to every HTTP-level check.

## Completed

**FIX-P0** — ASGI initialization order, Django messages without sessions, the sessions
migration app label, and the AGP 9 convention plugin plus source sets.

**P2 connected qualification** — migration convergence, both business invariants proven at
the database level, and the test suite running for the first time. `CONNECTED_VERIFIED`.

**P10 contracts recovery** — a real generated contract, three generated clients and a drift
gate that can fail. `CONNECTED_VERIFIED`.

**Contract alignment** — one error envelope, one cursor envelope, camelCase throughout,
`sequence` on the wire for business hours.

**Launch baseline** — fourteen provinces and the health taxonomy seeded by migration with
deterministic ids and non-destructive re-runs. P4 `CONNECTED_VERIFIED`.

**P11–P13 Admin data binding and golden path** — the Admin went from thirteen static pages
to a working console on the real backend. Sign-in, refresh, sign-out, `/admin/me/`,
permission-aware navigation, and every operational page bound through one BFF route and
the generated client. Cycle J runs from the Admin. 31 Playwright tests against a production
build, Django, PostGIS and Redis; 71 Vitest tests; 211 backend tests. P12 and P13
`CONNECTED_VERIFIED`.

**Android generated-client integration (§1–§48)** — the data layer runs on the generated
client, verified on the JVM against the real backend: 26 connected and 89 unit tests. The
app itself is not built. Fourteen defects were fixed on the way, nine of them in the contract
or the backend, and three more are recorded open (INT-048 to INT-064).

## Next executable phase

The rest of the Android brief, from §49. The Android build still needs a network that reaches
Google Maven, or a CI runner (`ENVIRONMENT_LIMITATION`); `BUILD_VERIFIED` waits on it.

## Gate status

| Gate | Status |
|---|---|
| `P12 ADMIN FOUNDATION` | CONNECTED_VERIFIED |
| `P13 ADMIN GOLDEN PATH` | CONNECTED_VERIFIED — see the one NOT_VERIFIED path below |

Not verified end to end: streaming an evidence file to an authorised operator. The e2e
stack runs no object storage, so no file exists to stream. The refusal, the 404 and the
absence of any storage key on the page are verified; the allowed path with real bytes needs
MinIO in the e2e stack.

## Deferred, recorded, not started

P23 to P26 iOS. `DutyShift` field restoration (INT-008). Category `description_ar/en`,
named in `06-DATA-MODEL.md` and absent from the model. `apps/web` HSTS (INT-024) and its
tsconfig rewrite (INT-023). LAUNCH_POLICY_PENDING — the pharmacy verification policy is a
production launch gate; the tool to configure it now exists. Every item carries an INT
identifier in `RECEIPT-AUDIT-2026-09-17.md`.

## Quality debt

`DECISIONS.md` carries the register. DEBT-001 ruff at 100, from 106 at intake. DEBT-002 mypy
at 783 in the backend container, against a 797 baseline measured the same way. DEBT-003 the
deferred `PermissionsMixin` evaluation. The standing rule is no new lint or type debt in a
touched file; every module added since P10 is strict-clean.

## Contract discipline

Django and DRF are the source of truth; `openapi/schema.yaml` is generated from them and
the three clients are generated from it. After any change that touches a request or
response, run `./scripts/generate-openapi.sh` and `./scripts/generate-api-clients.sh` and
commit the regenerated artefacts with the source change. No consumer may hand-write a
transport DTO. See `openapi/README.md` and DECISION-010 onwards.

## Running the Admin golden path

```bash
./scripts/e2e-admin.sh            # resets the test database, starts Django, builds and
                                  # serves the Admin, runs Playwright
cd apps/admin && pnpm test        # Vitest
```

The runner expects the `p10pg` PostGIS and `p10redis` containers on the `p10net` network.

## Parallel open gates

- P19 connected staging, waiting on EXT-004 and EXT-005.
- P20 connected end-to-end, security, load and restore.
- P22 signed Android AAB and Play tracks, waiting on EXT-002 and EXT-003.
