"""Cut every brand raster the platform ships from the one approved mark.

The mark is drawn once, as vectors: `packages/design-tokens/brand/mark.svg` — the letter, the road
that winds through it and the pin it leads to (DECISION-060). `docs/design/brand/dalini-mark.png`
is that file rendered at 2048 px, and everything else — the symbol the app draws, the launcher
icon at five densities, the round icon, the store's 512, the site's and the console's icons, the
web symbol — is a scaling of it, none drawn by hand. After changing the SVG, re-render the master
(see `docs/design/BRAND-ASSETS.md`), run this, and commit what it writes.

Two rules decide the geometry, and both come from Android rather than from taste:

* The name is never part of an asset. A name baked into a picture cannot be set in the app's
  typeface, read aloud, or corrected without an image editor, so every surface draws the mark and
  writes the name itself (`BrandLockup` on Android).
* On an adaptive icon the launcher shows the middle 72 of a 108 canvas and may mask it to a
  circle, so the mark is scaled until the circle that encloses its ink is 66 units across. That
  is the size Android guarantees is never clipped, whatever shape the launcher prefers.

Needs Pillow: `python -m pip install pillow`.
"""

from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

REPO = Path(__file__).resolve().parents[3]
ANDROID = REPO / "apps/android"
SOURCE = REPO / "docs/design/brand/dalini-mark.png"
TOKENS = REPO / "packages/design-tokens/tokens/colors.json"

SYMBOL = ANDROID / "core/designsystem/src/main/res/drawable-nodpi/brand_symbol.webp"
SPLASH = ANDROID / "core/designsystem/src/main/res/drawable-nodpi/splash_symbol.webp"
APP_RES = ANDROID / "app/src/main/res"
STORE_ICON = REPO / "docs/design/brand/play-store-icon.png"
WEB_SYMBOLS = REPO / "packages/design-tokens/brand"
WEB_ICONS = {
    # The tab icon of each site, and the one a phone puts on its home screen. Next.js serves each
    # file at its own path and writes the <link> for it.
    REPO / "apps/web/app/icon.png": 512,
    REPO / "apps/admin/app/icon.png": 512,
    REPO / "apps/web/app/apple-icon.png": 180,
}

# The adaptive canvas, the part of it a launcher shows, and the circle inside that part which
# no mask ever cuts into. Android's numbers, in the units it states them in.
CANVAS = 108
VIEWPORT = 72
SAFE = 66
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FOREGROUND = 432  # 108 units at xxxhdpi, the largest any launcher asks for.
# The splash: Android scales the icon into a 288 dp box and masks it, so the margin has to be
# inside the pixels. The mark is handed over at 90 dp and the app's own splash grows it to 120.
SPLASH_CANVAS = 288
SPLASH_MARK = 90
SPLASH_PIXELS = 864  # 288 dp at 3x, so the mark stays sharp where the system draws it largest.
INK = 24  # Below this the pixel is the artwork's glow fading out, not the mark.


def mark(source: Image.Image) -> Image.Image:
    """The mark, trimmed to its ink and centred on a square of its own.

    The letter, the road and the pin are separate shapes, so the whole picture is kept: the
    master holds the mark and nothing else.
    """
    letter = source.crop(source.split()[3].getbbox())
    side = max(letter.size)
    square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    square.paste(letter, ((side - letter.width) // 2, (side - letter.height) // 2), letter)
    return square


def enclosing_radius(image: Image.Image) -> float:
    """How far the ink reaches from the middle — the circle a mask must not cut into."""
    alpha = np.array(image.split()[3])
    rows, columns = np.nonzero(alpha > INK)
    middle = (alpha.shape[0] - 1) / 2
    return float(np.sqrt((columns - middle) ** 2 + (rows - middle) ** 2).max())


def centred(canvas_side: int, art: Image.Image, art_side: int, background=None) -> Image.Image:
    canvas = Image.new("RGBA", (canvas_side, canvas_side), background or (0, 0, 0, 0))
    scaled = art.resize((art_side, art_side), Image.LANCZOS)
    offset = (canvas_side - art_side) // 2
    canvas.paste(scaled, (offset, offset), scaled)
    return canvas


def rounded(image: Image.Image, radius: float) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, image.width - 1, image.height - 1), radius=radius, fill=255)
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    out.paste(image, (0, 0), mask)
    return out


def circular(image: Image.Image) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, image.width - 1, image.height - 1), fill=255)
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    out.paste(image, (0, 0), mask)
    return out


def write(image: Image.Image, path: Path, **options) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, **options)
    print(f"  {path.relative_to(REPO)}  {image.size[0]}x{image.size[1]}")


def main() -> int:
    background = json.loads(TOKENS.read_text(encoding="utf-8"))["primarySoft"]
    tint = tuple(int(background[index:index + 2], 16) for index in (1, 3, 5)) + (255,)
    letter = mark(Image.open(SOURCE).convert("RGBA"))

    print("the symbol the app draws:")
    write(letter.resize((512, 512), Image.LANCZOS), SYMBOL, format="WEBP", quality=94, method=6)

    # Scale by the ink's own circle rather than by its box: the box's corners are what a round
    # mask takes off, and this mark has ink in them.
    reach = enclosing_radius(letter) / (letter.width / 2)
    units = SAFE / reach
    ink_side = round(FOREGROUND * units / CANVAS)
    print(f"the adaptive foreground: ink reaches {reach:.2f} boxes, so the mark is {units:.1f} of {CANVAS}")
    write(centred(FOREGROUND, letter, ink_side), APP_RES / "drawable-nodpi/ic_launcher_foreground.webp",
          format="WEBP", quality=94, method=6)

    # A splash icon is masked, and this letter's ink reaches the corners of its box: a bitmap
    # drawn edge to edge comes back with its sides cut off, which is what happened on the A52.
    print("the mark the system splash hands over:")
    write(centred(SPLASH_PIXELS, letter, round(SPLASH_PIXELS * SPLASH_MARK / SPLASH_CANVAS)), SPLASH,
          format="WEBP", quality=94, method=6)

    print("the icon as launchers before Android 8 draw it, whole:")
    for density, side in DENSITIES.items():
        square = centred(side, letter, round(side * units / VIEWPORT), background=tint)
        write(rounded(square, side * 0.225), APP_RES / f"mipmap-{density}/ic_launcher.webp",
              format="WEBP", lossless=True, method=6)
        write(circular(square), APP_RES / f"mipmap-{density}/ic_launcher_round.webp",
              format="WEBP", lossless=True, method=6)

    print("the store's icon, square and opaque as Play requires:")
    store = centred(512, letter, round(512 * units / VIEWPORT), background=tint)
    write(store.convert("RGB"), STORE_ICON, format="PNG")

    print("the symbol the site and the console draw beside the name:")
    for side in (64, 128, 256):
        write(letter.resize((side, side), Image.LANCZOS), WEB_SYMBOLS / f"symbol-{side}.webp",
              format="WEBP", quality=94, method=6)

    print("the web icons, the same tile as the launcher's:")
    for path, side in WEB_ICONS.items():
        square = centred(side, letter, round(side * units / VIEWPORT), background=tint)
        # Apple draws its own corners and wants an opaque square; a tab icon keeps the rounding.
        icon = square.convert("RGB") if "apple" in path.name else rounded(square, side * 0.225)
        write(icon, path, format="PNG", optimize=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
