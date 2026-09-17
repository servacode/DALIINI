# Backup and Restore Runbook

## Targets
The design target is RPO <= 1 hour and RTO <= 4 hours where the selected provider plan supports it. These targets are not considered verified until a timed restore drill passes.

## Database
- Enable managed automated backups; enable PITR if the selected production plan supports it.
- Before risky migrations, record a recoverable restore point.
- Never store backup credentials in Git.

## Restore drill
1. Create a disposable isolated database.
2. Restore the selected backup/PITR point.
3. Enable/verify PostGIS before application checks.
4. Apply only migrations appropriate for the restored revision.
5. Run Django system checks and consistency queries.
6. Run critical API smoke against an isolated app instance.
7. Record start/end time, achieved RPO/RTO, backup identifier and evidence.
8. Destroy disposable resources after evidence is retained.

## Object storage
Use separate public/private staging buckets. Production private evidence requires versioning/retention and a tested recovery procedure appropriate to the storage provider.
