# Android integration inventory — 2026-09-19

Taken before any code change in the Android generated-client batch.
Base: `main` at `888105003a5f122b217e5732ae7bab87d40abeb2`, clean apart from the handoff
artefacts that were already untracked.

## 1. Build environment, measured

| Probe | Result |
|---|---|
| `dl.google.com/android/maven2/.../androidx/core/core/1.17.0/core-1.17.0.pom` | **404** from Google's `downloads` server, for artifacts that certainly exist |
| `maven.google.com/...` | 301 to the same `dl.google.com` path, then 404 |
| `repo.maven.apache.org`, `plugins.gradle.org` | 200 |
| `services.gradle.org` distribution | 307 to GitHub release assets, first MiB downloaded (206) — **reachable now**, unlike P10 |
| Git remote | none (`git remote` is empty), so no CI runner can be used without the owner creating one |
| Proxy / artifact mirror configured | none (`HTTP(S)_PROXY` unset, no `~/.gradle/init.d`) |
| Local Gradle | 9.6.0 in the wrapper cache; launcher JVM 25, daemon on the Android Studio JBR |
| Android SDK | platforms 35/36/37, build-tools 36.0.0/37.0.0, emulator, `system-images/android-36.1`, one AVD (`RahalGo`, unrelated project) |
| Gradle cache | AGP 9.4.0, core 1.19.0, Compose UI 1.12.1, Material3 1.4.0 present; **absent**: Room 2.8.5, DataStore 1.2.1, Navigation 2.10.1, Lifecycle 2.11.0, `androidx.hilt` 1.3.0, MapLibre 13.6.1 |
| `gradle --offline :core:model:compileDebugKotlin` | fails at plugin resolution: the cached `com.android.application` 9.4.0 marker is not usable offline |

The cached Google artifacts were written on 2026-09-11 and 2026-09-16, so this machine has
reached Google Maven recently on some network. Today it does not. `BUILD_VERIFIED` for the
Android app therefore still needs either that network, a trusted CI runner, or an artifact
proxy the owner controls. No AGP, Compose or AndroidX version was changed and no mirror was
used.

## 2. The generated Kotlin client

`packages/api-kotlin/generated`, openapi-generator 7.15.0, library `jvm-retrofit2` with
kotlinx.serialization. 22 API classes, suspend functions returning `retrofit2.Response<T>`.

- `ApiClient` defaults to an OkHttp builder with `HttpLoggingInterceptor` at `Level.BODY`,
  which would log tokens and passwords once a logger is attached. The app must pass its own
  `OkHttpClient` and never use the default builder.
- Date-times are `java.time.OffsetDateTime`. The spec's `minSdk 24` predates `java.time`
  (API 26), so the app needs core library desugaring.
- The generator's own `build.gradle` pins Kotlin 1.9.23 and Retrofit 2.10; the app compiles
  the sources with its catalog versions (Kotlin 2.3.21, Retrofit 3.0.0, OkHttp 5.3.0,
  serialization 1.9.0), exactly as `:core:designsystem` compiles the generated tokens.

Operations the Android app uses (non-admin):

| Area | Operations |
|---|---|
| Auth | `authLogin`, `authRefresh`, `authLogout`, `authLogoutAll`, `authRegisterStart/Verify/Complete`, `authRecoveryStart/Verify/Reset`, `authSessionsList`, `authSessionRevoke` |
| Account | `accountProfileRetrieve`, `accountProfileUpdate`, `accountRatingsList`, `accountDeletionRequestCreate` |
| Public | `publicProvincesList`, `publicProvinceCategoriesList`, `publicProvinceCitiesList`, `publicHomeRetrieve`, `publicFacilitiesList`, `publicSearchList`, `publicFacilityRetrieve`, `publicMapFacilitiesList`, `publicAdsList` |
| Ratings | `facilityRatingUpsert`, `facilityRatingDelete` |
| Owner | `ownerConfigRetrieve`, `ownerFacilitiesList`, `ownerFacilityCreate/Retrieve/Update/Submit`, `ownerFacilityLocationReplace`, members list/upsert/delete |
| Availability | `ownerFacilityHoursReplace`, temporary closures list/create/cancel |
| Duty | `ownerFacilityDutyList/Create/Update/Delete` |
| Media | `ownerFacilityImagesList/Create/Delete`, `ownerFacilityEvidenceCreate/Delete` |

## 3. Contract gaps found by reading the generated Kotlin

| Gap | Effect on Android |
|---|---|
| Upload fields `EvidenceUpload.file` and `PublicImageUpload.file` are `format: uri`, because DRF `FileField` is described as a URI when requests and responses share components | the Kotlin client types the part as `java.net.URI` and sends a string, so **no file can be uploaded** through it |
| `ownerFacilityDutyCreate` and `ownerFacilityTemporaryClosureCreate` take the response component (`DutyShift`, `TemporaryClosure`) whose read-only `id` is `required` | the Kotlin data class cannot be built without the caller inventing an id |
| No push-registration operation exists anywhere in the contract (`notifications` has no URLs) | `UnboundPushRegistrationBoundary` cannot be bound; FCM registration stays a source-only boundary |
| `PUT/DELETE /account/profile-image/` absent (INT-017) | profile image stays unavailable in the UI |
| Access tokens carry no session id (`sub`, `iat`, `exp` only, 900 s) | revoking a session or logging out does not invalidate an access token already issued; a WebSocket authenticated before revocation stays authenticated |

## 4. Android source, as found

27 modules, 6 332 lines of Kotlin. Layering is Composable → ViewModel → UseCase →
Repository → boundary, as `11-ANDROID-KOTLIN.md` specifies.

| Area | State |
|---|---|
| `PublicApiBoundary`, `OwnerApiBoundary`, `PushRegistrationBoundary` | domain-facing interfaces; `NetworkModule` binds all three to `UnboundGenerated*` objects that throw (INT-013) |
| `GeneratedApiClientBoundary` | empty marker interface |
| Generated client | not on any module's source path |
| `NetworkError` / `NetworkErrorMapper` | status-code only; ignores the `{code,message,details,requestId}` envelope |
| `AccessTokenInterceptor` | adds `Authorization` and `X-Request-ID`; not on the client the generated APIs would use |
| `RefreshAuthenticator` + `SessionCoordinator` | mutex-coordinated refresh exists and is tested; not installed on any OkHttp client; **any** refresh failure, including being offline, clears the session |
| `RefreshTokenVault` | Keystore AES-GCM, stores one string; the session id that `authLogout` needs is not kept |
| `MemoryAccessTokenStore` | memory only — correct |
| `feature:auth` | a marker object only: no login, registration or recovery screen exists, although the routes are declared |
| Home, Province, Directory, Facility, Search, Map | cache-first repositories over the unbound boundary; lists are `List<T>` with no cursor |
| Room | `province_cache`, `category_cache` (unused), `home_snapshot_cache`, `facility_cache`; paging would overwrite `sortRank` from page 2 onwards |
| Business hours | domain field `sortOrder`; the wire says `sequence` |
| Availability | `AvailabilityState` enum matches the wire values; `nextOpenAt` dropped |
| Category capabilities | not carried to the public `Category`; duty UI cannot be gated on `supportsDuty` |
| Ratings | `RatingValidator` 1–5; repository over the unbound boundary |
| Owner onboarding | config-driven evidence descriptors already modelled (`VerificationRequirementDescriptor` with min/max files) — nothing hard-coded |
| Uploads | `OwnerUploadReader` reads the picked URI with a 10 MiB client cap; boundary takes bytes |
| Realtime | lifecycle-aware OkHttp WebSocket, auth by message after connect (no token in the URL), dedup, backoff; **default URL path `/ws/events/` does not match the backend's `/ws/v1/directory/`**; `wss://` required, so no local connection is possible |
| Push | coordinator validates tokens; boundary unbound; no Firebase configuration in the tree |
| Environment | `API_BASE_URL` etc. from a Gradle property or environment variable, defaulting to `https://api.<ROOT_DOMAIN>/`; no local or staging variant; nothing refuses a placeholder at runtime for REST |
| Manifest | coarse and fine location only, no background location; cleartext disabled |
| Design system | tokens exist; existing screens use literal `dp` values |

## 5. Plan, in the order the brief sets

1. Contract: binary upload parts, input components for duty and closure creation, then
   regenerate all three clients and prove drift.
2. Put the generated sources on `:core:network`'s source path, the way `:core:designsystem`
   takes the generated tokens; desugaring for `java.time`.
3. One adapter around `ApiClient`, one error mapper from the envelope to a domain error,
   mappers from generated to domain models in one place.
4. Auth first: refresh wiring with rotation, session id kept with the refresh token, logout
   and revocation clearing, then the auth screens.
5. Public flows with cursor paging, then owner flows, realtime and push.
6. Verification: the Android build is blocked, so the platform-free layers — generated
   client, adapter, mappers, session coordination and repositories — are also compiled and
   tested by a JVM harness that uses only Maven Central, and a connected suite drives the
   generated client against the real Django, PostGIS and Redis stack. That proves the
   transport and data layers; it does not make the app `BUILD_VERIFIED` and is not reported
   as such.
