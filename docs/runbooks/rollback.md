# Bad Deploy Rollback Runbook

1. Declare incident severity and freeze further deploys.
2. Capture failing deploy ID, commit, logs and health status.
3. If the migration is backward compatible, redeploy the last known-good application commit.
4. Do not blindly reverse destructive database migrations. Use forward repair or restore only after impact review.
5. Re-run liveness/readiness and critical E2E smoke.
6. Confirm worker and web/admin revisions are compatible with the backend.
7. Document timeline, root cause and prevention action.

Database changes must follow expand/contract so code rollback normally does not require schema downgrade.
