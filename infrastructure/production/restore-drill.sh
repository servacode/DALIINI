#!/usr/bin/env bash
# The restore drill (DECISION-081): restore the newest backup into a throwaway database, prove it
# is whole, and record how long it took and how much would have been lost.
#
#   infrastructure/production/restore-drill.sh /srv/daliini/production.env
#
# Run monthly by systemd/daliini-restore-drill.timer, and by hand before trusting a new server.
# It never touches the running stack: the database it restores into is its own container, on its
# own network, removed at the end. The evidence lands in /srv/daliini/evidence/ and is checked by
# infrastructure/quality/restore_evidence.py against the targets (RPO <= 60 min, RTO <= 4 h).
set -euo pipefail

env_file="${1:?usage: restore-drill.sh <env-file>}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
evidence_dir="${EVIDENCE_DIR:-/srv/daliini/evidence}"
mkdir -p "$evidence_dir"

set -a
# shellcheck disable=SC1090
. "$env_file"
set +a
release="${RELEASE:-local}"
registry="${IMAGE_REGISTRY:-}"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
network="daliini-drill-$stamp"
db="daliini-drill-db-$stamp"
password="$(openssl rand -hex 24)"
target="postgresql://drill:$password@$db:5432/drill"
started="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
started_s="$(date -u +%s)"
log() { printf '%s [restore-drill] %s\n' "$(date -u +%H:%M:%S)" "$*"; }

cleanup() {
  docker rm -f "$db" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker network create "$network" >/dev/null
docker run -d --name "$db" --network "$network" \
  -e POSTGRES_DB=drill -e POSTGRES_USER=drill -e POSTGRES_PASSWORD="$password" \
  postgis/postgis:17-3.5 >/dev/null
for _ in $(seq 1 60); do
  docker exec "$db" pg_isready -U drill -d drill >/dev/null 2>&1 && break
  sleep 2
done

backup_image="${registry}daliini-backup:$release"
run_backup_image() {
  docker run --rm --network "$network" \
    -e TARGET_DATABASE_URL="$target" \
    -e BACKUP_S3_BUCKET="$BACKUP_S3_BUCKET" -e BACKUP_S3_PREFIX="${BACKUP_S3_PREFIX:-daliini/postgres}" \
    -e AWS_ACCESS_KEY_ID="$BACKUP_S3_ACCESS_KEY_ID" -e AWS_SECRET_ACCESS_KEY="$BACKUP_S3_SECRET_ACCESS_KEY" \
    -e AWS_ENDPOINT_URL="$BACKUP_S3_ENDPOINT_URL" -e AWS_DEFAULT_REGION="${BACKUP_S3_REGION:-auto}" \
    "$backup_image" "$@"
}

log "restoring the newest backup into $db"
restore_log="$(run_backup_image db-restore.sh --confirm-overwrite latest 2>&1)"
printf '%s\n' "$restore_log"
backup_id="$(grep -oE 'db-[0-9]{8}T[0-9]{6}Z\.dump\.gz' <<<"$restore_log" | head -n 1)"
[ -n "$backup_id" ] || { log "could not tell which backup was restored"; exit 1; }

# How much would have been lost: the time between that backup and the start of the drill.
backup_time="$(sed -E 's/^db-([0-9]{4})([0-9]{2})([0-9]{2})T([0-9]{2})([0-9]{2})([0-9]{2})Z.*/\1-\2-\3T\4:\5:\6Z/' <<<"$backup_id")"
rpo_minutes=$(( (started_s - $(date -u -d "$backup_time" +%s)) / 60 ))

psql_drill() { docker exec "$db" psql -U drill -d drill -tAc "$1"; }

# The database: the schema is the one this release expects, and the data is there.
database_pass=true
backend_image="${registry}daliini-backend:$release"
if ! docker run --rm --network "$network" \
  -e DJANGO_SETTINGS_MODULE=directory_backend.settings.development \
  -e DATABASE_URL="$target" -e REDIS_URL=redis://unused:6379/0 \
  "$backend_image" uv run python manage.py migrate --check --noinput; then
  log "the restored schema does not match this release's migrations"
  database_pass=false
fi
provinces="$(psql_drill 'select count(*) from locations_province')"
facilities="$(psql_drill 'select count(*) from facilities_facility')"
users="$(psql_drill 'select count(*) from accounts_user')"
log "restored: $provinces provinces, $facilities facilities, $users accounts"
[ "${provinces:-0}" -gt 0 ] || { log "no provinces in the restored database"; database_pass=false; }

# The photographs the restored rows point at are still in the media bucket (a sample of them).
storage_pass=true
keys="$(psql_drill 'select storage_key from facilities_facilityimage order by random() limit 5')"
for key in $keys; do
  if ! docker run --rm \
    -e AWS_ACCESS_KEY_ID="$S3_ACCESS_KEY_ID" -e AWS_SECRET_ACCESS_KEY="$S3_SECRET_ACCESS_KEY" \
    -e AWS_DEFAULT_REGION="${S3_REGION:-auto}" --entrypoint aws "$backup_image" \
    --endpoint-url "$S3_ENDPOINT_URL" s3api head-object --bucket "$S3_PUBLIC_BUCKET" --key "$key" \
    >/dev/null 2>&1; then
    log "photograph missing from the media bucket: $key"
    storage_pass=false
  fi
done

# The smoke: the restored data answers the queries the site makes first.
smoke_pass=true
[ "$(psql_drill "select count(*) from locations_province where code = 'raqqa'")" = 1 ] || smoke_pass=false

completed="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
rto_minutes=$(( ($(date -u +%s) - started_s + 59) / 60 ))
evidence="$evidence_dir/restore-$stamp.json"
cat > "$evidence" <<EOF
{
  "backupId": "$backup_id",
  "sourceEnvironment": "${ENVIRONMENT:-production}",
  "restoreTarget": "disposable container $db",
  "startedAt": "$started",
  "completedAt": "$completed",
  "databaseConsistencyPass": $database_pass,
  "objectStorageConsistencyPass": $storage_pass,
  "smokePass": $smoke_pass,
  "achievedRpoMinutes": $rpo_minutes,
  "achievedRtoMinutes": $rto_minutes,
  "counts": {"provinces": ${provinces:-0}, "facilities": ${facilities:-0}, "accounts": ${users:-0}},
  "release": "$release"
}
EOF
log "evidence: $evidence"
python3 "$repo/infrastructure/quality/restore_evidence.py" "$evidence" --max-rpo-minutes 60 --max-rto-minutes 240
