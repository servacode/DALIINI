# Brand assets

What the apps show as the brand, where each asset lives, and whether it is final. Nothing
listed as a placeholder may be shipped to a store as if it were approved.

## Mark

| Asset | Status | Where |
|---|---|---|
| The mark | **APPROVED** (identity v2, 2026-10-03, DECISION-060) | `packages/design-tokens/brand/mark.svg`, and `mark-on-dark.svg` for the bars and the dark theme: the letter, the road that winds through it and the gold pin it leads to, flat, drawn as vectors. It is the owner's artwork (`docs/design/brand/dalini-logo.png`, approved 2026-09-25) without the bevels and glow, which blurred into a smudge at icon sizes |
| The raster master | generated | `docs/design/brand/dalini-mark.png`, `mark.svg` rendered at 2048 px with a transparent ground. Every raster the platform ships is cut from it by `apps/android/scripts/build-brand-assets.py`; none is drawn by hand. To change the mark: edit the SVG, re-render the master at 2048 px, run the script, commit what it writes |
| Brand symbol | **APPROVED** | `apps/android/core/designsystem/src/androidMain/res/drawable-nodpi/brand_symbol.webp`, drawn by `BrandSymbol`, and `packages/design-tokens/brand/symbol-{64,128,256}.webp` for the site and the console. The name is never part of it: a name baked into a picture cannot be set in the brand's typeface, read aloud by a screen reader, or corrected without an image editor. `BrandLockup` writes the name instead |
| Splash mark | **APPROVED** | `apps/android/core/designsystem/src/androidMain/res/drawable/brand_mark.xml`, a bitmap of `splash_symbol.webp`. Android scales a splash icon into a 288 dp box and masks it to the launcher's shape, and this letter's ink reaches the corners of its box: drawn edge to edge it came back with its sides cut off, photographed on the A52. So the margin is inside the pixels — the mark is cut at 90 dp of a 288 dp canvas — rather than being a layer the system is free to ignore. The app's own splash then grows it to the 120 dp it draws, and `SplashThemeTest` fails if the baked size and the size it grows from stop agreeing |
| Launcher icon | **APPROVED** | `apps/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` from Android 8, over `drawable-nodpi/ic_launcher_foreground.webp` on `brand_launcher_background` (token `colors.primarySoft`); five densities of whole icons, square and round, for older launchers. The mark is scaled until the circle enclosing its ink is 66 of the 108-unit canvas — the part no launcher's mask cuts into, whatever shape it prefers |
| Store icon | **APPROVED** | `docs/design/brand/play-store-icon.png`, 512 × 512 and opaque, as Play requires (`20-GOOGLE-PLAY-RELEASE.md`) |
| Feature graphic | draft, for the owner's approval (DECISION-084) | `docs/design/brand/play-feature-graphic.png`, 1024 × 500: `mark-on-dark.svg` on the bars' deep green, with quiet roads ending at pins and kept clear of the mark. No name and no words: Play prints the name beside it. Rendered from the SVG and the colour tokens by `apps/web/scripts/play-feature-graphic.mjs`, never drawn by hand |
| Web icons | **APPROVED** | `apps/web/app/icon.png` and `apps/admin/app/icon.png` (512, rounded) and `apps/web/app/apple-icon.png` (180, opaque): the launcher's tile, from the same script |

There is no monochrome layer, so Android 13's themed icons fall back to the icon above. The
mark's identity is the road and the pin inside the letter, and a silhouette of it is a solid
blob: the tinting those icons apply would take away the only thing that makes it ours.

The colours around the mark are token colours through the resources the token generator emits
for Android (`packages/design-tokens/generated/android/`): the splash sits on
`semantic.surface.default` and the launcher icon on `colors.primarySoft`. The mark carries the
brand's own emerald and gold.

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

Two faces since identity v2 (`FONT-POLICY.md`): **Alexandria** for titles and big numbers, **IBM
Plex Sans Arabic** for everything read. Both ship from `packages/design-tokens/fonts/` — woff2
subsets for the web, static TTFs per weight for Android — under the Open Font Licence kept beside
them (`OFL-Alexandria.txt`, `OFL-IBMPlexSansArabic.txt`). Each typography role names its face in
the tokens; Android sets them once, in `DirectoryTheme`, and the web through the generated CSS
variables. No screen names a font of its own.
