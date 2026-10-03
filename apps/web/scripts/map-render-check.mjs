#!/usr/bin/env node
// Draw the base map in a real browser and check that its Arabic names come out shaped
// (DECISION-082).
//
//   node scripts/map-render-check.mjs --style <url> --out <dir> \
//     --view "raqqa:39.0089,35.9506,13.5:الرقة|شارع تل أبيض" [--view ...] [--min-arabic 3]
//
// The page uses the site's own MapLibre and RTL text plugin, from this app's node_modules, so
// what passes here is what visitors' browsers run. For each view it waits until the map is idle,
// then asks MapLibre which labels it actually placed, and fails unless:
//
// * the glyphs were requested in the Arabic presentation-form ranges (U+FB50 to U+FEFF), which
//   only happens once letters have been shaped into their joined forms, and every glyph
//   request answered 200;
// * the RTL text plugin the site ships loaded from where the site serves it. MapLibre 6 joins
//   Arabic letters by itself (measured 2026-10-03: the labels came out joined with the plugin
//   missing), so this checks the site's own setup rather than the shaping;
// * every name the view expects was placed, and at least --min-arabic Arabic labels in all;
// * the map reported no error.
//
// A screenshot of each view lands in --out for a person to look at.
import { chromium } from "@playwright/test";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { createServer } from "node:http";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const app = join(dirname(fileURLToPath(import.meta.url)), "..");
const dist = join(app, "node_modules", "maplibre-gl", "dist");
const rtl = join(app, "node_modules", "@mapbox", "mapbox-gl-rtl-text", "dist", "mapbox-gl-rtl-text.js");
const ARABIC = /[؀-ۿ]/;
const PRESENTATION_FORMS = 0xfb50;

function options(argv) {
  const parsed = { views: [], minArabic: 1 };
  for (let i = 0; i < argv.length; i += 2) {
    const [key, value] = [argv[i], argv[i + 1]];
    if (key === "--style") parsed.style = value;
    else if (key === "--out") parsed.out = value;
    else if (key === "--min-arabic") parsed.minArabic = Number(value);
    else if (key === "--view") {
      const [name, at, expected = ""] = value.split(":");
      const [lon, lat, zoom] = at.split(",").map(Number);
      parsed.views.push({ name, center: [lon, lat], zoom, expected: expected.split("|").filter(Boolean) });
    } else throw new Error(`unknown argument ${key}`);
  }
  if (!parsed.style || !parsed.out || parsed.views.length === 0) {
    throw new Error("usage: map-render-check.mjs --style <url> --out <dir> --view name:lon,lat,zoom[:names]");
  }
  return parsed;
}

const PAGE_HTML = `<!doctype html><html dir="rtl"><head><meta charset="utf-8">
<link rel="stylesheet" href="/maplibre-gl.css">
<style>html,body,#map{margin:0;width:100%;height:100%}</style></head>
<body><div id="map"></div><script type="module">
import * as maplibregl from "/maplibre-gl.mjs";
maplibregl.setWorkerUrl("/maplibre-gl-worker.mjs");
window.maplibregl = maplibregl;
window.rtlLoaded = maplibregl.setRTLTextPlugin("/mapbox-gl-rtl-text.js", false);
</script></body></html>`;

const FILES = {
  "/": [PAGE_HTML, "text/html"],
  "/maplibre-gl.css": [join(dist, "maplibre-gl.css"), "text/css"],
  "/maplibre-gl.mjs": [join(dist, "maplibre-gl.mjs"), "text/javascript"],
  "/maplibre-gl-shared.mjs": [join(dist, "maplibre-gl-shared.mjs"), "text/javascript"],
  "/maplibre-gl-worker.mjs": [join(dist, "maplibre-gl-worker.mjs"), "text/javascript"],
  "/mapbox-gl-rtl-text.js": [rtl, "text/javascript"],
};

async function main() {
  const { style, out, views, minArabic } = options(process.argv.slice(2));
  mkdirSync(out, { recursive: true });
  // The page is served from this machine, as a real origin: Chromium treats a page it cannot
  // place on the network as public, and refuses its requests to a local map host.
  const server = createServer((request, response) => {
    const file = FILES[new URL(request.url, "http://localhost").pathname];
    if (!file) {
      response.writeHead(404).end();
      return;
    }
    const [source, contentType] = file;
    response.writeHead(200, { "content-type": contentType });
    response.end(source === PAGE_HTML ? source : readFileSync(source));
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const pageOrigin = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({
    // Software WebGL: CI machines have no GPU.
    args: ["--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--ignore-gpu-blocklist"],
  });
  const context = await browser.newContext({
    viewport: { width: 900, height: 640 },
    deviceScaleFactor: 2,
    locale: "ar",
    // The smoke's map host has a certificate from Caddy's local authority.
    ignoreHTTPSErrors: true,
  });
  const glyphs = [];
  context.on("response", (response) => {
    const match = /\/font\/[^/]+\/(\d+)-(\d+)/.exec(new URL(response.url()).pathname);
    if (match) glyphs.push({ start: Number(match[1]), status: response.status() });
  });

  const page = await context.newPage();
  const consoleErrors = [];
  page.on("pageerror", (error) => consoleErrors.push(String(error)));
  await page.goto(`${pageOrigin}/`);
  await page.waitForFunction(() => window.maplibregl !== undefined);

  const failures = [];
  const results = [];
  for (const view of views) {
    glyphs.length = 0;
    const result = await page.evaluate(
      async ({ style, center, zoom }) => {
        await window.rtlLoaded.catch(() => undefined);
        document.getElementById("map").replaceChildren();
        const errors = [];
        const map = new window.maplibregl.Map({
          container: "map",
          style,
          center,
          zoom,
          attributionControl: false,
          canvasContextAttributes: { preserveDrawingBuffer: true },
        });
        map.on("error", (event) => errors.push(String(event.error?.message ?? event.error ?? event)));
        await new Promise((resolve) => {
          const timer = setTimeout(resolve, 60_000);
          map.once("idle", () => {
            clearTimeout(timer);
            resolve();
          });
        });
        if (!map.isStyleLoaded()) return { rtl: window.maplibregl.getRTLTextPluginStatus(), loaded: false, placed: [], errors };
        const symbolLayers = map.getStyle().layers.filter((layer) => layer.type === "symbol").map((layer) => layer.id);
        const placed = map
          .queryRenderedFeatures({ layers: symbolLayers })
          .map((feature) => feature.properties["name:ar"] ?? feature.properties.name)
          .filter(Boolean);
        return {
          rtl: window.maplibregl.getRTLTextPluginStatus(),
          loaded: map.loaded(),
          placed: [...new Set(placed)],
          errors,
        };
      },
      { style, center: view.center, zoom: view.zoom },
    );
    const screenshot = join(out, `${view.name}.png`);
    await page.screenshot({ path: screenshot });

    const arabic = result.placed.filter((name) => ARABIC.test(name));
    const shaped = glyphs.some((glyph) => glyph.start >= PRESENTATION_FORMS);
    const glyphFailures = glyphs.filter((glyph) => glyph.status !== 200);
    const missing = view.expected.filter((name) => !result.placed.includes(name));
    const problems = [
      result.rtl !== "loaded" && `the RTL text plugin is ${result.rtl}`,
      !result.loaded && "the map never finished loading",
      !shaped && "no glyphs in the Arabic presentation forms were requested: letters were not joined",
      glyphFailures.length > 0 && `glyph requests failed: ${JSON.stringify(glyphFailures)}`,
      missing.length > 0 && `not placed: ${missing.join("، ")}`,
      arabic.length < minArabic && `only ${arabic.length} Arabic labels placed (want ${minArabic})`,
      ...result.errors.map((error) => `map error: ${error}`),
    ].filter(Boolean);
    results.push({ view: view.name, placed: result.placed, glyphRanges: glyphs.map((g) => g.start), screenshot, problems });
    for (const problem of problems) failures.push(`${view.name}: ${problem}`);
    console.log(`${problems.length ? "FAIL" : "ok  "}  ${view.name}: ${arabic.length} Arabic labels — ${arabic.slice(0, 8).join("، ")}`);
  }
  await browser.close();
  server.close();
  for (const error of consoleErrors) failures.push(`page error: ${error}`);
  writeFileSync(join(out, "result.json"), JSON.stringify({ style, results, failures }, null, 2));
  if (failures.length) {
    for (const failure of failures) console.log(`FAIL  ${failure}`);
    process.exit(1);
  }
  console.log("map labels PASS");
}

await main();
