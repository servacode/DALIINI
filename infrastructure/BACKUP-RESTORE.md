# Database backup and restore (scripts)

Operational companion to `docs/runbooks/backup-restore.md`, covering the two scripts and
the Render cron job that runs them. Render's managed Postgres snapshots remain the first
line of recovery; these logical dumps are the provider-independent copy.

## Nightly backup

- Render cron `directory-v3-staging-db-backup` (render.yaml) runs daily at 02:17 UTC from
  `infrastructure/backup/Dockerfile` (postgres:17 client + awscli).
- `scripts/db-backup.sh`: `pg_dump -Fc` → gzip → `s3://$BACKUP_S3_BUCKET/$BACKUP_S3_PREFIX/db-<UTC stamp>.dump.gz`
  plus a `.sha256` sidecar, then deletes objects older than `BACKUP_RETENTION_DAYS` (default 14).
- Env: `DATABASE_URL`, `BACKUP_S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
  optional `AWS_ENDPOINT_URL` (R2/MinIO), `AWS_DEFAULT_REGION`, `BACKUP_S3_PREFIX`.
- Use a bucket separate from the media buckets, with credentials scoped to it.
- Verify: the cron's last run is green in Render and the newest object is < 26 h old.
- Each run writes its outcome back to the database it dumped (`health_servicesignal`, row
  `backup`). The console's «حالة النظام» page reads it: green within 26 h of the last
  backup, amber up to 50 h, red after that or when the last attempt failed (DECISION-073).
  The write is best-effort and never fails a backup; a pruning error after the upload does
  not count as a failed backup.

## Restore

`scripts/db-restore.sh` reads only `TARGET_DATABASE_URL` — never `DATABASE_URL` — so the
backup job's environment cannot point a restore at production by accident.

1. Dry run (downloads, checks the sha256 and gzip, lists the archive, changes nothing):
   ```sh
   TARGET_DATABASE_URL=postgres://... BACKUP_S3_BUCKET=... AWS_...=... \
     scripts/db-restore.sh latest
   ```
2. Apply to a fresh or scratch database first, then run the app's smoke checks against it:
   ```sh
   TARGET_DATABASE_URL=postgres://... scripts/db-restore.sh --confirm-overwrite db-20260101T021700Z.dump.gz
   ```
   The restore uses `--clean --if-exists --single-transaction --exit-on-error`: it either
   fully replaces the target's objects or leaves the target untouched.
3. Restoring over a live environment: scale the API, worker and beat to zero first, restore,
   run `python manage.py migrate --check`, then scale back up.

The target needs the PostGIS extension available (Render Postgres 17 has it).

## Restore drill

Restore `latest` into a scratch database at least monthly and record the result, as
`docs/runbooks/backup-restore.md` requires.
