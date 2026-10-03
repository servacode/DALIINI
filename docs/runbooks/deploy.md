# Deploy Runbook

The production server runs `infrastructure/production/compose.yml` (DECISION-080). First-time
setup is `infrastructure/production/README.md`; this is every deploy after it (DECISION-081).

## Preconditions
- CI green for the exact commit, the Android workflow included when the app changed.
- Staging deployed with the same commit and checked first. No production credentials in staging.
- A backup from the last two hours (the console's «حالة النظام» page, row «النسخ الاحتياطي»), or
  take one now: `sudo systemctl start daliini-backup.service`.
- Migrations reviewed for expand/contract: a destructive change waits for the release after the
  code stopped using what it drops.

## Sequence
1. **Release the images.** GitHub → Actions → **Release images** → *Run workflow* on `main`,
   with the environment. It publishes `ghcr.io/servacode/daliini-<name>:<environment>-<commit>`
   for the five images; the run's summary names the commit.
2. **Deploy.** Actions → **Deploy** → *Run workflow*: the environment and that full commit SHA.
   If the environment requires a reviewer, they approve the run. Over SSH the server runs:

   ```sh
   infrastructure/production/deploy.sh /srv/daliini/<environment>.env <commit> <environment>-<commit>
   ```

   which checks the commit out, writes `RELEASE` (and a history line) into the env file, pulls
   the images, and runs `up -d`. `migrate` runs `check --deploy` and the migrations first. The
   API, worker and beat restart only after it succeeds, so a failed migration leaves the
   running version in place. The script waits until `https://api.<ROOT>/health/ready/` answers
   through Caddy, then removes that environment's images older than the one it replaced.
3. Check `https://api.<ROOT>/health/live/` and `/health/ready/`, run
   `STAGING_API_ORIGIN=https://api.<ROOT> infrastructure/scripts/staging-smoke.sh` (the same
   smoke works against any origin), and open the site and the console. The system page should
   show every card green.
4. Record the commit, the schema hash (the system page, «بصمة العقد»), the time and anything
   known to be wrong.

The same command works by hand on the server, for example when GitHub is unreachable. Without
`IMAGE_REGISTRY` in the env file, it builds the images on the server instead of pulling them.

A version is rolled back by deploying the previous commit the same way (`rollback.md`). Its
images are still on the server.

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
