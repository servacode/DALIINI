# Bad Deploy Rollback Runbook

1. Declare incident severity and freeze further deploys.
2. Capture the failing commit, the logs of the services involved
   (in `/srv/daliini/repo/infrastructure/production`:
   `docker compose --env-file /srv/daliini/production.env logs --tail=200 api worker migrate`)
   and the health status.
3. Find the last good release: the env file's history lines say what each deploy replaced.

   ```sh
   grep '^# .* deployed ' /srv/daliini/production.env | tail -n 3
   ```

4. If the migrations are backward compatible, deploy that commit again: the **Deploy** workflow
   with its SHA, or on the server
   `infrastructure/production/deploy.sh /srv/daliini/production.env <commit> production-<commit>`.
   deploy.sh keeps the previous release's images, so no build or download is needed (DECISION-081).
5. Do not blindly reverse destructive database migrations. Use forward repair, or restore
   (`infrastructure/BACKUP-RESTORE.md`) only after reviewing the impact.
6. Re-run liveness and readiness, the smoke script and the critical paths, and check that the
   system page is green.
7. Confirm the worker, site and console run the same release as the API (`docker compose ps`
   shows each image's tag).
8. Document the timeline, the root cause and the prevention action.

Database changes must follow expand/contract, so a code rollback normally needs no schema
downgrade.
