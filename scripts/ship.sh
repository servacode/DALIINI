#!/usr/bin/env bash
# Ship the pushed head of this branch to an environment, end to end, from this machine:
# Release images for that commit, then Deploy it, waiting on each and failing on any job that did
# not succeed (`gh run watch`'s exit code is not trusted; each job's conclusion is read).
#
#   scripts/ship.sh [production|staging]
#
# Nothing here reaches the server: GitHub's runners do (.github/workflows/deploy.yml).
set -euo pipefail

environment="${1:-production}"
branch="$(git rev-parse --abbrev-ref HEAD)"
commit="$(git rev-parse HEAD)"
log() { printf '%s [ship] %s\n' "$(date +%H:%M:%S)" "$*"; }

[ -z "$(git status --porcelain --untracked-files=no)" ] || { log "commit your changes first"; exit 1; }
git fetch --quiet origin "$branch"
[ "$(git rev-parse "origin/$branch")" = "$commit" ] || { log "push $branch first"; exit 1; }

# Start a workflow on this branch and print the id of the run it made for this commit.
start() {
  local workflow="$1"; shift
  local since; since="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  gh workflow run "$workflow" --ref "$branch" "$@" >/dev/null
  for _ in $(seq 1 30); do
    sleep 4
    local id
    id="$(gh run list --workflow "$workflow" --branch "$branch" --limit 5 \
      --json databaseId,headSha,createdAt \
      -q "[.[] | select(.headSha == \"$commit\" and .createdAt >= \"$since\")][0].databaseId")"
    [ -n "$id" ] && [ "$id" != "null" ] && { echo "$id"; return; }
  done
  log "the $workflow run did not appear"; exit 1
}

# Wait for a run, then fail unless every job succeeded.
finish() {
  local id="$1" name="$2"
  until [ "$(gh run view "$id" --json status -q .status)" = completed ]; do sleep 20; done
  local failed
  failed="$(gh run view "$id" --json jobs -q '.jobs[] | select(.conclusion != "success") | .name')"
  if [ -n "$failed" ]; then
    log "$name failed: $failed"
    log "$(gh run view "$id" --json url -q .url)"
    exit 1
  fi
  log "$name: done"
}

log "shipping ${commit:0:8} to $environment"
release="$(start release-images.yml -f environment="$environment")"
log "building the images (run $release)"
finish "$release" "images"

deploy="$(start deploy.yml -f environment="$environment" -f commit="$commit")"
log "deploying (run $deploy)"
finish "$deploy" "deploy"

domain="$(gh variable get ROOT_DOMAIN --env "$environment" 2>/dev/null || true)"
log "live: ${commit:0:8} on $environment${domain:+, https://$domain}"
