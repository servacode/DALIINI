# Implementation Roadmap and Gates

The sequence is mandatory unless an ADR explicitly changes dependencies.

## Phase 0 — Repository + governance

Deliver:
- repo.
- docs.
- ADR template.
- formatting.
- CI skeleton.
- environment examples.

Gate:
`P0 GOVERNANCE PASS`

## Phase 1 — Design System foundations

Deliver:
- token schema.
- token generation TS/Kotlin/Swift.
- font policy.
- core component specs.

Gate:
`P1 DESIGN TOKENS PASS`

This phase can continue in parallel with backend foundation after token structure freezes.

## Phase 2 — Backend foundation

- Django.
- settings.
- PostGIS.
- Redis.
- Celery.
- Channels.
- object storage abstraction.
- logs.
- health.
- pytest/ruff/mypy.
- Docker.

Gate:
`P2 BACKEND FOUNDATION CONNECTED PASS`

## Phase 3 — Accounts/Auth/RBAC

- user.
- phone normalization.
- OTP abstraction.
- sessions.
- recovery.
- profile.
- roles/permissions.
- audit.

Gate:
`P3 AUTH RBAC PASS`

## Phase 4 — Locations/Taxonomy

- provinces.
- cities.
- groups/categories.
- capabilities.
- per-province switches.
- verification policies.
- seed.

Gate:
`P4 TAXONOMY PASS`

## Phase 5 — Facility/Owner

- facility.
- memberships.
- drafts.
- applications.
- public media.
- private evidence.
- reverification.

Gate:
`P5 OWNER DOMAIN PASS`

## Phase 6 — Hours/Duty

- availability engine.
- schedule.
- temporary closure.
- pharmacy duty.
- overlap constraints.

Gate:
`P6 AVAILABILITY PASS`

## Phase 7 — Public discovery/search/ratings

- Home API.
- list/detail.
- PostGIS nearest.
- bbox.
- search.
- ratings.

Gate:
`P7 PUBLIC DISCOVERY PASS`

## Phase 8 — Realtime

- WebSocket.
- scopes.
- after-commit events.
- tests.

Gate:
`P8 REALTIME CONNECTED PASS`

## Phase 9 — Ads/Push/Analytics

- ads.
- notification records/providers.
- FCM interface.
- APNs interface placeholder until iOS.
- analytics registry.

Gate:
`P9 CONTENT SERVICES PASS`

## Phase 10 — OpenAPI generated clients

- TS.
- Kotlin.
- Swift.
- drift CI.

Gate:
`P10 CONTRACT PASS`

## Phase 11 — Public Web

- landing.
- privacy.
- terms.
- support.
- delete-account.

Gate:
`P11 PUBLIC WEB PASS`

## Phase 12 — Admin foundation

- auth.
- layout.
- design system.
- API client.
- RBAC UI.

Gate:
`P12 ADMIN FOUNDATION PASS`

## Phase 13 — Admin operations

- review.
- facilities.
- users.
- taxonomy.
- provinces.
- ads.
- audit.
- analytics.
- settings/system.

Gate:
`P13 ADMIN GOLDEN PATH PASS`

## Phase 14 — Android foundation

- Kotlin.
- modules.
- Compose.
- DI.
- network.
- secure auth.
- cache.
- navigation.
- design system.

Gate:
`P14 ANDROID FOUNDATION DEVICE PASS`

## Phase 15 — Android public

- Home.
- location.
- search.
- directory.
- detail.
- map.
- account.
- rating.

Gate:
`P15 ANDROID PUBLIC DEVICE PASS`

## Phase 16 — Android owner

- onboarding.
- uploads.
- evidence.
- status.
- manage.
- duty.

Gate:
`P16 ANDROID OWNER DEVICE PASS`

## Phase 17 — Maps/navigation

- production map provider config.
- route.
- geocode.
- turn-by-turn.
- Arabic TTS.
- road tests.

Gate:
`P17 NAVIGATION ROAD PASS`

## Phase 18 — Offline/realtime/push hardening

Gate:
`P18 MOBILE LIVE DATA PASS`

## Phase 19 — Staging production-like deploy

- paid/prod-like services.
- custom staging domains where available.
- runtime smoke.
- secrets.

Gate:
`P19 STAGING RUNTIME PASS`

## Phase 20 — Full E2E/security/backup

- golden path.
- security.
- load baseline.
- restore.

Gate:
`P20 RELEASE QUALITY PASS`

## Phase 21 — Android Play RC

- release AAB.
- Play internal.
- closed testing if required.
- policy forms/assets.

Gate:
`P21 PLAY RC PASS`

## Phase 22 — Android production

- staged rollout.
- monitoring.

Gate:
`P22 ANDROID PRODUCTION VERIFIED`

## Phase 23 — iOS foundation

Gate:
`P23 IOS FOUNDATION DEVICE PASS`

## Phase 24 — iOS feature parity

Gate:
`P24 IOS PARITY PASS`

## Phase 25 — iOS TestFlight/App Store

Gate:
`P25 IOS PRODUCTION VERIFIED`

## Phase 26 — Expansion operations

- new provinces.
- new categories.
- operational analytics.
- continuous improvement.

### Closure rule

Every phase:
- automated evidence.
- status file update.
- no unresolved phase-blocking defect.
