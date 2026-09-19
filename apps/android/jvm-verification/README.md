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
`:core:auth`, the cache-first helper, the platform-free parts of `:core:maps` (camera policy,
MapView lifecycle steps, navigation models), and the feature repositories and use cases — is
compiled and tested exactly as the app compiles it.

Passing here is **not** `BUILD_VERIFIED` for the app. It proves portable and data-layer
integration only, and says nothing about Compose, Room, DataStore, Hilt's generated graph,
resources, the manifest, R8, FCM or a device.

## Unit tests

```bash
../gradlew -p . test
```

The module unit tests under `core/*/src/test` and `feature/*/src/test`, plus MockWebServer
tests that drive the generated client, the adapters and the app's own `NetworkModule` wiring.

## Connected suite

```bash
./scripts/e2e-android.sh      # from the repository root
```

Resets a PostGIS database, starts MinIO with a public-media bucket anyone may read and a
private-evidence bucket nobody may, starts Django with its WebSocket endpoint, seeds
`seed_e2e_fixtures` and `seed_e2e_mobile_fixtures`, and runs `connectedCheck` here against it:
sign-in, refresh rotation, replay and revocation, push tokens ending with their session,
registration and recovery, discovery with cursor paging, ratings, the owner onboarding path
with real uploads against a configured test requirement, and realtime events.

Then the hand-off, across the three real parties. `HandoffConnectedTest` runs with
`HANDOFF_PHASE=submit`: the owner submits a facility with a photo and evidence. The Admin's
production build runs `tests/e2e/handoff.spec.ts` in Chromium, where an operator opens the
evidence from the private bucket, audited, and approves. `HandoffConnectedTest` then runs with
`HANDOFF_PHASE=public`: the owner sees the approval and anyone finds the facility with its
photo. Last, the runner reads a public object and a private one without credentials: 200 and
403. Outside the runner the phase is unset and both halves are skipped.

It expects the `p10pg` PostGIS and `p10redis` containers on the `p10net` network, as the Admin
suite does, and uses the backend's test-only `e2e_set_otp` command to learn a registration
code, because the development OTP provider deliberately delivers none.
