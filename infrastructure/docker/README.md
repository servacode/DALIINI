# Local stack

One command brings up everything the backend needs, and the backend itself:

```bash
pnpm stack:up      # PostGIS, Redis, S3 (SeaweedFS), the API, a Celery worker and beat
pnpm stack:seed    # operators, an owner, and a directory with something in every section
pnpm dev:admin     # the console against it, on http://localhost:3000
pnpm dev:web       # the public site against it, on http://localhost:3001
pnpm stack:down    # stop; the data stays in named volumes
```

| Service | Address | Notes |
|---|---|---|
| API | http://localhost:8000 | migrations run on start; the source is mounted, so edits reload |
| PostgreSQL + PostGIS | localhost:5432 | `directory` / `directory` |
| Redis | localhost:6379 | |
| S3 | http://localhost:9000 | `directory-public` readable by anyone, never listable; `directory-private` refuses anonymous access |

The seeded operator is `+963900111222` / `OperatorPass123!`, and the limited operator
`+963900333444` / `LimitedPass123!`. Registration codes are not sent anywhere locally:
`docker compose -f infrastructure/docker/compose.yml exec api uv run python manage.py e2e_set_otp`
sets a known one.

Running the backend tests against this stack's database, from `apps/backend`:

```bash
DATABASE_URL=postgresql://directory:directory@localhost:5432/directory uv run pytest
```

Every credential in `compose.yml` is a development-only constant and must never be reused for a
server anyone else can reach.
