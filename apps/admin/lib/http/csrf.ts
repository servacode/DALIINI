import "server-only";

/**
 * Cross-origin protection for the BFF's cookie-authenticated mutations.
 *
 * The previous check compared `Origin` to `Host` but skipped the comparison entirely when
 * `Origin` was absent, which is fail-open: the one request that carries no `Origin` is
 * exactly the one a cross-site form submission produces. This version refuses instead.
 *
 * `SameSite=Strict` is on the session cookies too, but it is not relied on alone. It is a
 * browser behaviour, it has differed between browsers and versions, and it says nothing
 * about a request that never came from a browser.
 *
 * Three independent signals, each fail-closed, on unsafe methods only:
 *
 *  1. `Sec-Fetch-Site`, where the browser sends it, must say `same-origin`.
 *  2. `Origin` must be present and must match the host this request arrived on.
 *  3. `Content-Type` must be JSON, which a cross-site HTML form cannot set without a
 *     preflight the browser would then block.
 *
 * Safe methods are left alone. A GET that changes nothing needs no defence, and refusing
 * one would break ordinary navigation for no gain.
 */

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

export type OriginRejection = Readonly<{ code: string; reason: string }>;

/** The host this request actually arrived on, honouring a trusted reverse proxy. */
function effectiveHost(request: Request): string | null {
  const forwarded = request.headers.get("x-forwarded-host");
  if (forwarded) return forwarded.split(",")[0]!.trim().toLowerCase();
  return request.headers.get("host")?.trim().toLowerCase() ?? null;
}

/**
 * Returns null when the request may proceed, or the reason it may not.
 *
 * The reason is for the server log. What reaches the caller is a bare code, because a
 * precise description of which check failed is a free hint to whoever is probing.
 */
export function checkSameOrigin(request: Request): OriginRejection | null {
  if (SAFE_METHODS.has(request.method.toUpperCase())) return null;

  const fetchSite = request.headers.get("sec-fetch-site");
  if (fetchSite && fetchSite !== "same-origin") {
    return { code: "ORIGIN_REJECTED", reason: `Sec-Fetch-Site is ${fetchSite}` };
  }

  const host = effectiveHost(request);
  if (!host) {
    return { code: "ORIGIN_REJECTED", reason: "no Host header" };
  }

  const origin = request.headers.get("origin");
  if (!origin) {
    return { code: "ORIGIN_REJECTED", reason: "no Origin header on an unsafe method" };
  }
  let originHost: string;
  try {
    originHost = new URL(origin).host.toLowerCase();
  } catch {
    return { code: "ORIGIN_REJECTED", reason: "Origin is not a URL" };
  }
  if (originHost !== host) {
    return { code: "ORIGIN_REJECTED", reason: "Origin does not match Host" };
  }

  const contentType = request.headers.get("content-type") ?? "";
  if (!contentType.split(";")[0]!.trim().toLowerCase().startsWith("application/json")) {
    return { code: "ORIGIN_REJECTED", reason: `Content-Type is ${contentType || "absent"}` };
  }

  return null;
}
