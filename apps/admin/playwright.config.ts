import { defineConfig, devices } from "@playwright/test";

/**
 * The Admin golden path, against the real thing.
 *
 * No mocks and no fixtures faked at the network boundary. These run a production `next
 * build` output against a real Django, a real PostGIS and a real Redis, because the point
 * of this suite is to catch what only appears when those four are wired together — a cookie
 * a browser refuses, a permission the backend enforces differently from the UI, a mutation
 * that reports success and changes nothing.
 *
 * Both servers are started outside this config, by `scripts/e2e.sh`, which also resets the
 * database and seeds the operators. Playwright only waits for them.
 */
export default defineConfig({
  testDir: "./tests/e2e",
  globalSetup: "./tests/e2e/global-setup.ts",
  fullyParallel: false,
  // Serial on purpose. These tests mutate shared state — a province is activated, a user is
  // blocked — and running them in parallel against one database would make each one depend
  // on the others' timing.
  workers: 1,
  retries: 0,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: [["list"]],
  use: {
    baseURL: process.env.ADMIN_E2E_URL ?? "http://localhost:3000",
    locale: "ar-SY",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
