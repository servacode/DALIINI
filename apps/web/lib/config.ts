/* An http(s) origin, or null for an empty, placeholder or malformed value. */
function originOf(raw: string | undefined): string | null {
  const value = raw?.trim();
  if (!value || value.includes("ROOT_DOMAIN")) return null;
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:" ? url.origin : null;
  } catch {
    return null;
  }
}

export const publicConfig = {
  rootDomain: process.env.NEXT_PUBLIC_ROOT_DOMAIN?.trim() || "ROOT_DOMAIN",
  supportEmail: process.env.SUPPORT_EMAIL?.trim() || "SUPPORT_EMAIL",
  privacyEmail: process.env.PRIVACY_CONTACT_EMAIL?.trim() || "PRIVACY_CONTACT_EMAIL",
  /* Store links are optional; the download CTA hides any link left unset. */
  playStoreUrl: process.env.NEXT_PUBLIC_PLAY_STORE_URL?.trim() || null,
  appStoreUrl: process.env.NEXT_PUBLIC_APP_STORE_URL?.trim() || null,
  /*
   * Where the bar's "download the app" button goes. A direct build while there is one, and the
   * Play Store once the app is listed — one setting to change rather than a button to move.
   */
  appDownloadUrl:
    process.env.NEXT_PUBLIC_APP_DOWNLOAD_URL?.trim() ||
    process.env.NEXT_PUBLIC_PLAY_STORE_URL?.trim() ||
    null,
  /*
   * The API as a visitor's browser reaches it, for the one request a page sends
   * from the browser: the contact form. Inlined at build time, when next.config.ts
   * also adds it to the CSP's connect-src. null hides the form.
   */
  browserApiOrigin: originOf(process.env.NEXT_PUBLIC_API_ORIGIN),
  /*
   * The Android app's package: "Open in the app" on facility pages and the
   * app-link statement at /.well-known/assetlinks.json. Unset hides both.
   */
  androidPackage: process.env.NEXT_PUBLIC_ANDROID_PACKAGE?.trim() || null,
  /*
   * The base map's MapLibre style (DECISION-071), the same one the app draws. Unset, the site
   * shows no map anywhere and everything else works as it did; a map is never the only way to a
   * place, since every pin's facility is also in the list beside it.
   */
  mapStyleUrl: originOf(process.env.NEXT_PUBLIC_MAP_STYLE_URL) ? process.env.NEXT_PUBLIC_MAP_STYLE_URL!.trim() : null,
} as const;

export const SITE_NAME = "دليني";

/* A Java package name: at least two dot-separated segments, each starting with a letter. */
const ANDROID_PACKAGE = /^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/;

/* The configured Android package when it is a valid package name, else null. */
export function androidPackage(): string | null {
  const value = publicConfig.androidPackage;
  return value && ANDROID_PACKAGE.test(value) ? value : null;
}

/*
 * Absolute origin used for canonical URLs, OpenGraph, sitemap and JSON-LD.
 * Falls back to a non-routable placeholder so builds never crash when the
 * domain is not configured yet (e.g. CI).
 */
const PLACEHOLDER_SITE = "https://example.invalid";

export function siteUrl(): URL {
  const domain = publicConfig.rootDomain;
  if (!domain || domain === "ROOT_DOMAIN") return new URL(PLACEHOLDER_SITE);
  return new URL(/^https?:\/\//.test(domain) ? domain : `https://${domain}`);
}

export function absoluteUrl(path: string): string {
  return new URL(path, siteUrl()).toString();
}

/* Where the contact form posts, straight from the visitor's browser; null when not configured. */
export function contactEndpoint(): string | null {
  const origin = publicConfig.browserApiOrigin;
  return origin ? `${origin}/api/v1/contact/` : null;
}

/*
 * «افتح في التطبيق» on Android: an intent for this page's own URL, addressed to
 * the app. The app claims the site's links (App Links, verified through
 * /.well-known/assetlinks.json), so the same URL opens the facility in the app;
 * without the app, Chrome follows browser_fallback_url, which is this very page.
 * null when the package or the site's domain is not configured.
 */
export function appOpenUrl(facilityId: string): string | null {
  const pkg = androidPackage();
  const site = siteUrl();
  if (!pkg || site.origin === PLACEHOLDER_SITE) return null;
  const path = `/f/${encodeURIComponent(facilityId)}`;
  const scheme = site.protocol.replace(/:$/, "");
  return (
    `intent://${site.host}${path}#Intent;scheme=${scheme};package=${pkg};` +
    `S.browser_fallback_url=${encodeURIComponent(absoluteUrl(path))};end`
  );
}
