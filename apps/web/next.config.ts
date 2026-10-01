import path from "node:path";
import type { NextConfig } from "next";

/**
 * The public site renders on the server; the only request a page sends from the browser
 * to another origin is the contact form's POST to NEXT_PUBLIC_API_ORIGIN. So
 * `connect-src` and `img-src` stay on 'self' plus the API: the origins of PUBLIC_API_ORIGIN
 * and NEXT_PUBLIC_API_ORIGIN when a deployment sets them (read at build time, because these
 * headers are baked into the build) — never a blanket `https:`.
 *
 * `img-src` carries one origin more: the media store. Slides and facility photographs are
 * served from object storage, which is a different host from the API — and while it was not
 * listed, every one of those images was blocked by the browser and the page showed alt text
 * where a photograph should be. NEXT_PUBLIC_MEDIA_ORIGIN names it; unset, nothing is added.
 */
function originOf(raw: string | undefined): string {
  const value = raw?.trim();
  if (!value || value.includes("ROOT_DOMAIN")) return "";
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:" ? url.origin : "";
  } catch {
    return "";
  }
}

const apiOrigins = [
  ...new Set([originOf(process.env.PUBLIC_API_ORIGIN), originOf(process.env.NEXT_PUBLIC_API_ORIGIN)].filter(Boolean)),
];
const withApi = (sources: string) => [sources, ...apiOrigins].join(" ");

/* Where the photographs are served from, when a deployment serves them elsewhere. */
const mediaOrigin = originOf(process.env.NEXT_PUBLIC_MEDIA_ORIGIN);
const imageOrigins = [...apiOrigins, mediaOrigin].filter(Boolean);
const withImages = (sources: string) => [sources, ...imageOrigins].join(" ");

const contentSecurityPolicy = [
  "default-src 'self'",
  `img-src ${withImages("'self' data:")}`,
  "style-src 'self' 'unsafe-inline'",
  // Next streams each page's server-component payload as inline <script> tags. The
  // admin console signs those with a per-request nonce, but these pages are cached
  // (ISR) and a cached page has no request to mint a nonce for, so they are allowed
  // inline. The risk that normally carries is script injection through rendered data;
  // React escapes everything it renders here and no page uses dangerouslySetInnerHTML
  // for anything but JSON-LD built from JSON.stringify.
  "script-src 'self' 'unsafe-inline'",
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
  // Next writes AGENTS.md and CLAUDE.md into this directory on every dev run unless told not
  // to. Nothing here is generated into the repository by a tool nobody asked.
  agentRules: false,
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
