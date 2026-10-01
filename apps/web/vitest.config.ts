import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

/**
 * Unit tests for the public site.
 *
 * Two projects in one run, as in the Admin: the pure modules under `lib/` are plain Node code
 * and are tested as such, while anything that renders needs a DOM. One `pnpm test` runs both,
 * so neither can be the one somebody forgets.
 *
 * These cover logic — a number formatted the way it is spoken, an opening time read correctly
 * across midnight. What needs a real backend is not here; the site's pages are checked against
 * a running Django by the visual pass, and this is for the decisions made before any of that.
 */

// `server-only` throws the moment it is imported outside a Next server bundle. The modules
// under test that carry it are the server side, exercised here as plain Node.
const alias = {
  "server-only": new URL("./tests/stubs/server-only.ts", import.meta.url).pathname,
};

export default defineConfig({
  test: {
    projects: [
      {
        resolve: { alias },
        test: {
          name: "node",
          environment: "node",
          include: ["tests/unit/*.test.ts"],
          globals: true,
          restoreMocks: true,
        },
      },
      {
        plugins: [react()],
        resolve: { alias },
        test: {
          name: "components",
          environment: "jsdom",
          include: ["tests/unit/components/**/*.test.tsx"],
          globals: true,
          restoreMocks: true,
        },
      },
    ],
  },
});
