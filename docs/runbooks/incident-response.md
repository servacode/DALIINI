# Incident Response Runbook

Severity: SEV0 security/data loss; SEV1 broad outage/core workflow; SEV2 major partial failure; SEV3 minor degradation.

Flow: Detect → Acknowledge → Contain → Mitigate/Rollback → Recover → Verify → Communicate → Postmortem → Prevent recurrence.

## API outage
Check deploy health, `/health/live/`, `/health/ready/`, logs, DB/Redis dependencies; rollback a bad code deploy when schema-compatible.

## DB outage
Stop risky writes, inspect provider state/connections, restore service, then verify migrations and data consistency.

## Redis/worker outage
Treat Channels/Celery as degraded; restore Key Value/worker, verify queue processing and realtime reconnect. REST remains source of truth.

## Object storage outage
Disable/contain affected upload paths if required; never expose private evidence directly; verify S3 health before reopening uploads.

## Credential/signing-key compromise
Rotate the affected secret/provider credential, revoke sessions/tokens when scope demands it, redeploy all consumers, audit access, and document blast radius. Never paste the credential into incident docs.

## Push/map provider failure
Keep core REST discovery usable; surface degraded behavior, stop retry storms, restore provider credentials/endpoints, then verify on a device.
