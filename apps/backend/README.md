# Backend

Django 5.2 LTS modular-monolith backend for Directory Platform V3.

## Local bootstrap

```bash
uv sync
uv run python manage.py check --settings=directory_backend.settings.test
uv run pytest
```

Connected development uses PostgreSQL 17 + PostGIS, Redis and S3-compatible storage from `infrastructure/docker/compose.yml` when Docker is available.
