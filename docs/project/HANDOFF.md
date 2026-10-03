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

The roadmap was approved on 2026-10-03. Phases 0 to 3 are done, merged through pull requests
#27 to #40, phase 4 through #41 to #44, phase 5's code through #45 to #48, and phase 6's
through #49 to #52. Phase 7's code is done, and phase 8 has begun:

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
| 3.1 | the site's readable addresses, theme switch, search engines | 069 |
| 3.2–3.3 | a home page that answers, compact cards, a facility page with its photos | 070 |
| 3.4 | the site's map, the app's own style drawn by MapLibre | 071 |
| 4.1 | the role editor, the owner role, `grant_operator` for the first operator | 072 |
| 4.2 | a system page that asks each service, and records for those that cannot be asked | 073 |
| 4.3 | tables that sort from their headers and remember columns, density and page size | 074 |
| 4.4 | daily charts, status bars, and the facility map on the platform's own style | 075 |
| 5.1 | Android: invitations by phone both ways, «هذه منشأتي», the edit-under-review banner | 076 |
| 5.2 | Android: notice switches kept by the account, a newer build offered once | 077 |
| 5.3 | Android: a trip keeps its readings and voice with the screen locked | 078 |
| 5.4 | Android: build numbers from CI, a baseline profile, map motion that follows the system | 079 |
| 6.1 | the production stack on one server behind Caddy; Render retired | 080 |
| 6.2 | release images on GHCR, deploys over SSH started by a person, hourly backups with a monthly restore drill | 081 |
| 6.3 | the map's tiles, Arabic glyphs and routing served from `maps.<ROOT>`, rebuilt from OpenStreetMap | 082 |
| 6.4 | a load test at launch scale in CI; the map and search made constant-query; a database pool | 083 |
| 7.1 | the second draft of the legal and help pages, the Play listing and feature graphic, the launch runbook | 084 |
| 8.1 | a Kotlin Multiplatform convention; observability and analytics shared and tested on the iPhone simulator | 085 |
| 8.2 | the models shared: Damascus dates on kotlinx-datetime, the site's links read in common code | 086 |
| 8.3 | the session, cache, preferences and location shared, Android's parts in androidMain; `@Inject` usable in common code | 087 |

**Now: phase 8, the iPhone app from the Android code** (DECISION-051). The shared layers move
into Kotlin Multiplatform one module at a time, each step leaving the Android app green and
unchanged; the models and the core layers are shared, and the network layer is next (ROADMAP ٨).
Everything else that remains waits on the owner: the server and domain (EXT-007, EXT-001),
approving the launch texts and graphic, the Play account (EXT-003), and then the closed test and
the public release, step by step in `docs/runbooks/launch.md`. An owner portal on the web was
offered at the start of phase 3 and set aside: owners use the Android app.

Android has caught up with every server feature, a trip survives a locked screen, and builds
number themselves. What remains of phase 5 needs a real phone: a full check on the device, a road
test with the screen locked, and a generated baseline profile.

## The repository

| Path | What |
|---|---|
| `apps/backend` | Django 5.2, DRF, GeoDjango/PostGIS, Channels, Celery; served by Uvicorn |
| `apps/admin` | the operators' console: Next.js 16, a server-only BFF in front of the API |
| `apps/web` | the public site: Next.js |
| `apps/android` | Kotlin and Compose, on the generated Kotlin client |
| `apps/ios` | empty: the iPhone app's shell comes later in phase 8; its shared code lives in the Android modules that are multiplatform |
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
| Android | the **Android Build Verification** workflow, started by hand on the branch (`workflow_dispatch`); `ci.yml` does not build the app. Its `ios-shared` job runs the multiplatform modules' tests on the iPhone simulator |

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

**The first operator** of a new deployment: register the number in the app, then on the server
run `manage.py grant_operator 09XXXXXXXX`. It gives that account the owner role («مدير المنصة»,
every permission); everyone after is appointed from the console. `--create --name "…"` makes the
account instead, asking for the password at a prompt (DECISION-072).

Production is one VPS running `infrastructure/production/compose.yml` behind Caddy and
Cloudflare (DECISION-080). Bringing a server up is `infrastructure/production/README.md`; each
deploy after is `docs/runbooks/deploy.md`. CI boots the whole stack on every pull request
(`production-stack`). Render is not used (owner's decision), and its files are gone.

A release is five images on GHCR tagged `<environment>-<commit>` (**Release images** workflow),
and a deploy is `infrastructure/production/deploy.sh` run on the server by the **Deploy**
workflow over SSH. Both are started by hand, and rollback is a deploy of the previous commit
(DECISION-081). Backups run hourly, and a restore drill runs on the first Sunday of each month
and writes evidence to `/srv/daliini/evidence/`. The system page watches the disk; an outside
uptime service watches the server (`docs/runbooks/monitoring.md`).

The base map and routing come from the server itself, under `maps.<ROOT>` (DECISION-082).
Martin serves the tiles, the Arabic glyphs, the icons and the style; Valhalla serves routes, by
POST only. `infrastructure/production/map/build-map.sh` rebuilds both from OpenStreetMap
monthly. The style's one source is `maps/raqqa.style.json`; `bind-style.py` points it at a
host. `map-labels` (CI) and the **Map build** workflow draw it in a browser and fail when
Arabic names are not joined or not placed.

The public API is load-tested in CI (`infrastructure/load/`, DECISION-083): 5,000 made-up
facilities (`seed_load_directory`, which refuses real data) and 40 visitors through Caddy, each
screen held to its own p95. Connections come from Django's pool, one per process
(`DB_POOL_MAX_SIZE`); never set `CONN_MAX_AGE` back above 0 under ASGI.
