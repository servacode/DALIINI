import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

/**
 * Unit tests for the Admin.
 *
 * Two projects in one run: the BFF and session modules are plain Node code and are tested
 * as such, while the components need a DOM. Keeping both under one `pnpm test` means
 * neither can be the one someone forgets to run.
 *
 * These cover logic. Anything that needs a real backend belongs in the Playwright suite,
 * which runs against Django rather than a stub.
 */

// `server-only` throws the moment it is imported outside a Next server bundle. These
// modules are the server side and are tested as plain Node, so the guard has nothing to
// protect here.
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
