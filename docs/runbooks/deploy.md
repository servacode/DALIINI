# Deploy Runbook

The production server runs `infrastructure/production/compose.yml` (DECISION-080). First-time
setup is `infrastructure/production/README.md`; this is every deploy after it.

## Preconditions
- CI green for the exact commit, the Android workflow included when the app changed.
- No production credentials in staging, and staging deployed and checked first.
- A backup from the last 24 hours (the console's «حالة النظام» page, row «النسخ الاحتياطي»), or
  take one now: `sudo systemctl start daliini-backup.service`.
- Migrations reviewed for expand/contract: a destructive change waits for the release after the
  code stopped using what it drops.

## Sequence
On the server, in `/srv/daliini/repo/infrastructure/production`:

1. `git -C /srv/daliini/repo fetch && git -C /srv/daliini/repo checkout <commit>`; record the
   commit and the OpenAPI schema hash.
2. Set `RELEASE=<commit>` in `/srv/daliini/production.env`, so the images carry the commit and
   the previous ones stay on the server for a rollback.
3. `docker compose --env-file /srv/daliini/production.env up -d --build`. `migrate` runs
   `check --deploy` and the migrations first; the API, worker and beat restart only after it
   succeeded, and a failed migration leaves the running version in place.
4. Require `https://api.<ROOT>/health/live/` and `/health/ready/` to answer 200.
5. Run `STAGING_API_ORIGIN=https://api.<ROOT> infrastructure/scripts/staging-smoke.sh` (the same
   smoke, any origin) and open the site and the console.
6. Record the commit, the schema hash, the time and anything known to be wrong.

A version is rolled back by checking out the previous commit, setting `RELEASE` back, and the
same `up -d --build` (`rollback.md`).

## Push notifications (Android)

The API and worker send pushes through FCM HTTP v1 when `PUSH_PROVIDER=fcm`. Both need:

1. `FCM_PROJECT_ID`: the Firebase project ID (Project settings > General).
2. `FCM_SERVICE_ACCOUNT_JSON`: a key for a service account that may send messages
   (Project settings > Service accounts > Generate new private key). Paste the downloaded
   JSON as is, or its base64 (`base64 -w0 key.json`). It is a secret: never commit it, and
   rotate it in Google Cloud if it leaks.

Production refuses to start with `PUSH_PROVIDER=fcm` and either value missing. A push carries
identifiers only (`notificationId`, `type`, and for some kinds `facilityId`, `gapDate`,
`provinceId`); the words stay in the inbox. The Android build must point at the same Firebase
project: `DIRECTORY_FIREBASE_PROJECT_ID`, `DIRECTORY_FIREBASE_APPLICATION_ID`,
`DIRECTORY_FIREBASE_API_KEY` and `DIRECTORY_FIREBASE_SENDER_ID` (see
`apps/android/play/release.env.example`).

Check: sign in on a phone with the release build, trigger a notification (for example an
application decision), and confirm it arrives. The worker log shows `push.token_deactivated`
for tokens FCM no longer knows, and `push.provider_misconfigured` when the key or project is
wrong.
