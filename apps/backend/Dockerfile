# Backend image (API, Celery worker and Celery beat all run from it).
# Build context is apps/backend. Dependencies come from uv.lock (--frozen fails on drift)
# and the process runs as the unprivileged `app` user.
# UV_NO_SYNC keeps `uv run` (CMD, Render dockerCommand/preDeployCommand) from re-syncing
# the environment - including dev dependencies - at container start.
FROM python:3.14-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_LINK_MODE=copy \
    UV_FROZEN=1 \
    UV_NO_CACHE=1 \
    UV_NO_SYNC=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends gdal-bin libgdal-dev libgeos-dev curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=ghcr.io/astral-sh/uv:0.10.0 /uv /uvx /bin/
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project
COPY . .
RUN uv sync --frozen --no-dev && chown -R app:app /app

USER app
EXPOSE 8000
# Liveness only (no DB/Redis dependency). Render uses healthCheckPath instead; compose
# disables this for the worker, which serves no HTTP.
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${PORT:-8000}/health/live/" || exit 1
CMD ["uv", "run", "daphne", "-b", "0.0.0.0", "-p", "8000", "directory_backend.asgi:application"]
