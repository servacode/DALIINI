# Environment and Configuration Contract

No secret values are committed.

## Backend

```text
DJANGO_SETTINGS_MODULE
ENVIRONMENT=development|staging|production
SECRET_KEY
DATABASE_URL
REDIS_URL
ALLOWED_HOSTS
CORS_ALLOWED_ORIGINS
CSRF_TRUSTED_ORIGINS

ACCESS_TOKEN_SIGNING_KEY / JOSE key material
REFRESH_HMAC_SECRET
RECOVERY_HMAC_SECRET

S3_ENDPOINT_URL
S3_REGION
S3_ACCESS_KEY_ID
S3_SECRET_ACCESS_KEY
S3_PUBLIC_BUCKET
S3_PRIVATE_BUCKET

OTP_PROVIDER
OTP_*
PUSH_PROVIDER
FCM_*
APNS_*

SENTRY_DSN
SENTRY_ENVIRONMENT
```

## Admin

```text
NODE_ENV
ADMIN_API_ORIGIN
NEXT_PUBLIC_APP_NAME
NEXT_PUBLIC_ROOT_DOMAIN
SENTRY_DSN
```

Secrets that must stay server-side must not use `NEXT_PUBLIC_`.

## Public Web

```text
PUBLIC_API_ORIGIN
NEXT_PUBLIC_ROOT_DOMAIN
SUPPORT_EMAIL
PRIVACY_CONTACT_EMAIL
```

## Android

Build config:
```text
API_BASE_URL
WS_BASE_URL
MAP_STYLE_URL
SENTRY_DSN
APP_ENV
```

No backend secret in APK.

## iOS

Same public client configuration.

## Domain

```text
ROOT_DOMAIN
```

Used by deployment templates, not hardcoded source.

## Feature flags

Typed server-side settings, not arbitrary env proliferation for runtime product settings.

## Production validation

Startup fails if:
- secret default placeholder.
- DEBUG true.
- test OTP provider.
- missing database/redis/storage.
- wildcard allowed hosts.
- insecure cookie settings.
