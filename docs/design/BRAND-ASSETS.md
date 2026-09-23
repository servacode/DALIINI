# Brand assets

What the apps show as the brand, where each asset lives, and whether it is final. Nothing
listed as a placeholder may be shipped to a store as if it were approved.

## Mark

| Asset | Status | Where | Replace by |
|---|---|---|---|
| Brand mark: a location pin carrying a heart | **PLACEHOLDER** (2026-09-19, Screen 01) | `apps/android/core/designsystem/src/main/res/drawable/brand_mark.xml` | Replacing this one vector. The system splash and the app's splash (`BrandMark`) both draw it. Keep its layout: a 288-unit canvas with the mark in the middle 120 units, the margin inside the drawable, because Android 12+ scales a splash icon into a 288 dp box. `SplashThemeTest` fails if the canvas, the mark size or the centring drift. |
| Launcher icon | **MISSING** | — | Not part of Screen 01. Lint reports it (MissingApplicationIcon); the store listing needs a 512 × 512 icon (`20-GOOGLE-PLAY-RELEASE.md`). |

The placeholder uses only design-token colours (`colors.primary`, `colors.primarySoft`,
`semantic.surface.default`), through the colour resources the token generator emits for
Android (`packages/design-tokens/generated/android/`).

## Copy

No product copy has been approved. The splash words live in one place,
`feature/bootstrap/.../SplashGeometry.kt` (`SplashCopy`):

| Text | Status |
|---|---|
| الدليل | App name, as in the manifest label |
| أقرب الخدمات الصحية إليك | Proposed for the redesign, not approved copy |
| محافظة الرقة | Proposed launch footer, secondary text. Remove or replace when more provinces open |

## Typeface

The token font is Tajawal (`FONT-POLICY.md`), and the Android app now bundles it: three weights
in `apps/android/core/designsystem/src/main/res/font/`, under the Open Font Licence kept beside
them at `docs/design/fonts/Tajawal-OFL.txt`. It is set once, in `DirectoryTheme`, and every text
style in the app is built from it. No screen names a font of its own.
