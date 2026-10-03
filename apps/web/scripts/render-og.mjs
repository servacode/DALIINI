// Render scripts/og-card.html into app/opengraph-image.png, the card every shared link shows.
//
//   pnpm --filter @servacode/public-web og:render
//
// Run it after a change to the card, the brand tokens, the faces or the mark, and commit the PNG.
import { chromium } from "@playwright/test";
import { fileURLToPath } from "node:url";

const card = new URL("./og-card.html", import.meta.url);
const out = fileURLToPath(new URL("../app/opengraph-image.png", import.meta.url));

const browser = await chromium.launch(
  process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {},
);
const page = await browser.newPage({ viewport: { width: 1200, height: 630 } });
await page.goto(card.href, { waitUntil: "networkidle" });
await page.evaluate(() => document.fonts.ready);
await page.screenshot({ path: out });
await browser.close();
console.log(`wrote ${out}`);
