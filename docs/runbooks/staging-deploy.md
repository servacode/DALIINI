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
