# Infrastructure — Development, Staging and Production

## Baseline provider architecture

Recommended initial managed stack:

```text
Cloudflare DNS
        |
        +--> www.<domain>   → Render Next.js Public Web
        +--> admin.<domain> → Render Next.js Admin
        +--> api.<domain>   → Render Django ASGI
                                   |
                 +-----------------+------------------+
                 |                 |                  |
          Render Postgres     Render Redis      Cloudflare R2
             + PostGIS            |             S3-compatible
                                  |
                             Celery worker
```

Monitoring:
- Sentry.
- Render metrics/logs.
- optional external uptime check.

## Development

Docker Compose:
- PostGIS.
- Redis.
- MinIO optional S3 emulator.
- backend.
- Celery.
- Admin.
- Web.

Mobile runs native outside Docker.

## Staging

Separate:
- DB.
- Redis.
- storage bucket.
- secrets.
- FCM credentials if possible.
- hostname.

Suggested:
```text
staging-api.<ROOT_DOMAIN>
staging-admin.<ROOT_DOMAIN>
staging-www.<ROOT_DOMAIN>
```

If root domain not yet purchased, use provider hostnames **only for Staging**.

## Production

Paid service plans only.

Requirements:
- production secret set.
- DEBUG false.
- allowed hosts exact.
- secure cookies.
- CORS exact.
- private storage.
- DB backups.
- worker health.
- TLS.
- monitoring.

## PostgreSQL

PostgreSQL 17 + PostGIS.

Production requirements:
- automated managed backups.
- PITR if plan supports and chosen.
- connection limits.
- migration runbook.
- restore drill.

## Redis

Use paid persistent/reliable service for:
- Channels.
- Celery broker.
- caching.
- throttling coordination.

Eviction policy selected with care; channel/broker critical data must not be unpredictably evicted.

## Object storage

Cloudflare R2 baseline.

Buckets/prefixes:
- public.
- private evidence/profile.
- backups maybe separate.

Use server-side credentials only.

## Deploy sequence

```text
1. CI green
2. backup
3. run migration compatibility precheck
4. deploy backend
5. migrate
6. runtime smoke
7. deploy worker
8. deploy admin/web
9. E2E smoke
10. mobile rollout if release
```

For destructive migration, use expand/contract pattern.

## Rollback

App code rollback should not assume DB downgrade is always safe.

Prefer backward-compatible migrations.

## Capacity

Before Production measure:
- API latency.
- DB connections.
- Redis memory.
- Celery queue.
- storage latency.
- image bandwidth.

No Kubernetes initially.
