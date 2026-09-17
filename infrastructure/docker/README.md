# Local connected dependencies

When Docker is available:

```bash
docker compose -f infrastructure/docker/compose.yml up -d postgres redis minio
cd apps/backend
uv sync --dev
uv run python manage.py migrate --settings=directory_backend.settings.development
uv run python ../../infrastructure/scripts/backend-runtime-smoke.py
```

The compose credentials are development-only constants and must never be reused for staging or production.
