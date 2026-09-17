# Operations, Monitoring, Backup and Incident Response

## Monitoring

Server:
- HTTP 5xx rate.
- latency.
- DB connections.
- slow queries.
- Redis.
- Celery queue/heartbeat.
- storage errors.

Mobile:
- crashes.
- ANRs Android.
- app version.
- failed API categories.
- map/navigation failures.

Admin:
- client/server errors.

## Alerts

Alert only actionable signals:
- API down.
- DB unavailable.
- Redis unavailable where critical.
- Celery queue stalled.
- elevated 5xx.
- backup failure.
- storage failure.
- crash spike.

## Backup

Production DB:
- provider automated backups.
- PITR if available/approved.
- additional logical backup schedule if needed.

Suggested target:
```text
RPO <= 1 hour where provider supports
RTO <= 4 hours
```

These targets must be validated against selected production plan.

Object storage:
- versioning/retention.
- backup strategy for private evidence and public media.

## Restore drill

At least before production and periodically:
1. create separate disposable DB.
2. restore backup.
3. run migrations if appropriate.
4. run consistency checks.
5. run smoke query.
6. record evidence.

## Incident severity

SEV0:
- catastrophic security/data loss.

SEV1:
- app/API broadly unavailable/core workflow unusable.

SEV2:
- major partial failure.

SEV3:
- minor degradation.

## Incident flow

```text
Detect
→ Acknowledge
→ Contain
→ Mitigate/Rollback
→ Recover
→ Verify
→ Communicate
→ Postmortem
→ Prevent recurrence
```

## Runbooks

Create:
- API outage.
- DB outage.
- Redis outage.
- worker outage.
- object storage outage.
- bad deploy rollback.
- compromised credential.
- signing key incident.
- push provider failure.
- map provider failure.
- backup restore.

## Maintenance

Dependency updates:
- monthly routine review.
- urgent security patch fast path.
- quarterly architecture health review.

## Release records

Every production release records:
- commit.
- schema hash.
- migrations.
- artifact hash.
- deployment IDs.
- release notes.
- known issues.
- rollback.
