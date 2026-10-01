#!/usr/bin/env bash
# Restore a backup made by scripts/db-backup.sh into a target database.
#
# Usage:
#   TARGET_DATABASE_URL=postgres://... scripts/db-restore.sh [--confirm-overwrite] <backup>
#
#   <backup>  "latest", an object name (db-20260101T021700Z.dump.gz), or a local file path
#
# Required env: TARGET_DATABASE_URL, plus BACKUP_S3_BUCKET and AWS_* for remote backups
# (same variables as db-backup.sh). The source DATABASE_URL is deliberately NOT read, so
# a restore can never hit production by inheriting the backup job's environment.
#
# Without --confirm-overwrite the script verifies and lists the dump and stops. With it,
# pg_restore --clean --if-exists replaces existing objects in the target database.
# Runbook: infrastructure/BACKUP-RESTORE.md
set -euo pipefail

confirm=0
backup=""
for arg in "$@"; do
  case "$arg" in
    --confirm-overwrite) confirm=1 ;;
    -h|--help) sed -n '2,15p' "$0"; exit 0 ;;
    *) backup="$arg" ;;
  esac
done

: "${TARGET_DATABASE_URL:?TARGET_DATABASE_URL is required}"
[[ -n "$backup" ]] || { echo "usage: $0 [--confirm-overwrite] <latest|object-name|file>" >&2; exit 2; }
export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-auto}"
PREFIX="${BACKUP_S3_PREFIX:-postgres}"
PREFIX="${PREFIX%/}"

aws_s3() {
  if [[ -n "${AWS_ENDPOINT_URL:-}" ]]; then
    aws --endpoint-url "$AWS_ENDPOINT_URL" s3 "$@"
  else
    aws s3 "$@"
  fi
}

log() { printf '%s [db-restore] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

if [[ -f "$backup" ]]; then
  file="$backup"
  log "using local file $file"
else
  : "${BACKUP_S3_BUCKET:?BACKUP_S3_BUCKET is required for remote backups}"
  src="s3://${BACKUP_S3_BUCKET}/${PREFIX}"
  if [[ "$backup" == "latest" ]]; then
    backup="$(aws_s3 ls "$src/" | awk '{print $4}' | grep -E '^db-[0-9]{8}T[0-9]{6}Z\.dump\.gz$' | sort | tail -n 1)"
    [[ -n "$backup" ]] || { log "ERROR: no backups under $src/"; exit 1; }
  fi
  file="$workdir/$backup"
  log "downloading $src/$backup"
  aws_s3 cp --only-show-errors "$src/$backup" "$file"
  if aws_s3 cp --only-show-errors "$src/$backup.sha256" "$file.sha256" 2>/dev/null; then
    (cd "$workdir" && sha256sum -c "$backup.sha256")
  else
    log "WARNING: no checksum sidecar found; continuing on gzip integrity check only"
  fi
fi

gzip -t "$file"
dump="$workdir/restore.dump"
gzip -dc "$file" > "$dump"
# Proves the archive is a readable pg_dump custom-format file before anything is dropped.
objects="$(pg_restore --list "$dump" | grep -cv '^;' || true)"
log "archive OK: $objects TOC entries"

target_host="$(printf '%s' "$TARGET_DATABASE_URL" | sed -E 's#^[a-z]+://([^@]*@)?([^/?]+).*#\2#')"
if (( ! confirm )); then
  log "dry run: would restore into host '$target_host'. Re-run with --confirm-overwrite to apply."
  exit 0
fi

log "restoring into host '$target_host' (--clean --if-exists, single transaction)"
pg_restore --clean --if-exists --no-owner --no-privileges --single-transaction \
  --exit-on-error --dbname="$TARGET_DATABASE_URL" "$dump"
log "restore complete"
