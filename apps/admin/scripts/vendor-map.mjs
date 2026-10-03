#!/usr/bin/env node
// Serve the map's worker and its Arabic text shaping from the console itself, as the site does
// (DECISION-071; the console's facility map is DECISION-075).
//
// MapLibre 6 starts its worker from a file beside its own module, which a bundler does not keep;
// the page tells it where the copy is (`setWorkerUrl`). Arabic labels on the base map need the
// RTL text plugin, which MapLibre loads by URL. Both are copied from node_modules into
// public/vendor/maplibre-<version>/ on every `dev` and `build`, so the version in the lockfile is
// the version served, a new version gets a new path (nothing stale is cached), and nothing is
// fetched from a third-party CDN at run time.
import { copyFileSync, mkdirSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const app = join(dirname(fileURLToPath(import.meta.url)), "..");
// The app's own node_modules (pnpm links each dependency there); the RTL package exports no
// package.json, so the packages are found by directory rather than through require.resolve.
const maplibre = join(app, "node_modules", "maplibre-gl");
const rtl = join(app, "node_modules", "@mapbox", "mapbox-gl-rtl-text");
const version = JSON.parse(readFileSync(join(maplibre, "package.json"), "utf8")).version;

const out = join(app, "public", "vendor", `maplibre-${version}`);
mkdirSync(out, { recursive: true });
for (const file of ["maplibre-gl-worker.mjs", "maplibre-gl-shared.mjs"]) {
  copyFileSync(join(maplibre, "dist", file), join(out, file));
}
copyFileSync(join(rtl, "dist", "mapbox-gl-rtl-text.js"), join(out, "mapbox-gl-rtl-text.js"));
console.log(`map assets: public/vendor/maplibre-${version}/`);
