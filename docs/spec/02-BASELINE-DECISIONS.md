# Baseline Decisions — لا تسأل عن هذه القرارات أثناء التنفيذ

هذا الملف يحسم defaults حتى يستطيع وكيل برمجي بدء التنفيذ تلقائيًا.

## Product / naming

```text
Repository slug: directory-platform-v3
Internal product code: DIRECTORY
Development display name Arabic: الدليل
Development display name English: Directory
Android applicationId: com.servacode.directory
iOS bundle identifier: com.servacode.directory
Timezone: Asia/Damascus
Primary locale: ar
Secondary-ready locale: en
```

يمكن تغيير **الاسم الظاهر** قبل Store Listing بدون تغيير المعمارية. لا تغيّر `applicationId` بعد إنشاء Play production listing إلا بقرار صريح.

## Backend

```text
Language: Python 3.13
Framework: Django 5.2 LTS
API: Django REST Framework
Geo: GeoDjango/PostGIS
DB: PostgreSQL 17
Cache/broker: Redis
Async: Celery
Realtime: Django Channels
Schema: drf-spectacular OpenAPI
Password hasher: Argon2 first
Package management: uv + pyproject + lock
```

Django 5.2 LTS يفضل على feature release أحدث في البداية لأن الهدف هو استقرار طويل العمر.

## Web

```text
Node: 24.x
Package manager: pnpm
Admin: Next.js + React + TypeScript strict
Public web: Next.js
Styling: Tailwind CSS + Serva Design System
Accessible primitives: Radix where useful
Unit: Vitest
E2E: Playwright
```

## Android

```text
Kotlin Native
Jetpack Compose
minSdk: 24
targetSdk: 36
compileSdk: 36 (or higher compatible stable without changing target policy)
Architecture: feature modular + UDF/MVVM
DI: Hilt
Network: Retrofit + OkHttp
Serialization: Kotlin Serialization
Cache: Room
Preferences: DataStore
Secrets: Android Keystore-backed secure store
Maps: MapLibre Native
Location: native fused/location implementation behind LocationProvider
Push: FCM
Images: Coil
```

As of 2026-09-17 Google Play requires new apps/updates submitted after 2026-08-31 to target Android 16 / API 36 or higher.

## iOS

```text
Swift + SwiftUI
async/await
URLSession/generated OpenAPI client
Keychain
SwiftData baseline unless ADR finds a blocker
Core Location
MapLibre Native
APNs
```

## Design

```text
Direction: Arabic RTL, clean, professional, Syrian-modern without political decoration
Primary: #0B6B47
Primary strong: #06452F
Primary soft: #DCEFE6
Background: #F7F8F5
Surface: #FFFFFF
Text primary: #15231C
Text secondary: #5D6B64
Border: #DDE4E0
Success: #138A5B
Warning: #C98214
Danger: #C23B3B
Info: #2E6FB5
Font baseline: Tajawal
```

Exact accessibility variants may adjust during Design System validation.

## Production baseline infrastructure

```text
DNS: Cloudflare
Public web: Render paid Web Service
Admin: Render paid Web Service
API: Render paid ASGI Web Service
Worker: Render paid background worker
DB: Render managed PostgreSQL 17 + PostGIS
Redis: Render managed Key Value / Redis-compatible paid service
Object storage: Cloudflare R2 (S3-compatible) baseline
Monitoring: Sentry baseline
Region: Frankfurt/closest reliable supported region baseline
```

Provider abstractions prevent lock-in.

## Production domain convention

The actual root domain is an external owned asset and cannot be invented.

Code must use:

```text
ROOT_DOMAIN=<owned-domain>

www.<ROOT_DOMAIN>
api.<ROOT_DOMAIN>
admin.<ROOT_DOMAIN>
```

Optional:
```text
cdn.<ROOT_DOMAIN>
status.<ROOT_DOMAIN>
```

No code may hardcode a provider-generated Render hostname as Production identity.

## Maps / routing / geocoding

```text
Renderer: MapLibre
Nearby: backend/PostGIS
Routing interface: RoutingProvider
Initial routing implementation: OSRM
Geocoding interface: GeocodingProvider
Development geocoder: Nominatim-compatible
Production provider/hosting: configurable, never public demo endpoint by assumption
```

## Auth

Use:
- short-lived access token.
- opaque rotating refresh secret.
- session table.
- reuse detection.
- revoke current/all.
- password change revokes all.
- blocked user rejected.

Access-token wire standard should use standard JOSE/JWT or a well-reviewed signed format. Do not invent cryptography.

## Public data

No account required for browsing/search/map/details/directions/duty.

## Account creation

Baseline:
```text
name
Syrian mobile
province
OTP verification
password
```

Supported input:
```text
09xxxxxxxx
+9639xxxxxxxx
009639xxxxxxxx
```

Canonical:
```text
+9639xxxxxxxx
```

## Owner verification

Public images != private evidence.

Default generic verification policy can start with:
- storefront/sign photo.
- business card/photo proof.

But requirements are Admin-configurable and not hardcoded into clients.

## Release behavior

- Android first.
- iOS begins after contracts/design are stable and Android core is device-qualified.
- Production rollout is staged.
- No production claim without backup restore proof and monitoring.
