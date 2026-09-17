# Implementation Plan

Updated: 2026-09-17T18:44:51+03:00

## Current phase

P17 — Maps / Navigation

## Goal

Implement production-configurable MapLibre routing/geocoding/navigation foundations for Android with OSRM as the initial `RoutingProvider`, a Nominatim-compatible `GeocodingProvider`, an explicit turn-by-turn state machine and Arabic platform TTS, without shipping public demo endpoints or moving geo truth out of PostGIS.

## Tasks

1. Preserve PostGIS as truth for facility coordinates, nearest, distance and bbox.
2. Centralize production map style/tile provider configuration; never ship MapLibre demo tiles.
3. Define `RoutingProvider.route(origin, destination, profile)` domain boundary.
4. Implement an OSRM-compatible adapter using environment-configured base URL only.
5. Define `GeocodingProvider.forward/reverse` and implement a Nominatim-compatible adapter behind configured provider URL/user-agent policy.
6. Add route/maneuver domain models independent of transport DTOs.
7. Implement navigation states: Idle, Routing, Navigating, Rerouting, Arrived, Error.
8. Track maneuver index, remaining distance, ETA, off-route distance, reroute cooldown and arrival threshold.
9. Add Arabic maneuver phrase builder plus Android native TTS adapter behind a voice interface.
10. Integrate facility Directions into built-in navigation without inventing route data.
11. Handle lifecycle/network-loss/reroute source paths and keep foreground-only location policy.
12. Add pure unit/source tests for geometry/off-route/state/TTS phrases/provider URL policy.
13. Update evidence/status/handoff and commit.

## Acceptance criteria

- MapLibre Native remains the renderer; no WebView map.
- No `demotiles.maplibre.org` or public OSRM demo endpoint in production source.
- Routing/geocoding provider URLs come from environment/build configuration and fail closed when placeholders remain.
- OSRM is an adapter, not embedded business logic.
- Navigation engine does not fabricate distance/ETA/maneuvers.
- Route geometry and coordinates validate latitude/longitude ranges.
- Arabic TTS phrases are deterministic and platform speech is behind an interface.
- No background-location permission is introduced.
- P10 transport ownership remains respected; provider-specific DTOs stay inside provider adapters.

## Required tests

- Routing provider URL/config policy source tests.
- OSRM response mapping unit tests.
- Nominatim mapping/unit tests.
- Coordinate validation tests.
- Off-route and arrival-threshold tests.
- Reroute cooldown tests.
- Navigation state transition tests.
- Arabic maneuver phrase tests.
- P14/P15/P16 regression gates.
- Gradle build/unit/Compose/instrumentation when Android tooling exists.
- Real road test for GPS drift, maneuver timing, reroute, lifecycle/screen lock, voice and network loss before P17 can close.

## Expected files

- `apps/android/core/maps/**`
- `apps/android/core/location/**`
- `apps/android/feature/navigation/**`
- `apps/android/app/**`
- Android source qualification/tests.
- project management/evidence files.

## Risks

- Final licensed map/style/routing/geocoding provider endpoints are external deployment inputs and must not be guessed.
- Android SDK/Gradle/ADB are unavailable locally, so road/device qualification cannot run here.
- Provider usage/rate/caching rules must be finalized for the actual production provider before release.

## Gate

Target gate: `P17 NAVIGATION ROAD PASS`.

Current environment can establish source/unit qualification only. Road/device verification remains mandatory before closure.
