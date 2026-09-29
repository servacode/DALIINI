# Staging Deploy Runbook

## Preconditions
- CI green for the exact commit.
- Dedicated V3 Git repository connected to Render; never reuse V2 services.
- Dedicated staging PostgreSQL/PostGIS, Key Value, object-storage buckets and secrets.
- `render.yaml` reviewed before applying because its staging plans can incur cost.
- No production credentials in staging.
- Enter identical shared cryptographic secret values for the API and worker (`SECRET_KEY`, signing/HMAC, push-token encryption and analytics salt); keep them outside Git.

## Sequence
1. Record commit SHA and OpenAPI schema hash when P10 is available.
2. Verify a recoverable DB backup exists when staging contains durable test data.
3. Run migration compatibility review (expand/contract for destructive changes).
4. Deploy API; Render `preDeployCommand` runs `manage.py migrate --noinput`.
5. Require `/health/live/` and `/health/ready/` HTTP 200.
6. Deploy worker with the same source revision and secret set.
7. Deploy public web and admin.
8. Run `STAGING_API_ORIGIN=https://... infrastructure/scripts/staging-smoke.sh`.
9. Run API/Admin/public E2E and record evidence.
10. Record Render deploy IDs, commit, schema hash and known issues.

Provider `onrender.com` hostnames are acceptable only for staging while `ROOT_DOMAIN` is unavailable.

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
