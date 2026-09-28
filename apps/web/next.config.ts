import path from "node:path";
import type { NextConfig } from "next";

/**
 * The public site renders on the server and makes no browser-side requests to other
 * origins, so `connect-src` and `img-src` stay on 'self'. If a deployment sets
 * PUBLIC_API_ORIGIN (read at build time, because these headers are baked into the build),
 * that single origin is allowed as well — never a blanket `https:`.
 */
function apiOrigin(): string {
  const raw = process.env.PUBLIC_API_ORIGIN;
  if (!raw) return "";
  try {
    return new URL(raw).origin;
  } catch {
    return "";
  }
}

const extraOrigin = apiOrigin();
const withApi = (sources: string) => (extraOrigin ? `${sources} ${extraOrigin}` : sources);

const contentSecurityPolicy = [
  "default-src 'self'",
  `img-src ${withApi("'self' data:")}`,
  "style-src 'self' 'unsafe-inline'",
  "script-src 'self'",
  `connect-src ${withApi("'self'")}`,
  "frame-ancestors 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "object-src 'none'",
].join("; ");

const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "X-Frame-Options", value: "DENY" },
  // Browsers ignore HSTS on plain-http responses, so this is harmless for local dev.
  { key: "Strict-Transport-Security", value: "max-age=63072000; includeSubDomains" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
  { key: "Content-Security-Policy", value: contentSecurityPolicy },
];

const nextConfig: NextConfig = {
  poweredByHeader: false,
  // Standalone output lets the Docker runtime stage ship only the traced server files.
  // The tracing root is the monorepo root so workspace packages are traced too.
  output: "standalone",
  outputFileTracingRoot: path.join(__dirname, "../.."),
  transpilePackages: ["@servacode/design-tokens"],
  async headers() {
    return [{ source: "/(.*)", headers: securityHeaders }];
  },
};

export default nextConfig;
