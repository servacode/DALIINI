import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

export default defineConfig([
  ...nextVitals,
  ...nextTs,
  // public/vendor holds MapLibre's own built files, copied by scripts/vendor-map.mjs.
  globalIgnores([".next/**", "out/**", "build/**", "next-env.d.ts", "public/vendor/**"]),
]);
