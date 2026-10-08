#!/usr/bin/env node
// Start the admin console or the public site in development against the local stack.
//
//   pnpm dev:admin   ->  http://localhost:3000
//   pnpm dev:web     ->  http://localhost:3001
//
// Each app refuses to start without the addresses it talks to, and setting them by hand on
// every machine and shell (Windows included) is how a local run ends up pointed somewhere it
// should not be. These are the local stack's addresses (infrastructure/docker/compose.yml);
// anything already set in the environment wins.
import { spawn } from "node:child_process";

// 127.0.0.1 rather than localhost, deliberately. On Windows `localhost` resolves to ::1 first,
// and Docker Desktop advertises the IPv6 publisher without serving it — so every request waits
// out its timeout and fails, while the same call to 127.0.0.1 answers in milliseconds. The
// console's sign-in failed this way with «تعذر الوصول إلى الخدمة» while the API was perfectly
// healthy.
const API = "http://127.0.0.1:8000";
const MEDIA = "http://127.0.0.1:9000";

const apps = {
  admin: {
    filter: "@servacode/admin",
    port: 3000,
    env: {
      ADMIN_API_ORIGIN: API,
      ADMIN_PUBLIC_ORIGIN: "http://localhost:3000",
      ADMIN_PUBLIC_MEDIA_ORIGIN: MEDIA,
      // The map the facility windows place a pin on (DECISION-109), once
      // `scripts/local-map.sh <archive.pmtiles>` has put it in the media store. Its glyphs and
      // icons come from the owner's RahalGo map host, which the console's policy must allow.
      // Without the map built, the windows fall back to typing the two coordinates.
      ADMIN_MAP_STYLE_URL: `${MEDIA}/directory-public/map/style.json`,
      ADMIN_MAP_ORIGINS: "https://maps.rahalgo.com",
    },
  },
  web: {
    filter: "@servacode/public-web",
    port: 3001,
    env: {
      PUBLIC_API_ORIGIN: API,
      NEXT_PUBLIC_API_ORIGIN: API,
      NEXT_PUBLIC_MEDIA_ORIGIN: MEDIA,
      NEXT_PUBLIC_ROOT_DOMAIN: "localhost",
      // No map by default: the local style exists only once scripts/local-map.sh has put it in
      // the media store. After that, NEXT_PUBLIC_MAP_STYLE_URL=$MEDIA/directory-public/map/style.json.
    },
  },
};

const name = process.argv[2];
const app = apps[name];
if (!app) {
  console.error(`usage: node scripts/dev-app.mjs <${Object.keys(apps).join("|")}>`);
  process.exit(2);
}

const env = { ...app.env, ...process.env };
// The app's own `dev` script, not `next dev` directly: it first copies the map's worker and
// Arabic shaping into public/vendor, which the maps on both apps load (DECISION-071, 075).
const child = spawn(
  "pnpm",
  ["--filter", app.filter, "run", "dev", "-p", String(app.port)],
  { env, stdio: "inherit", shell: process.platform === "win32" },
);
child.on("exit", (code) => process.exit(code ?? 1));
