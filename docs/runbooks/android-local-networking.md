# Android Local Networking Runbook

How a `local` build of the Android app reaches a backend running on a developer machine. No
machine address is committed anywhere. The `local` flavor defaults to the emulator's alias
for the host, and anything else is passed to the build as a Gradle property.

## Rules
- Cleartext exists only in the `local` flavor, and only towards `10.0.2.2` and `localhost`
  (`apps/android/app/src/local/res/xml/network_security_config.xml`). `staging` and
  `production` refuse it.
- Never put a LAN address in source, in the network security config or in a committed
  `gradle.properties`. A phone reaches the machine through `adb reverse`, as below.
- The backend's `S3_PUBLIC_MEDIA_BASE_URL` must be an address the device can reach, or photos
  will not load. Private evidence has no public address at all, by design.

## Emulator
1. Start the stack: Django on port 8000 and MinIO on port 9000 (as `scripts/e2e-android.sh`
   does), with `S3_PUBLIC_MEDIA_BASE_URL=http://10.0.2.2:9000/directory-public`.
2. Build and install with the defaults: `./gradlew :app:installLocalDebug`.
   The API is `http://10.0.2.2:8000/` and the socket `ws://10.0.2.2:8000/ws/v1/directory/`.

## Physical phone over USB
1. Enable USB debugging and connect the phone; `adb devices` must list it.
2. Forward the phone's own ports to the machine:
   `adb reverse tcp:8000 tcp:8000`, `adb reverse tcp:9000 tcp:9000` and, when routes
   are wanted, `adb reverse tcp:8002 tcp:8002` for Valhalla (`scripts/valhalla.sh up`).
3. Start the stack with `S3_PUBLIC_MEDIA_BASE_URL=http://localhost:9000/directory-public`.
4. Build and install against `localhost`, which the phone now forwards to the machine:
   ```bash
   ./gradlew :app:installLocalDebug \
     -PDIRECTORY_LOCAL_API_BASE_URL=http://localhost:8000/ \
     -PDIRECTORY_LOCAL_REALTIME_WS_URL=ws://localhost:8000/ws/v1/directory/ \
     -PDIRECTORY_LOCAL_ROUTING_BASE_URL=http://localhost:8002/
   ```
5. `adb reverse` does not survive a reconnect; repeat step 2 after the cable is unplugged.

## Signing: the same debug key as CI
CI signs every debug APK with the project's stable debug key (DECISION-041), so a device updates
with `adb install -r` and keeps its data. A debug build made on an authorised machine must use
the same key, or it cannot update an app installed from CI. Set these four in
`~/.gradle/gradle.properties` (never in the repository), pointing at the private copy of the
keystore:

```
DIRECTORY_DEBUG_KEYSTORE_PATH=<path to directory-platform-debug.keystore>
DIRECTORY_DEBUG_KEYSTORE_PASSWORD=<password>
DIRECTORY_DEBUG_KEY_ALIAS=directory-debug
DIRECTORY_DEBUG_KEY_PASSWORD=<password>
```

Check with `apksigner verify --print-certs <apk>`: the certificate SHA-256 must be
`21e9ff5c409a14cec929bb078d53625f4e3142ef1bb027337360e9e6d5b62caa`.

## Push
Push needs a Firebase project, which does not exist yet. Pass
`DIRECTORY_FIREBASE_PROJECT_ID`, `DIRECTORY_FIREBASE_APPLICATION_ID`,
`DIRECTORY_FIREBASE_API_KEY` and `DIRECTORY_FIREBASE_SENDER_ID` as Gradle properties or
environment variables; never commit them. Without all four, the app runs with push off
(DECISION-038).

## Status
Written 2026-09-19 and not yet exercised: the Android app cannot be built on the network it
was written on, because Google Maven answers 404 there. Verify this runbook on the first
device run.
