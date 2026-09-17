# Backend Django Implementation Specification

## Bootstrap

Create:
```bash
uv init
uv add Django djangorestframework django-filter django-cors-headers drf-spectacular
uv add "channels[daphne]" channels-redis celery redis "psycopg[binary]" argon2-cffi Pillow "django-storages[s3]"
uv add --dev pytest pytest-django ruff mypy django-stubs
```

Pin exact resolved versions in lockfile.

## Settings split

```text
directory_backend/settings/
  base.py
  development.py
  test.py
  staging.py
  production.py
```

Production validation fails closed if required env vars missing.

## Django apps

Use domain modules listed in architecture.

Each app:
```text
models/
services/
selectors/
serializers/
views/
permissions/
urls.py
tests/
```

## Authentication

Implement custom authentication backend/API layer.

Do not use Django Admin as product UI.

Admin staff roles are domain roles, not a dependency on Django `is_staff` workflows.

## API documentation

drf-spectacular:
- schema.
- Swagger only in dev/staging privileged or disabled in prod per policy.
- operation IDs stable.
- enums explicit.
- error responses documented.

## Geo

Enable:
- `django.contrib.gis`.
- PostGIS extension migration.
- PointField.
- GIST indexes.

Query:
- annotate distance.
- order by distance when coords valid.
- bbox intersection/filter.
- province eligibility first.

## Search

Start with:
- normalized columns or expressions.
- PostgreSQL trigram/full-text where appropriate.
- indexes proven by EXPLAIN.

Do not install external search engine before metrics.

## Availability service

Central service:
```text
get_facility_availability(facility, now)
get_next_open(facility, now)
```

No duplicate logic in serializers/mobile.

## Owner services

Functions/tasks:
- create draft.
- update core.
- update location.
- replace hours.
- manage images.
- upload evidence.
- validate submission.
- submit.
- trigger reverification.
- manage duty.
- manage closure.
- membership.

Every service rechecks membership.

## Admin review service

Transactional approve/reject/suspend/reactivate.

Use `select_for_update` where concurrency matters.

## Media

Validation service:
- verify signature by decoding.
- size and pixel limits.
- re-encode.
- metadata strip.
- deterministic safe extension.
- private/public namespace.

## Realtime

Channels consumer authenticates after connect or via secure subprotocol/header mechanism supported by client/proxy.

Never put access token in URL query where logs may capture it.

## Celery

Tasks should be idempotent where practical.

Retry:
- push.
- outbound messages.
- analytics aggregation.
- media cleanup.

No async task decides transaction truth after response unless product explicitly accepts eventual consistency.

## Throttling

Implement central policies:
- login.
- OTP start.
- OTP verify.
- recovery.
- search abuse.
- uploads.
- admin sensitive endpoints.

Use trusted proxy-aware client IP rules.

## Health

`/health/live/`: process alive.
`/health/ready/`: DB and required runtime ready.

Runtime smoke command checks:
- PostGIS.
- Redis.
- Celery.
- object storage.

## Logging

JSON production logs:
- timestamp.
- level.
- requestId.
- route.
- status.
- duration.
- safe actor id.
- error code.

Never:
- password.
- OTP.
- access/refresh token.
- DB URL.
- storage secret.
- evidence path if sensitive.

## Tests

Backend minimum:
- domain rules.
- permissions.
- IDOR.
- lifecycle.
- concurrency.
- PostGIS.
- media privacy.
- duty overlap.
- availability.
- account/session.
- admin.
- OpenAPI.
