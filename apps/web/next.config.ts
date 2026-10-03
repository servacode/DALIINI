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

/*
 * The base map (DECISION-071): the style's own host, and any other host its tiles, glyphs and
 * sprites come from (NEXT_PUBLIC_MAP_ORIGINS, comma-separated). The map fetches all of them, so
 * they are connect-src; the sprite sheet is also drawn as an image.
 */
const mapOrigins = [
  ...new Set(
    [originOf(process.env.NEXT_PUBLIC_MAP_STYLE_URL), ...(process.env.NEXT_PUBLIC_MAP_ORIGINS ?? "").split(",").map(originOf)].filter(Boolean),
  ),
];
const imageOrigins = [...new Set([...apiOrigins, mediaOrigin, ...mapOrigins].filter(Boolean))];
const withImages = (sources: string) => [sources, ...imageOrigins].join(" ");

const contentSecurityPolicy = [
  "default-src 'self'",
  `img-src ${withImages("'self' data:")}`,
  "style-src 'self' 'unsafe-inline'",
  // Next streams each page's server-component payload as inline <script> tags. The
  // admin console signs those with a per-request nonce, but these pages are cached
  // (ISR) and a cached page has no request to mint a nonce for, so they are allowed
  // inline. The risk that normally carries is script injection through rendered data;
  // React escapes everything it renders here, and dangerouslySetInnerHTML is used only for
  // JSON-LD built from JSON.stringify. The one inline script of the site's own is the fixed
  // theme script (THEME_SCRIPT, through next/script), which interpolates nothing a visitor or
  // the API supplies.
  // `next dev` alone needs eval for React's debugging call stacks, as the console allows.
  `script-src 'self' 'unsafe-inline'${process.env.NODE_ENV === "development" ? " 'unsafe-eval'" : ""}`,
  `connect-src ${[withApi("'self'"), ...mapOrigins].join(" ")}`,
  // The map's worker is a module served from this origin (public/vendor); MapLibre may also
  // start one from a blob when the browser cannot load a module worker.
  "worker-src 'self' blob:",
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
