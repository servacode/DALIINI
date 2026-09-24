# Brand assets

What the apps show as the brand, where each asset lives, and whether it is final. Nothing
listed as a placeholder may be shipped to a store as if it were approved.

## Mark

| Asset | Status | Where | Replace by |
|---|---|---|---|
| Brand symbol: the road and the pin inside the letter | **APPROVED** (2026-09-24, supplied by the owner) | `apps/android/core/designsystem/src/main/res/drawable-nodpi/brand_symbol.webp`, drawn by `BrandSymbol`. Everything the app itself draws uses this. The artwork is a raster with gradients and bevels that no vector reproduces, and the wordmark under it was left out on purpose: a name baked into a picture cannot be set in the app's typeface, read aloud by a screen reader, or corrected without an image editor | — |
| Splash vector: a location pin carrying a heart | **PLACEHOLDER** (2026-09-19, Screen 01) | `apps/android/core/designsystem/src/main/res/drawable/brand_mark.xml` | Replacing this one vector. The system splash and the app's splash (`BrandMark`) both draw it. Keep its layout: a 288-unit canvas with the mark in the middle 120 units, the margin inside the drawable, because Android 12+ scales a splash icon into a 288 dp box. `SplashThemeTest` fails if the canvas, the mark size or the centring drift. |
| Launcher icon | **MISSING** | — | Not part of Screen 01. Lint reports it (MissingApplicationIcon); the store listing needs a 512 × 512 icon (`20-GOOGLE-PLAY-RELEASE.md`). The approved symbol is square and 512 px, so it is what the icon should be cut from. |

The placeholder uses only design-token colours (`colors.primary`, `colors.primarySoft`,
`semantic.surface.default`), through the colour resources the token generator emits for
Android (`packages/design-tokens/generated/android/`).

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
| محافظة الرقة | Proposed launch footer, secondary text. Remove or replace when more provinces open |

## Typeface

The token font is Tajawal (`FONT-POLICY.md`), and the Android app now bundles it: three weights
in `apps/android/core/designsystem/src/main/res/font/`, under the Open Font Licence kept beside
them at `docs/design/fonts/Tajawal-OFL.txt`. It is set once, in `DirectoryTheme`, and every text
style in the app is built from it. No screen names a font of its own.
