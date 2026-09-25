# Brand assets

What the apps show as the brand, where each asset lives, and whether it is final. Nothing
listed as a placeholder may be shipped to a store as if it were approved.

## Mark

| Asset | Status | Where |
|---|---|---|
| The artwork | **APPROVED** (2026-09-25, supplied by the owner) | `docs/design/brand/dalini-logo.png`. Every raster the app ships is cut from this one file by `apps/android/scripts/build-brand-assets.py`; none is drawn by hand. Replace the file, run the script, commit what it writes |
| Brand symbol: the road and the pin inside the letter | **APPROVED** | `apps/android/core/designsystem/src/main/res/drawable-nodpi/brand_symbol.webp`, drawn by `BrandSymbol`. The artwork is a raster with gradients and bevels that no vector reproduces, and the wordmark under it is left out on purpose: a name baked into a picture cannot be set in the app's typeface, read aloud by a screen reader, or corrected without an image editor. `BrandLockup` writes the name instead |
| Splash mark | **APPROVED** | `apps/android/core/designsystem/src/main/res/drawable/brand_mark.xml`, a bitmap of `splash_symbol.webp`. Android scales a splash icon into a 288 dp box and masks it to the launcher's shape, and this letter's ink reaches the corners of its box: drawn edge to edge it came back with its sides cut off, photographed on the A52. So the margin is inside the pixels — the mark is cut at 90 dp of a 288 dp canvas — rather than being a layer the system is free to ignore. The app's own splash then grows it to the 120 dp it draws, and `SplashThemeTest` fails if the baked size and the size it grows from stop agreeing |
| Launcher icon | **APPROVED** | `apps/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` from Android 8, over `drawable-nodpi/ic_launcher_foreground.webp` on `brand_launcher_background` (token `colors.primarySoft`); five densities of whole icons, square and round, for older launchers. The mark is scaled until the circle enclosing its ink is 66 of the 108-unit canvas — the part no launcher's mask cuts into, whatever shape it prefers |
| Store icon | **APPROVED** | `docs/design/brand/play-store-icon.png`, 512 × 512 and opaque, as Play requires (`20-GOOGLE-PLAY-RELEASE.md`) |

There is no monochrome layer, so Android 13's themed icons fall back to the icon above. The
mark's identity is the road and the pin inside the letter, and a silhouette of it is a solid
blob: the tinting those icons apply would take away the only thing that makes it ours.

The colours around the mark are token colours through the resources the token generator emits
for Android (`packages/design-tokens/generated/android/`): the splash sits on
`semantic.surface.default` and the launcher icon on `colors.primarySoft`. The mark itself is a
photograph of sorts and carries its own greens.

## Copy

**The name is دليني.** It lives in `core/model/.../DirectoryBrand.kt` and in
`app/src/main/res/values/strings.xml`, and in no third place — the manifest cannot read a Kotlin
constant, so the launcher label is a resource and `BrandNameTest` fails if the two disagree or if
any screen writes the name as a literal of its own.

It was written five ways before that: `الدليل` in the manifest, in the notification builder and
on the splash, and `دليلك` on Home and on the sign-in page. None of them was the name.

| Text | Status |
|---|---|
| دليني | **APPROVED** app name (2026-09-24). `DirectoryBrand.NAME` |
| أقرب الخدمات الصحية إليك | Proposed, not approved copy. `DirectoryBrand.TAGLINE` |
| محافظة الرقة | **REMOVED** from the splash (2026-09-25). The app opens in more than one province, and the one a reader is in is written on Home, where it can be changed |

## Typeface

The token font is Tajawal (`FONT-POLICY.md`), and the Android app now bundles it: three weights
in `apps/android/core/designsystem/src/main/res/font/`, under the Open Font Licence kept beside
them at `docs/design/fonts/Tajawal-OFL.txt`. It is set once, in `DirectoryTheme`, and every text
style in the app is built from it. No screen names a font of its own.
