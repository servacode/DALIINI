# Monitoring and Alerting Runbook

Two views, because neither can see everything (DECISION-081).

## From inside: the console's «حالة النظام» page

Each dependency is asked directly when the page opens and every minute after that
(DECISION-073):
- the database and pending migrations;
- Redis, the Celery workers and the scheduler's heartbeat;
- both storage buckets;
- the sign-in code channel and push notifications;
- the hourly backup: a warning after 2 hours without one, a failure after 26;
- **the server's disk**: a warning at 80%, a failure at 90%. A full disk stops the database.
  Free space by removing old release images (`docker image prune -a` keeps only what runs) or
  by growing the disk;
- error tracking and maintenance mode.

The page needs the server to be up, so it cannot tell anyone that the server is down.

## From outside: an uptime service

Use any external monitor (UptimeRobot, Better Stack, Healthchecks.io and similar), checking
every one to five minutes and alerting by e-mail and phone:

| What | Expect |
|---|---|
| `https://api.<ROOT>/health/live/` | 200: the process answers |
| `https://api.<ROOT>/health/ready/` | 200: the database and Redis answer |
| `https://<ROOT>/` | 200: the site renders |
| `https://admin.<ROOT>/login` | 200: the console renders |
| the TLS certificate of `<ROOT>` | more than 14 days left |

For the backup, a dead-man's switch is optional: `db-backup.sh` already reports each run to the
system page.

## Logs

Each service writes JSON lines to Docker's log (20 MB × 5 per service), with request IDs and no
access log:

```sh
cd /srv/daliini/repo/infrastructure/production
docker compose --env-file /srv/daliini/production.env logs --since=1h api worker
```

## Alerts

Alerts must be actionable:
- the API down or not ready;
- the database or Redis unavailable;
- a stalled worker;
- elevated 5xx;
- a failed or late backup;
- a failed restore drill (`journalctl -u daliini-restore-drill`);
- storage failures;
- a disk above 80%;
- a crash spike in Sentry, for the backend and the Android app.

Do not alert on noisy metrics that need no action.

Every release record should include the commit, the schema hash, the migrations, the image
tags, any known issues and the rollback target.
