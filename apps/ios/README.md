# دليني for iPhone

The iPhone app is the Android app's own Kotlin (DECISION-051). Almost nothing here is Swift. The
SwiftUI file gives the shared screens a window, and the tests run inside the app. Everything the
app does lives in the Kotlin Multiplatform modules under `apps/android`. They are built into
one static framework, `DaliiniKit`, by `:ios-framework` (DECISION-093).

## Building it

On a Mac with Xcode 26, JDK 21 and [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
cd apps/ios
xcodegen generate          # writes Daliini.xcodeproj from project.yml; it is not committed
open Daliini.xcodeproj     # or: xcodebuild test -scheme Daliini -destination 'platform=iOS Simulator,name=iPhone 16'
```

Xcode builds the Kotlin framework itself, before it compiles the app. It runs
`./gradlew :ios-framework:embedAndSignAppleFrameworkForXcode` from `apps/android`.

## What it is pointed at

`Config/Debug.xcconfig` points the app at a backend on the same Mac (`pnpm stack:up`), which
the simulator reaches through the Mac's own loopback. `Config/Release.xcconfig` holds a
placeholder. The app refuses the placeholder and says it is not connected to a server, so a
release is pointed at the real API by the build that makes it, never by this repository.

## What is checked

The `ios-app` job of the Android Build Verification workflow:

1. It generates the project.
2. It builds the app and the framework for the simulator.
3. It runs `DaliiniTests` inside the app:
   * the refresh secret kept in the Keychain and removed again;
   * the build's configuration;
   * every word the screens use present in `ar.lproj/Shell.strings`;
   * the shared screens loading in their view controller.

The shared modules' own tests run on the simulator in the `ios-shared` job.

## Not yet

* The real screens. Android's screens move to Compose Multiplatform and replace the shell's two,
  which already draw with the shared design system (DECISION-094).
* The map, notices, and signing for the App Store. Signing needs the owner's Apple developer
  account.
