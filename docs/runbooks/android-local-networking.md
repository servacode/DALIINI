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

## Physical phone, over USB or Wi-Fi debugging
`scripts/android-link.sh` does step 2 and then proves it from the phone, which is the only
answer that counts; `--check` reports without changing anything. Run it again after the phone
disconnects. The steps it automates:

1. Enable USB debugging and connect the phone, or pair Wi-Fi debugging; `adb devices` must list
   it. Over Wi-Fi one phone often appears **twice**, once per mDNS registration, and every adb
   command then fails with "more than one device": name one with `-t <transport_id>` from
   `adb devices -l`. Either entry reaches the same phone, and `adb reverse` works over Wi-Fi
   exactly as it does over the cable.
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
5. `adb reverse` does not survive a reconnect; repeat step 2 after the cable is unplugged, the
   phone sleeps off Wi-Fi debugging, or the adb server restarts. Until it is repeated the app
   reaches nothing and looks broken while every service on this machine is running perfectly —
   which is exactly how this presents itself.
6. An APK from CI needs no build here at all: install it with `adb install -r`, open the
   tunnels, and it is pointed at `localhost` already (the workflow sets
   `DIRECTORY_LOCAL_API_BASE_URL=http://localhost:8000/`).

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
`f875d994ddb187754c54b74968d6997c7d45dbca91c17a43d5501999ed248838`, which is what the Android
Build Verification workflow asserts on every APK it produces (`EXPECTED_DEBUG_CERT_SHA256`) and
what the APK installed on the test device carries. An earlier value recorded here
(`21e9ff5c…`) belonged to no key either of them uses.

## Push
Push needs a Firebase project, which does not exist yet. Pass
`DIRECTORY_FIREBASE_PROJECT_ID`, `DIRECTORY_FIREBASE_APPLICATION_ID`,
`DIRECTORY_FIREBASE_API_KEY` and `DIRECTORY_FIREBASE_SENDER_ID` as Gradle properties or
environment variables; never commit them. Without all four, the app runs with push off
(DECISION-038).

## Status
Written 2026-09-19 and **exercised on 2026-10-01**, on an SM-A525F over Wi-Fi debugging, with
an APK built by CI rather than on this machine (Google Maven answers 404 on this network, so
the app is not built here). What the run established:

- The three tunnels carry: the phone's own `localhost` reached the API, the media store and
  Valhalla, all 200.
- The app then loaded provinces, resolved its province from the device's position, and listed
  facilities and ads. `/account/notifications/unread-count/` answers 401 while nobody is signed
  in, which is correct.
- The app had never crashed. With no tunnels it simply reached nothing, which is indistinguishable
  from a broken app on the screen — hence `scripts/android-link.sh` and step 5 above.
- The signing fingerprint recorded here was wrong; it is corrected above.

Still unexercised: push (no Firebase project — see `docs/project/NOTIFICATIONS-SETUP.md`), and
anything behind signing in.
