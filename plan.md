# Implementation Plan

Updated: 2026-09-17T18:31:00+03:00

## Current phase

P16 — Android Owner

## Goal

Implement the native Android owner experience for onboarding, evidence/uploads, facility status/management and pharmacy duty without bypassing the canonical P10 generated-client boundary or backend owner-policy validation.

## Dependency repair

The recovered backend lineage contains facility persistence plus hours/duty endpoints but is missing the owner config/facility/images/evidence/members API source required by the immutable API contract. Restore that source before binding Android owner flows.

## Tasks

1. Restore `/owner/config/` and owner facility CRUD/submit API from the V3 contract.
2. Restore owner image/evidence/member subresources with private evidence and membership/IDOR checks.
3. Preserve existing hours/temporary-closure/duty endpoints and current-policy submit validation.
4. Add Android owner domain models + generated-client-facing `OwnerApiBoundary`.
5. Implement My Facilities state/actions.
6. Implement onboarding steps: province/category → info → map → hours → images → specialized fields → evidence → review → submit → status.
7. Autosave draft changes; if onboarding is later disabled, existing drafts remain editable while submit rechecks current policy.
8. Implement active facility management: info/contact/location/images/hours/temporary closures/managers.
9. Implement pharmacy duty list/create/edit/cancel/end-early flows.
10. Use Android Photo Picker for uploads where supported; server remains final validator.
11. Wire type-safe MyFacilities/Onboarding/ManageFacility/Duty navigation.
12. Add source/unit-contract qualification, update evidence/status/handoff, and commit.

## Acceptance criteria

- Arabic RTL from the shared Design System.
- Composable → ViewModel → UseCase → Repository layering for owner features.
- Owner APIs enforce authenticated facility membership and prevent IDOR.
- Private verification evidence never receives a public URL or enters public cache.
- Draft autosave is explicit and does not invent production data.
- Submit revalidates current owner-registration switch and current verification requirements on the server.
- Sensitive facility edits defer lifecycle/reverification truth to backend.
- Duty UI only appears when capability permits it; backend remains final authority.
- No hand-authored OpenAPI wire DTOs; P10 adapter remains the transport owner.
- No background location permission.

## Required tests

- Backend owner membership/IDOR source + runtime tests when Django is available.
- Owner config current-switch qualification.
- Submit/current-evidence policy tests.
- Evidence privacy/storage-key leak checks.
- Onboarding reducer/step validation tests.
- Duty input/range validation tests.
- Android owner source architecture/navigation/upload/security checks.
- P14/P15 regression gates.
- Gradle/Compose/instrumentation/physical-device tests when tooling exists.

## Expected files

- `apps/backend/facilities/**`
- `apps/backend/directory_backend/urls.py`
- `apps/android/core/model/**`
- `apps/android/core/network/**`
- `apps/android/feature/owner/**`
- `apps/android/feature/onboarding/**`
- `apps/android/feature/duty/**`
- `apps/android/app/**`
- project management/evidence files.

## Risks

- P10 generated Kotlin client is not materialized; owner transport adapter must remain fail-closed until generation.
- Django/PostGIS/S3 runtime is unavailable locally, so restored owner backend can only receive source qualification here.
- Gradle/Android SDK/ADB are unavailable locally, so P16 cannot reach device verification in this environment.

## Gate

Target gate: `P16 ANDROID OWNER DEVICE PASS`.

Current environment can only establish `SOURCE_IMPLEMENTED`; connected backend and Android device closure require their respective toolchains.
