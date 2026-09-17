# Monitoring and Alerting Runbook

Collect structured server logs with request IDs and monitor:
- API availability, HTTP 5xx and latency.
- DB connections and slow queries.
- Redis availability/memory.
- Celery worker heartbeat/queue stall.
- Object-storage failures/latency.
- Admin server/client failures.
- Android crashes, ANRs, API failure classes and map/navigation failures when Sentry/mobile telemetry is connected.

Alerts must be actionable: API down, DB/Redis unavailable, worker stalled, elevated 5xx, backup failure, storage failure, crash spike. Do not alert on noisy non-actionable metrics.

Every release record should include commit, schema hash, migrations, artifact hash, deployment IDs, known issues and rollback target.
