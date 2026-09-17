# Implementation Plan

Updated: 2026-09-17T18:56:00+03:00

## Current phase

P18 — Offline / Realtime / Push Hardening

## Goal

Harden Android live-data behavior around the existing REST truth, Room public cache, Channels invalidation events and push notification boundary so reconnects, stale cache refresh, token rotation and lifecycle transitions do not duplicate domain truth or leak sensitive data.

## Tasks

1. Read P18 requirements from realtime/offline/security/Android specs.
2. Define typed realtime invalidation envelope matching the P8 event catalog.
3. Add authenticated WebSocket lifecycle boundary with post-connect auth; never query-string tokens.
4. Implement reconnect/backoff and network/lifecycle-safe resubscription.
5. Route invalidation events to repositories/cache refresh; REST remains source of truth.
6. Add stale/fresh cache policy and explicit offline UI state where required.
7. Add device push registration boundary with safe token rotation/unregister behavior.
8. Ensure notification payloads contain identifiers/invalidation intent only, not evidence/storage keys/secrets.
9. Add source/pure tests for envelope validation, backoff, dedupe, reconnect and invalidation routing.
10. Run P14-P17 regression gates, document evidence and commit.

## Acceptance criteria

- REST remains the authoritative content path.
- WebSocket/push events cause invalidation/refetch, not direct domain mutation from event payloads.
- Access tokens are never placed in WebSocket URL/query parameters.
- Reconnect uses bounded backoff/jitter and does not create duplicate subscriptions.
- Public Room cache remains usable offline; account/owner/private evidence are not added to public cache.
- Push token registration is safe for rotation/logout and no provider secret is embedded in app source.
- Background location is still absent.

## Required tests

- Realtime envelope validation/source tests.
- Reconnect/backoff/dedup tests.
- Invalidation routing tests.
- Push registration lifecycle tests.
- P14/P15/P16/P17 regression gates.
- Gradle/unit/Compose/instrumentation/device tests when Android tooling exists.

## Expected files

- `apps/android/core/network/**`
- `apps/android/core/database/**`
- `apps/android/core/observability/**`
- `apps/android/app/**`
- relevant public/owner repositories and Android tests/scripts.
- project management/evidence files.

## Risks

- P10 generated client remains unbound, so REST adapter integration cannot be claimed connected.
- Redis/Channels/FCM connected services are unavailable locally.
- Android SDK/Gradle/ADB are unavailable locally.

## Gate

Target gate: `P18 MOBILE LIVE DATA PASS`.

Current environment can establish source/pure qualification only; connected Channels/FCM and device lifecycle verification remain mandatory before closure.
