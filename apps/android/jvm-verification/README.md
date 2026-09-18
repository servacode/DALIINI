# JVM verification build

A second Gradle build over the Android app's own sources, for the parts that do not need the
Android framework. It exists because the Android build resolves AGP, AndroidX, Compose, Room
and DataStore from Google Maven, and Google Maven does not answer from every network this
project is developed on. Everything here resolves from Maven Central and the Gradle Plugin
Portal, and every version comes from `../gradle/libs.versions.toml`.

It is not a second app and it copies nothing. `build.gradle.kts` points at the modules'
source directories and leaves out the files that need Android (`*Screen.kt`, `*ViewModel.kt`,
Room, DataStore, the Keystore vault, Hilt modules that bind Android types). What remains — the
generated P10 client, the adapters and mappers in `:core:network`, session coordination in
`:core:auth`, the cache-first helper, and the feature repositories and use cases — is compiled
and tested exactly as the app compiles it.

Passing here is **not** `BUILD_VERIFIED` for the app. It says nothing about Compose, Room,
DataStore, Hilt's generated graph, resources, the manifest, R8 or a device.

## Unit tests

```bash
gradle test
```

The module unit tests under `core/*/src/test` and `feature/*/src/test`, plus MockWebServer
tests that drive the generated client, the adapters and the app's own `NetworkModule` wiring.

## Connected suite

```bash
./scripts/e2e-android.sh      # from the repository root
```

Resets a PostGIS database, starts MinIO and Django with its WebSocket endpoint, seeds
`seed_e2e_mobile_fixtures`, and runs `gradle connectedCheck` here against it: sign-in, refresh
rotation, replay and revocation, registration and recovery, discovery with cursor paging,
ratings, the owner onboarding path with real uploads, and realtime events.

It expects the `p10pg` PostGIS and `p10redis` containers on the `p10net` network, as the Admin
suite does, and uses the backend's test-only `e2e_set_otp` command to learn a registration
code, because the development OTP provider deliberately delivers none.
