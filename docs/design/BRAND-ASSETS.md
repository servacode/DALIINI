# Brand assets

What the apps show as the brand, where each asset lives, and whether it is final. Nothing
listed as a placeholder may be shipped to a store as if it were approved.

## Mark

| Asset | Status | Where | Replace by |
|---|---|---|---|
| Brand mark: a location pin carrying a heart | **PLACEHOLDER** (2026-09-19, Screen 01) | `apps/android/core/designsystem/src/main/res/drawable/brand_mark.xml` | Replacing this one vector. The system splash (`brand_mark_splash.xml`, an inset of it) and the app's splash (`BrandMark`) both draw it, at 120 dp. Keep the 120 × 120 viewport, or change `BrandMarkSize` and the inset together; `SplashThemeTest` fails if they drift. |
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

The token font is Tajawal (`FONT-POLICY.md`). The Android app does not bundle it yet, so every
screen, the splash included, uses the platform sans-serif through the design-system styles.
Bundling Tajawal changes every screen at once and belongs to its own step.
