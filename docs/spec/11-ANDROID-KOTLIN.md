# Android Native — Kotlin + Jetpack Compose

## Package

```text
com.servacode.directory
```

## SDK baseline

```text
minSdk 24
targetSdk 36
compileSdk 36+
```

Before Play upload re-check current policy; as of 2026-09-17 new apps/updates must target API 36+.

## Project modules

```text
:app
:core:model
:core:network
:core:database
:core:datastore
:core:auth
:core:designsystem
:core:location
:core:maps
:core:analytics
:core:observability
:core:testing

:feature:bootstrap
:feature:home
:feature:province
:feature:search
:feature:directory
:feature:facility
:feature:map
:feature:navigation
:feature:auth
:feature:account
:feature:ratings
:feature:owner
:feature:onboarding
:feature:duty
:feature:settings
```

## Build system

- Gradle Kotlin DSL.
- Version catalog.
- convention plugins in `build-logic`.
- dependency locking/verification where practical.
- no secrets in gradle files.

## Architecture

```text
Composable
→ ViewModel
→ UseCase
→ Repository
→ RemoteDataSource / LocalDataSource
```

Use immutable `UiState`.

Example:
```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(...) : HomeUiState
    data class Offline(...) : HomeUiState
    data class Error(val kind: ErrorKind) : HomeUiState
}
```

## Networking

- Retrofit.
- OkHttp.
- generated models/services or generated client wrapper.
- auth interceptor.
- refresh coordination mutex to avoid refresh storms.
- request ID capture.
- error mapper.

## Secure session

Refresh material:
- Android Keystore-backed encrypted storage.

Access token:
- memory preferred, recover through refresh after process death.

No raw password.

## Room

Cache entities:
- province.
- category.
- home snapshot.
- facility list/detail.
- owner drafts where safe.

Separate cache models from domain if schema needs it.

## DataStore

- selected province.
- settings.
- onboarding hints.
- location preference metadata.

## Location

Create `LocationProvider`.

Requirements:
- while-in-use permission.
- approximate/precise handling.
- timeout.
- last known fallback.
- no background permission Core V3.

## MapLibre

Native map composable/view integration behind `MapController` abstraction.

No WebView map.

## Home behavior

On bootstrap:
1. render cached.
2. resolve selected province.
3. request/refresh location if allowed.
4. fetch home.
5. collect relevant realtime invalidations.

## Navigation

Use type-safe route definitions.

Routes:
- Home.
- ProvincePicker.
- Search.
- Directory(categoryId).
- FacilityDetail(id).
- Map.
- BuiltInNavigation(destination).
- Login.
- Register.
- Recovery.
- Account.
- MyRatings.
- MyFacilities.
- Onboarding(draftId?).
- ManageFacility(id).
- Duty(id).
- Settings.

## Uploads

Use Android photo picker where possible.

Before upload:
- local preview.
- client compression optional.
Server remains final validator.

## Realtime

Lifecycle-aware WebSocket:
- foreground connection.
- auth.
- subscribe scopes.
- bounded exponential backoff.
- network aware.
- events invalidate repositories.

## Push

FCM:
- token registration with backend.
- token refresh.
- notification permission for applicable Android versions.
- deep links.
- no sensitive text.

## Built-in navigation

Use native location stream during active navigation.

Foreground service only if product/navigation behavior legally and technically requires continuous navigation while screen/background transitions. If used, declare correct foreground-service type/permission and document Play policy.

## Testing

Unit:
- ViewModels.
- use cases.
- repositories.
- error mapping.
- cache.
- auth refresh.

UI:
- Compose tests.

Device:
- permission deny.
- approximate location.
- offline.
- background/foreground.
- process death.
- push.
- map.
- navigation.
- image upload.
- Arabic RTL.

## Release

Build:
```bash
./gradlew clean
./gradlew test
./gradlew lint
./gradlew :app:bundleRelease
```

Release signing:
- upload key outside repository.
- Play App Signing.
