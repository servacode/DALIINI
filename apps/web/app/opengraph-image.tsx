import { ImageResponse } from "next/og";
import { readFile } from "node:fs/promises";
import { SITE_NAME } from "../lib/config";

/**
 * The picture a shared link shows.
 *
 * The site is built around sending someone a link — a pharmacy, a duty roster, a search. Every
 * one of those arrived in a chat as a bare URL with a blank grey rectangle over it, because the
 * metadata asked for `summary_large_image` and there was no large image to put in it. A card
 * declared and not supplied is worse than one never declared: the reader gets the empty frame.
 *
 * It is drawn rather than stored, so it cannot fall out of step with the brand: the colours are
 * the brand tokens' own and the type is the font the site itself is set in. Next renders it once
 * at build time and serves it as a static file.
 */

export const alt = SITE_NAME;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

/* The deep green of the bar and the gold the brand pairs with it. */
const BAR = "#042623";
const INK = "#f7f7f5";
const MUTED = "#9fb8ae";
const ACCENT = "#0b6b47";

const TAGLINE = "دليل الخدمات الصحية في سوريا";

/*
 * The TrueType cuts, not the site's own woff2: the renderer behind `ImageResponse` reads TTF,
 * OTF and WOFF and does not read WOFF2 — with one it draws every Arabic letter as a box. These
 * are the same typeface at the same weights, shipped for Android by the same package.
 */
async function tajawal(weight: "medium" | "bold") {
  return readFile(
    new URL(
      `../../../packages/design-tokens/fonts/android/font/tajawal_${weight}.ttf`,
      import.meta.url,
    ),
  );
}

export default async function OpengraphImage() {
  const [medium, bold] = await Promise.all([tajawal("medium"), tajawal("bold")]);

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          /* Everything hugs the right edge, stated rather than inherited: the renderer ignores
             `direction`, so an Arabic card left to it comes out aligned the wrong way. */
          alignItems: "flex-end",
          padding: "88px",
          background: BAR,
        }}
      >
        {/* The brand's green, as a mark rather than a logo file: one shape, no asset to go stale. */}
        <div
          style={{
            display: "flex",
            width: "92px",
            height: "10px",
            borderRadius: "5px",
            background: ACCENT,
            marginBottom: "48px",
          }}
        />
        <div style={{ display: "flex", fontSize: 104, fontWeight: 700, color: INK }}>
          {SITE_NAME}
        </div>
        {/*
          * Word by word, laid out in reverse.
          *
          * The renderer shapes Arabic letters correctly but lays words out left to right whatever
          * `direction` says, so a sentence came back with its words in the opposite order — fine
          * for the one-word name above, nonsense for a line. Each word is its own box here and
          * the row runs backwards, which puts them in the order they are read without relying on
          * the renderer having an opinion about bidi.
          */}
        <div
          style={{
            display: "flex",
            flexDirection: "row-reverse",
            gap: "16px",
            fontSize: 46,
            fontWeight: 500,
            color: MUTED,
            marginTop: "28px",
          }}
        >
          {TAGLINE.split(" ").map((word) => (
            <span key={word} style={{ display: "flex" }}>
              {word}
            </span>
          ))}
        </div>
      </div>
    ),
    {
      ...size,
      fonts: [
        { name: "Tajawal", data: medium, weight: 500, style: "normal" },
        { name: "Tajawal", data: bold, weight: 700, style: "normal" },
      ],
    },
  );
}
