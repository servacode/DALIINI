import { defineConfig, devices } from "@playwright/test";

/**
 * The public site, in a browser, against a real backend.
 *
 * The site's unit tests check decisions, and `scripts/verify-public-site.mjs` checks what the
 * server sends. Neither can see what a visitor actually gets: whether the page hydrates, so a
 * filter or a dialog responds at all; whether a link leads anywhere; whether something throws
 * once React runs. The console has already failed exactly that way — every page rendered,
 * nothing hydrated, and every control in the product was dead while every test stayed green.
 *
 * The server and the backend are started outside this config, by `scripts/verify-all.sh`,
 * which also seeds the data these tests expect. Playwright only drives the browser.
 *
 * Mobile first, because that is how this site is read: a phone viewport is the default
 * project, and a desktop one runs the same specs after it.
 */
export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: true,
  workers: 2,
  retries: 0,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: [["list"]],
  use: {
    baseURL: process.env.WEB_E2E_URL ?? "http://127.0.0.1:3000",
    locale: "ar-SY",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "phone", use: { ...devices["Pixel 7"] } },
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
  ],
});
