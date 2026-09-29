#!/usr/bin/env bash
# Logical PostgreSQL backup to an S3-compatible bucket, with retention pruning.
#
# Required env:
#   DATABASE_URL            postgres connection string to dump
#   BACKUP_S3_BUCKET        target bucket name (no s3:// prefix)
#   AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
# Optional env:
#   AWS_ENDPOINT_URL        S3-compatible endpoint (R2, MinIO, ...); unset = AWS S3
#   AWS_DEFAULT_REGION      defaults to "auto"
#   BACKUP_S3_PREFIX        key prefix, default "postgres"
#   BACKUP_RETENTION_DAYS   delete objects older than this, default 14 (0 = keep all)
#
# The dump is pg_dump custom format (-Fc), which is already compressed internally, then
# gzip'd for the transfer. A .sha256 sidecar is uploaded next to it so a restore can
# verify integrity before touching the target database.
# Runbook: infrastructure/BACKUP-RESTORE.md
set -euo pipefail

: "${DATABASE_URL:?DATABASE_URL is required}"
: "${BACKUP_S3_BUCKET:?BACKUP_S3_BUCKET is required}"
: "${AWS_ACCESS_KEY_ID:?AWS_ACCESS_KEY_ID is required}"
: "${AWS_SECRET_ACCESS_KEY:?AWS_SECRET_ACCESS_KEY is required}"
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-auto}"
PREFIX="${BACKUP_S3_PREFIX:-postgres}"
PREFIX="${PREFIX%/}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

aws_s3() {
  if [[ -n "${AWS_ENDPOINT_URL:-}" ]]; then
    aws --endpoint-url "$AWS_ENDPOINT_URL" s3 "$@"
  else
    aws s3 "$@"
  fi
}

log() { printf '%s [db-backup] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
name="db-${stamp}.dump.gz"
file="$workdir/$name"

log "dumping database"
pg_dump --format=custom --no-owner --no-privileges --dbname="$DATABASE_URL" | gzip -c > "$file"
# A truncated or empty dump must never replace a good one in the retention window.
gzip -t "$file"
if [[ ! -s "$file" ]]; then
  log "ERROR: dump is empty"
  exit 1
fi
(cd "$workdir" && sha256sum "$name" > "$name.sha256")

dest="s3://${BACKUP_S3_BUCKET}/${PREFIX}"
log "uploading $name ($(du -h "$file" | cut -f1)) to $dest/"
aws_s3 cp --only-show-errors "$file" "$dest/$name"
aws_s3 cp --only-show-errors "$file.sha256" "$dest/$name.sha256"

if [[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] && (( RETENTION_DAYS > 0 )); then
  cutoff="$(date -u -d "-${RETENTION_DAYS} days" +%Y%m%dT%H%M%SZ)"
  log "pruning backups older than $RETENTION_DAYS days (before $cutoff)"
  # Names embed a sortable UTC stamp, so pruning compares names rather than trusting
  # the provider's LastModified semantics.
  aws_s3 ls "$dest/" | awk '{print $4}' | while read -r key; do
    [[ "$key" =~ ^db-([0-9]{8}T[0-9]{6}Z)\.dump\.gz(\.sha256)?$ ]] || continue
    if [[ "${BASH_REMATCH[1]}" < "$cutoff" ]]; then
      log "deleting $key"
      aws_s3 rm --only-show-errors "$dest/$key"
    fi
  done
fi

log "done: $dest/$name"
