#!/usr/bin/env node
// Render Google Play's feature graphic (1024 × 500) from the approved mark (DECISION-084).
//
//   node scripts/play-feature-graphic.mjs     # writes docs/design/brand/play-feature-graphic.png
//
// It lives beside the site's scripts because the site already carries a browser (Playwright).
// The picture is the mark on the bars' deep green, with roads like the one through the mark
// running to pins across it. No name and no words, by the brand's rule: Play prints the app's
// name beside the graphic, set in its own type (packages/design-tokens/brand/README.md).
import { chromium } from "@playwright/test";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const repo = join(dirname(fileURLToPath(import.meta.url)), "..", "..", "..");
const mark = readFileSync(join(repo, "packages/design-tokens/brand/mark-on-dark.svg"), "utf8");
const colors = JSON.parse(readFileSync(join(repo, "packages/design-tokens/tokens/colors.json"), "utf8"));
const out = join(repo, "docs/design/brand/play-feature-graphic.png");

// Roads across the banner, each ending at a small pin: the mark's own idea, repeated quietly.
const roads = [
  "M-20 400 C 120 360, 180 300, 280 300 S 350 270, 372 236",
  "M1044 90 C 930 120, 880 190, 780 186 S 690 214, 662 262",
  "M-20 120 C 90 140, 150 90, 240 110",
  "M1044 430 C 940 400, 900 450, 800 420",
];
const pins = [
  [240, 110],
  [800, 420],
  [372, 236],
  [662, 262],
];
const pin = (x, y) => `
  <g transform="translate(${x - 9}, ${y - 26})">
    <path d="M9 0C4 0 0 3.9 0 8.7 0 14.9 9 22.3 9 22.3s9-7.4 9-13.6C18 3.9 14 0 9 0z" fill="${colors.accent}" opacity="0.55"/>
    <circle cx="9" cy="8.6" r="3.2" fill="${colors.primaryDeep}"/>
  </g>`;

const html = `<!doctype html><html><head><meta charset="utf-8"><style>
  html, body { margin: 0; width: 1024px; height: 500px; overflow: hidden; }
  body { background: radial-gradient(120% 140% at 50% 45%, ${colors.primaryStrong} 0%, ${colors.primaryDeep} 62%); }
  .roads { position: absolute; inset: 0; }
  .mark { position: absolute; left: 50%; top: 50%; width: 300px; height: 300px; transform: translate(-50%, -50%); }
  .mark svg { width: 100%; height: 100%; }
</style></head><body>
  <svg class="roads" viewBox="0 0 1024 500" xmlns="http://www.w3.org/2000/svg">
    ${roads.map((d) => `<path d="${d}" fill="none" stroke="${colors.barContentMuted}" stroke-opacity="0.16" stroke-width="10" stroke-linecap="round"/>
    <path d="${d}" fill="none" stroke="${colors.primaryDeep}" stroke-opacity="0.5" stroke-width="1.6" stroke-dasharray="7 9" stroke-linecap="round"/>`).join("")}
    ${pins.map(([x, y]) => pin(x, y)).join("")}
  </svg>
  <div class="mark">${mark}</div>
</body></html>`;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1024, height: 500 }, deviceScaleFactor: 1 });
await page.setContent(html);
await page.screenshot({ path: out, omitBackground: false });
await browser.close();
console.log(`feature graphic: ${out}`);
