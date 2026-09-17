# System Architecture

## 1. Architecture style

Use a modular monolith.

Reason:
- product is one bounded platform.
- simpler transaction integrity.
- easier deployment and debugging.
- enough scalability for initial and medium scale.
- prevents premature microservice complexity.

Split services only after measured operational need.

## 2. Backend boundaries

```text
core
accounts
sessions
locations
directory
facilities
ownership
verification
business_hours
pharmacy_duty
ratings
media_storage
search
realtime
notifications
ads
analytics
audit
platform_settings
admin_console
health
```

## 3. Read/write layering

Recommended:

```text
HTTP View
→ Serializer / Request DTO
→ Permission
→ Service
→ Model/DB
```

Complex reads:

```text
View
→ Selector/Query Service
→ ORM/PostGIS
```

Do not place complex lifecycle logic in serializer `.save()` or views.

## 4. Transactions

Use `transaction.atomic()` for:
- application submit.
- review approve/reject.
- membership changes.
- duty mutations.
- sensitive state transitions.
- settings mutations tied to audit.

External effects after commit:
- WebSocket.
- push queue.
- cache invalidation.

## 5. Cache

Redis:
- public taxonomy cache.
- rate limiting.
- WebSocket layer.
- Celery broker/result where needed.
- selective response fragments.

Database remains authoritative.

## 6. ASGI

Django runs ASGI because:
- REST.
- WebSocket.
- async-compatible edges.

Do not convert all DB services to async merely because ASGI exists.

## 7. Public/owner/admin API separation

Public DTOs are privacy-minimal.

Owner:
- member-scoped.

Admin:
- permission-scoped.

No shared “full facility serializer” exposed to all three.

## 8. Public Web architecture

`apps/web`:
- landing.
- legal.
- support.
- deletion request.
- future public SEO.

It can call public/account deletion endpoints but should not become Admin.

## 9. Admin BFF

Admin Next.js uses same-origin server routes/BFF for browser session refresh cookie.

Browser:
```text
admin.<domain>
→ Next.js
→ backend API
```

Avoid exposing refresh token to JS.

## 10. Mobile architecture

Mobile calls `api.<domain>` directly over HTTPS.

Secrets stored using platform secure storage.

## 11. Contract generation

Django OpenAPI is generated during CI.

Clients generated:
- TS.
- Kotlin.
- Swift.

Schema hash committed/verified.

## 12. Provider interfaces

External dependencies behind interfaces:

```text
OtpProvider
RecoveryProvider
PushProvider
ObjectStorageProvider
RoutingProvider
GeocodingProvider
MapStyleProvider
AnalyticsProvider
ObservabilityProvider
```

## 13. Failure philosophy

- DB failure → readiness false.
- Redis failure → degrade only where safe; auth/session policies that require Redis must fail safely.
- Celery down → queue persists/retries if broker works; critical transaction still committed.
- push failure → do not rollback business action.
- analytics failure → do not block product.
- map routing failure → external map fallback.
- geocoder failure → manual map pin still possible.

## 14. Versioning

API:
```text
/api/v1/
```

Breaking changes use new version or compatibility window.

Mobile version support window documented with each release.

## 15. Security boundaries

Trust:
- device/browser untrusted.
- client IDs/params untrusted.
- object-storage paths private by default.
- Admin UI permission display is convenience, backend is enforcement.
