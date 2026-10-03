# The load test

What the public API does under the traffic the platform expects at launch, and where it gives
way (DECISION-083).

| What | Where |
|---|---|
| The visitors | `public.js` (k6): Home, a section's list and its next page, a facility, the map around them, a search, tonight's duty roster, with a few seconds between screens |
| The data | `manage.py seed_load_directory --facilities 5000`: made-up facilities in all fourteen provinces, with opening hours and a duty roster. It refuses a database that holds any facility it did not make |
| In CI | the `production-stack` job: after the smoke, 5,000 facilities and 40 visitors for a minute through Caddy (`SMOKE_LOAD=1 infrastructure/production/smoke.sh`) |

Each screen has its own limit, so a slow map cannot hide behind fast lists:

| Screen | p95 |
|---|---|
| Home | 800 ms |
| List | 600 ms |
| Facility | 500 ms |
| Map | 1000 ms |
| Search | 800 ms |
| Duty roster | 600 ms |

Every screen together: p99 under 2 s, fewer than 1% failed requests.

## Against staging

On a staging server whose database holds only load data, never real facilities:

```sh
docker compose --env-file /srv/daliini/staging.env exec api \
  uv run python manage.py seed_load_directory --facilities 5000
docker run --rm --network host -v "$PWD/infrastructure/load:/load:ro" grafana/k6:1.3.0 run \
  -e API=https://api.staging.<ROOT> -e VUS=40 -e HOLD=5m /load/public.js
```

The per-address limits apply on staging as everywhere. From one machine, 40 visitors stay under
`THROTTLE_ANON_DEFAULT` (600 a minute), but searches pass `THROTTLE_SEARCH` (120 a minute).
Either raise `THROTTLE_SEARCH` in staging's env file for the run, or read the 429s as the
throttle working. Never run it against production's data.

## What it found (2026-10-03, 5,000 facilities)

- **The map ran two queries per marker**: a thousand for a full viewport, 1.1 s. Search ran four
  per row. Both now take their open and duty flags from the one query, like the list. The map
  builds its markers from five columns instead of whole facilities: 172 ms became 32 ms, in one
  query.
- **Connections ran out under load.** Under ASGI each request runs in its own thread, and
  `CONN_MAX_AGE=60` kept each thread's connection open after its request. At 120 visitors
  PostgreSQL refused new clients ("too many clients already"), and half the requests failed.
  Django's own connection pool now bounds them per process: `DB_POOL_MIN_SIZE`,
  `DB_POOL_MAX_SIZE` (8) and `DB_POOL_TIMEOUT`.
- **With those fixed**, on 4 shared cores with 2 API workers:
  - 40 visitors (14 requests a second): every screen's p95 between 250 and 450 ms, and nothing
    failed;
  - 120 visitors (34 requests a second): nothing failed, PostgreSQL held 16 connections (two
    workers at 8 each), and each screen's p95 was 1.5 to 1.9 s.

  At 120 the workers' CPU is the limit, not the database. On a server with more cores,
  `WEB_CONCURRENCY` adds workers.
