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

const API = "http://localhost:8000";
const MEDIA = "http://localhost:9000";

const apps = {
  admin: {
    filter: "@servacode/admin",
    port: 3000,
    env: {
      ADMIN_API_ORIGIN: API,
      ADMIN_PUBLIC_ORIGIN: "http://localhost:3000",
      ADMIN_PUBLIC_MEDIA_ORIGIN: MEDIA,
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
const child = spawn(
  "pnpm",
  ["--filter", app.filter, "exec", "next", "dev", "-p", String(app.port)],
  { env, stdio: "inherit", shell: process.platform === "win32" },
);
child.on("exit", (code) => process.exit(code ?? 1));
