import { type NextRequest, NextResponse } from "next/server";

/**
 * Content Security Policy with a fresh nonce on every request.
 *
 * The previous policy was a static `script-src 'self'` in `next.config.ts`. The App Router
 * streams its React Server Component payload through inline `<script>` tags, and a static
 * policy without a nonce blocks every one of them — so the page rendered, React never
 * hydrated, and every button and form in the console was dead. The login form fell back to
 * a native submit that reloaded the page with the fields empty. No HTTP-level test can see
 * that; it took a real browser.
 *
 * A nonce fixes it without `'unsafe-inline'`. Next reads the nonce out of the request's
 * CSP header while rendering and stamps it on its own scripts, and `'strict-dynamic'` lets
 * those scripts load the chunks they need. An injected script has no nonce and does not
 * run. This is the approach the bundled Next 16 guide documents, and it requires dynamic
 * rendering, which the root layout enforces.
 *
 * Styles keep `'unsafe-inline'`, as before. The components use a handful of inline `style`
 * attributes, which a nonce cannot cover, and style injection is a far smaller risk than
 * script injection.
 *
 * HSTS and `upgrade-insecure-requests` are sent only when the Admin is served over https.
 * On a loopback http origin the first would be ignored by the browser and the second would
 * rewrite same-origin requests to an https port nothing listens on. INT-024.
 */

function isSecureDeployment(): boolean {
  const origin = process.env.ADMIN_PUBLIC_ORIGIN ?? "";
  return origin.startsWith("https://");
}

/**
 * The public-media origin (the CDN or bucket facility photos are served from), so a
 * reviewer can see the photos an application carries. Only that one origin is added to
 * `img-src`; it is public content by definition, and nothing else widens.
 */
function mediaOrigin(): string {
  const raw = process.env.ADMIN_PUBLIC_MEDIA_ORIGIN ?? "";
  try {
    return raw ? ` ${new URL(raw).origin}` : "";
  } catch {
    return "";
  }
}

/**
 * The facility map's hosts (DECISION-075): the style's own, and any other its tiles, glyphs and
 * sprites come from (`ADMIN_MAP_ORIGINS`, comma-separated). Nothing is added when no map is
 * configured, so a console without a map keeps the strict `connect-src 'self'`.
 */
export function mapOrigins(): string[] {
  const origins = [process.env.ADMIN_MAP_STYLE_URL ?? "", ...(process.env.ADMIN_MAP_ORIGINS ?? "").split(",")]
    .map((value) => {
      try {
        return value.trim() ? new URL(value.trim()).origin : "";
      } catch {
        return "";
      }
    })
    .filter(Boolean);
  return [...new Set(origins)];
}

export function buildContentSecurityPolicy(nonce: string, secure: boolean, dev: boolean): string {
  const map = mapOrigins();
  const extra = map.length > 0 ? ` ${map.join(" ")}` : "";
  return [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${dev ? " 'unsafe-eval'" : ""}`,
    "style-src 'self' 'unsafe-inline'",
    `img-src 'self' data: blob:${mediaOrigin()}${extra}`,
    "font-src 'self'",
    `connect-src 'self'${extra}`,
    // The map's worker is a module served from this origin (public/vendor); MapLibre may also
    // start one from a blob when the browser cannot load a module worker.
    "worker-src 'self' blob:",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
    ...(secure ? ["upgrade-insecure-requests"] : []),
  ].join("; ");
}

export function proxy(request: NextRequest): NextResponse {
  const nonce = btoa(crypto.randomUUID());
  const secure = isSecureDeployment();
  const policy = buildContentSecurityPolicy(
    nonce,
    secure,
    process.env.NODE_ENV === "development",
  );

  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", policy);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", policy);
  if (secure) {
    response.headers.set(
      "Strict-Transport-Security",
      "max-age=63072000; includeSubDomains",
    );
  }
  return response;
}

export const config = {
  matcher: [
    {
      // Pages only. API routes answer JSON and carry no scripts; static assets are
      // immutable files; a prefetch is not rendered for display.
      source: "/((?!api|_next/static|_next/image|favicon.ico).*)",
      missing: [
        { type: "header", key: "next-router-prefetch" },
        { type: "header", key: "purpose", value: "prefetch" },
      ],
    },
  ],
};
