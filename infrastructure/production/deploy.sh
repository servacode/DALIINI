#!/usr/bin/env bash
# One deploy, on the server (DECISION-081): the commit checked out, its images pulled (or built),
# the stack brought up, and the API asked through its own public name until it is ready.
#
#   infrastructure/production/deploy.sh <env-file> <commit> [release]
#
# <release> is the image tag. With IMAGE_REGISTRY set in the env file it is pulled
# (CI publishes <environment>-<commit>); without, the images are built here and tagged with it.
# The previous release stays in the env file's history line, and its images on the server, so
# going back is the same command with the previous commit and release (docs/runbooks/rollback.md).
set -euo pipefail

env_file="${1:?usage: deploy.sh <env-file> <commit> [release]}"
commit="${2:?usage: deploy.sh <env-file> <commit> [release]}"
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
log() { printf '%s [deploy] %s\n' "$(date -u +%H:%M:%S)" "$*"; }

setting() { sed -n "s/^$1=//p" "$env_file" | tail -n 1; }
environment="$(setting ENVIRONMENT)"
environment="${environment:-production}"
release="${3:-$environment-$commit}"
registry="$(setting IMAGE_REGISTRY)"
domain="$(setting ROOT_DOMAIN)"
project="$(setting COMPOSE_PROJECT_NAME)"
project="${project:-daliini}"
previous="$(setting RELEASE)"

log "checking out $commit"
git -C "$repo" fetch --quiet origin
git -C "$repo" checkout --quiet --detach "$commit"

log "release $release (was ${previous:-none})"
sed -i "s|^RELEASE=.*|RELEASE=$release|" "$env_file"
grep -q '^RELEASE=' "$env_file" || echo "RELEASE=$release" >> "$env_file"
echo "# $(date -u +%Y-%m-%dT%H:%M:%SZ) deployed $release over ${previous:-none}" >> "$env_file"

compose() { docker compose -p "$project" -f "$here/compose.yml" --env-file "$env_file" "$@"; }
if [ -n "$registry" ]; then
  log "pulling ${registry}daliini-*:$release"
  # Only the project's own images. Caddy, PostgreSQL and Redis are pinned and already here; `up`
  # fetches them only when missing, so a deploy never waits on Docker Hub's rate limit.
  compose --profile tools pull --quiet migrate api worker beat web admin whatsapp-bot backup
  compose up -d --no-build --remove-orphans
else
  log "building on this server"
  compose up -d --build --remove-orphans
fi

# Each release's images stay on the disk the database shares. Keep this one and the one it
# replaced (the rollback), drop this environment's older ones; another environment's are not ours.
prune() {
  docker image ls --format '{{.Repository}} {{.Tag}}' | while read -r repository tag; do
    case "$repository" in "${registry}daliini-"*) ;; *) continue ;; esac
    case "$tag" in "$environment-"*) ;; *) continue ;; esac
    if [ "$tag" != "$release" ] && [ "$tag" != "$previous" ]; then
      docker image rm "$repository:$tag" >/dev/null 2>&1 && log "removed $repository:$tag"
    fi
  done
  docker image prune -f >/dev/null
}

# Ready means the whole path answers: Caddy, its certificate, the API, the database and Redis.
for _ in $(seq 1 60); do
  if curl -fsS --max-time 5 --resolve "api.$domain:443:127.0.0.1" \
    "https://api.$domain/health/ready/" >/dev/null 2>&1; then
    log "ready: https://api.$domain/health/ready/"
    compose ps --format '{{.Service}}\t{{.State}}\t{{.Status}}'
    prune || log "could not remove older images; the system page shows the disk"
    exit 0
  fi
  sleep 5
done

log "the API did not become ready within five minutes"
compose ps -a
compose logs --no-color --tail=80 migrate api caddy
exit 1
