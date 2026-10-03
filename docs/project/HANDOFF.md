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
#27 to #40, phase 4 through #41 to #44, and phase 5's code through #45 to #48. Phase 6 is
under way:

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

**Now: phase 6, the production server.** The stack is ready and booted in CI on every pull
request (`infrastructure/production/`); a real server waits on EXT-007. Next: deploys from
GitHub, backups with a restore drill and monitoring (6.2), then self-hosted map tiles and routing
(6.3). After that, content and the Play launch (7), and the iPhone app from the Android code with
Kotlin Multiplatform (8). An owner portal on the web was offered at the start of phase 3 and set
aside: owners use the Android app.

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
