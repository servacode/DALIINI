# HANDOFF

Last updated: 2026-10-03

What someone picking this project up needs first. The plan itself is `ROADMAP.md`; why each
thing is the way it is, `DECISIONS.md`; what waits on the owner, `BLOCKERS.md`. The handoff this
replaces, which described the project's first build-out, is `docs/archive/HANDOFF-2026-09-19.md`.

## What this is

«دليني» (DALIINI): a directory of places in Syria (pharmacies first, with the night-duty roster),
where owners keep their own listing and operators verify it. The launch is Raqqa, pharmacies and
duty. Provinces and categories are switched on from the console, not by rebuilding a client. The
scope is places only: nothing about medicines (DECISION-054).

## Where things stand

The roadmap was approved on 2026-10-03. Phases 0 to 2 are done, merged through pull requests
#27 to #37 (and the one that closes phase 2):

| Phase | What it delivered | Decisions |
|---|---|---|
| 0 | confirmed bugs fixed, search that folds Arabic spelling, one-command dev stack, crest removed | 052–059 |
| 1 | identity v2 (emerald, gold for duty, sand; light and dark) on the site, console and Android | 060 |
| 2.1 | Uvicorn, default throttles, 404 for non-members, constant-query lists, health probe | 061 |
| 2.2 | cursor paging on every console list, operators create and edit facilities | 062 |
| 2.3 | an owner's edit to a live facility is reviewed without taking it down | 063 |
| 2.4 | claiming an ownerless facility, inviting members by phone | 064 |
| 2.5 | a second sign-in step (TOTP) for every operator | 065 |
| 2.6 | duty rosters from CSV/XLSX and rotations, previewed before writing | 066 |
| 2.7 | readable facility slugs, advertisement numbers, notification preferences | 067 |
| 2.8 | end-to-end suites and the phone stack on compose, anywhere | 068 |
| 2.9 | `admin_console/views.py` split by domain; this handoff and the blocker register rewritten | — |

**Next: phase 3, the public website.** At its start the owner is to be offered the choice of an
owner portal on the web. Then the console v2 (4), Android v2 (5), the VPS (6), content and the
Play launch (7), and the iPhone app from the Android code with Kotlin Multiplatform (8).

Server work that Android has not caught up with yet, all planned for phase 5:
- the «تعديلاتك بانتظار المراجعة» banner (`pendingChange`);
- the claim and invitation screens;
- syncing the notification switches with `account/notification-preferences/`.

## The repository

| Path | What |
|---|---|
| `apps/backend` | Django 5.2, DRF, GeoDjango/PostGIS, Channels, Celery; served by Uvicorn |
| `apps/admin` | the operators' console: Next.js 16, a server-only BFF in front of the API |
| `apps/web` | the public site: Next.js |
| `apps/android` | Kotlin and Compose, on the generated Kotlin client |
| `apps/ios` | empty until phase 8 |
| `services/whatsapp-bot` | delivers registration codes over WhatsApp (DECISION-052) |
| `openapi/` | the contract, generated from the backend and committed with its hash |
| `packages/api-*` | the TypeScript, Kotlin and Swift clients generated from it |
| `packages/design-tokens` | identity v2, generated into each platform |
| `infrastructure/docker` | the development stack, and the suites' own stack beside it |
| `scripts/` | stack, end-to-end, contract, map and backup scripts |
| `docs/spec` | the original specification; checksummed, not edited |

## Running it

```bash
pnpm stack:up && pnpm stack:seed   # PostGIS, Redis, SeaweedFS, API, worker, beat; fixtures
pnpm dev:admin                     # console on http://localhost:3000 (+963900111222 / OperatorPass123!)
pnpm dev:web                       # site on http://localhost:3001
scripts/local-stack.sh up          # the same, prepared for a phone; then scripts/android-link.sh
```

`infrastructure/docker/README.md` has the addresses, accounts and the backend tests against
the stack's database.

## Checking it

| What | Command |
|---|---|
| backend | in `apps/backend`: `uv run ruff check .`, `uv run mypy .`, `uv run pytest`, `uv run python manage.py makemigrations --check --dry-run` (GDAL needed; `scripts/verify.sh backend` runs them in the stack's container instead) |
| contract | `scripts/check-openapi-drift.sh` |
| console | in `apps/admin`: `npx tsc --noEmit`, `npx eslint .`, `npx vitest run` |
| console in a browser | `scripts/e2e-admin.sh` (its own stack, production build) |
| the whole cycle | `scripts/e2e-android.sh`, or `scripts/verify-all.sh` for everything |
| Android | the **Android Build Verification** workflow, started by hand on the branch (`workflow_dispatch`); `ci.yml` does not build the app |

CI (`ci.yml`, `codeql.yml`, `security.yml`) runs on every pull request. A change that touches
Android, or the contract Android consumes, is not done until that workflow is green on its branch.

## How work is done here

- **Contract first.** A change to the API is made in the backend, then
  `scripts/generate-openapi.sh` and `scripts/generate-api-clients.sh`. The schema, its hash and all
  three clients are committed together; CI fails on drift. The contract tests reject a request body
  that requires a read-only field, and a DRF field named after a `Serializer` attribute (`fields`,
  `errors`).
- **Errors** are one envelope, `{code, message, details, requestId}`. Domain refusals raise
  `DomainError` or `ConflictError`, never a bare exception whose text could reach a client.
- **The console** reaches the API only through `lib/api/operations.ts` (`READS` over GET,
  `WRITES` over POST, each with an allowlist of parameters). Uploads go through dedicated
  same-origin, size-bounded routes.
- **No production secret is committed.** Development credentials are constants, marked as such.
- **Formatting:** Ruff format is not enforced across the backend. Format only the files you
  create; never run it over the whole tree. Shell scripts are LF (`.gitattributes`).
- **Every decision** that shapes behaviour gets a `DECISION-NNN` entry and a line in the roadmap.

## Hosting

Render is not used (owner's decision). `render.yaml` and `render.production.yaml` remain only
until phase 6 replaces them with the VPS setup: Docker Compose, Caddy and Cloudflare. A test
still covers their start-up command until then.
