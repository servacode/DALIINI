# Brand assets

What the apps show as the brand, where each asset lives, and whether it is final. Nothing
listed as a placeholder may be shipped to a store as if it were approved.

## Mark

| Asset | Status | Where |
|---|---|---|
| The mark | **APPROVED** (2026-09-25; restored 2026-10-16, DECISION-120) | `docs/design/brand/dalini-logo.png`, the owner's artwork: the emerald letter with its bevel and glow, the road that winds through it and the pin it leads to, the name under it. A flat vector redraw replaced it on 2026-10-03 (DECISION-060) and was retired at the owner's word: the artwork is the brand |
| How the rasters are cut | generated | Every raster the platform ships is cut from the artwork by `apps/android/scripts/build-brand-assets.py`, none drawn by hand: the letter is the largest piece of ink in the picture, so the name under it is left out. To change the mark: replace the artwork, run the script, commit what it writes |
| Brand symbol | **APPROVED** | `apps/android/core/designsystem/src/androidMain/res/drawable-nodpi/brand_symbol.webp`, drawn by `BrandSymbol`, and `packages/design-tokens/brand/symbol-{64,128,256,512}.webp` for the site, the console and the cards. The name is never part of it: a name baked into a picture cannot be set in the brand's typeface, read aloud by a screen reader, or corrected without an image editor. `BrandLockup` writes the name instead |
| Splash mark | **APPROVED** | `apps/android/core/designsystem/src/androidMain/res/drawable/brand_mark.xml`, a bitmap of `splash_symbol.webp`. Android scales a splash icon into a 288 dp box and masks it to the launcher's shape, and this letter's ink reaches the corners of its box: drawn edge to edge it came back with its sides cut off, photographed on the A52. So the margin is inside the pixels — the mark is cut at 90 dp of a 288 dp canvas — rather than being a layer the system is free to ignore. The app's own splash then grows it to the 120 dp it draws, and `SplashThemeTest` fails if the baked size and the size it grows from stop agreeing |
| Launcher icon | **APPROVED** | `apps/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` from Android 8, over `drawable-nodpi/ic_launcher_foreground.webp` on `brand_launcher_background` (token `colors.primarySoft`); five densities of whole icons, square and round, for older launchers. The mark is scaled until the circle enclosing its ink is 66 of the 108-unit canvas — the part no launcher's mask cuts into, whatever shape it prefers |
| Store icon | **APPROVED** | `docs/design/brand/play-store-icon.png`, 512 × 512 and opaque, as Play requires (`20-GOOGLE-PLAY-RELEASE.md`) |
| Feature graphic | draft, for the owner's approval (DECISION-084) | `docs/design/brand/play-feature-graphic.png`, 1024 × 500: the symbol on the bars' deep green, with quiet roads ending at pins and kept clear of the mark. No name and no words: Play prints the name beside it. Rendered from `symbol-512.webp` and the colour tokens by `apps/web/scripts/play-feature-graphic.mjs`, never drawn by hand |
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
