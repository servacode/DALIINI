# Implementation Plan

Updated: 2026-09-17T17:58:00+03:00

## Current phase

P15 — Android Public

## Goal

Implement the public Android experience on top of the P14 native foundation without bypassing the canonical P10 generated-client boundary.

## Tasks

1. Home cache-first state and repository contract.
2. Province selection persistence and province picker UI.
3. Location permission/approximate-aware integration.
4. Search state, query validation and result surfaces.
5. Category directory/list with open/duty filters and nearest semantics.
6. Facility detail with availability, images, hours, actions and directions entry.
7. MapLibre public map surface behind `MapController`.
8. Account public profile/session surface boundary.
9. Ratings list/upsert/delete domain/UI flows.
10. Navigation graph wiring and typed route arguments.
11. Unit/source qualification and offline/error state coverage.
12. Update evidence/status/handoff and commit.

## Acceptance criteria

- Arabic RTL from the shared Design System.
- Immutable UiState for every public ViewModel.
- Composable → ViewModel → UseCase → Repository layering.
- Cache is rendered before refresh where applicable.
- Location denial and approximate location are valid non-crashing states.
- Search/Directory/Facility do not hand-author OpenAPI transport DTOs.
- Map uses native MapLibre boundary, never WebView.
- Ratings enforce 1..5 in client domain and rely on server as final authority.
- No sensitive auth material in UI/cache/logs.
- No background location permission.

## Required tests

- Home cache-first/offline state unit tests.
- Province persistence unit tests.
- Search query/result state tests.
- Directory filter state tests.
- Facility detail state/error tests.
- Rating validation tests.
- Source qualification for routes/layers/RTL/location/map/security.
- Gradle/Compose/device tests when Android tooling is available.

## Expected files

- `apps/android/feature/home/**`
- `apps/android/feature/province/**`
- `apps/android/feature/search/**`
- `apps/android/feature/directory/**`
- `apps/android/feature/facility/**`
- `apps/android/feature/map/**`
- `apps/android/feature/account/**`
- `apps/android/feature/ratings/**`
- `apps/android/app/**`
- project management/evidence files.

## Risks

- P10 generated Kotlin client is not yet materialized because Django schema generation cannot run locally; repositories must remain interfaces/boundaries rather than duplicate wire DTOs.
- Gradle/Android SDK/ADB are unavailable locally; DEVICE_PASS cannot be claimed here.
- Map style/provider production configuration is a later external/configuration concern; no production map URL will be invented.

## Gate

Target gate: `P15 ANDROID PUBLIC DEVICE PASS`.

Current environment can only establish `SOURCE_IMPLEMENTED`; device closure requires Android SDK/emulator/physical device and P10 client binding.
