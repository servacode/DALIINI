export const publicConfig = {
  rootDomain: process.env.NEXT_PUBLIC_ROOT_DOMAIN ?? "ROOT_DOMAIN",
  supportEmail: process.env.SUPPORT_EMAIL ?? "SUPPORT_EMAIL",
  privacyEmail: process.env.PRIVACY_CONTACT_EMAIL ?? "PRIVACY_CONTACT_EMAIL",
  /* Store links are optional; the download CTA hides any link left unset. */
  playStoreUrl: process.env.NEXT_PUBLIC_PLAY_STORE_URL?.trim() || null,
  appStoreUrl: process.env.NEXT_PUBLIC_APP_STORE_URL?.trim() || null,
  /*
   * "Open in the app" on facility pages. Shown only when the Android package is set;
   * the scheme must match a deep link the app declares for facility/<id>.
   */
  androidPackage: process.env.NEXT_PUBLIC_ANDROID_PACKAGE?.trim() || null,
  appLinkScheme: process.env.NEXT_PUBLIC_APP_LINK_SCHEME?.trim() || "daliini",
} as const;

export const SITE_NAME = "دليني";

/*
 * Absolute origin used for canonical URLs, OpenGraph, sitemap and JSON-LD.
 * Falls back to a non-routable placeholder so builds never crash when the
 * domain is not configured yet (e.g. CI).
 */
export function siteUrl(): URL {
  const domain = publicConfig.rootDomain;
  if (!domain || domain === "ROOT_DOMAIN") return new URL("https://example.invalid");
  return new URL(/^https?:\/\//.test(domain) ? domain : `https://${domain}`);
}

export function absoluteUrl(path: string): string {
  return new URL(path, siteUrl()).toString();
}

/*
 * An Android intent link that opens the facility in the app, or falls back to the
 * store listing (or this page) when the app is not installed. null when unset.
 */
export function appOpenUrl(facilityId: string): string | null {
  const { androidPackage, appLinkScheme, playStoreUrl } = publicConfig;
  if (!androidPackage || !/^[A-Za-z0-9_.]+$/.test(androidPackage) || !/^[a-z][a-z0-9+.-]*$/i.test(appLinkScheme)) return null;
  const fallback = playStoreUrl ?? absoluteUrl(`/f/${facilityId}`);
  return (
    `intent://facility/${encodeURIComponent(facilityId)}#Intent;scheme=${appLinkScheme};package=${androidPackage};` +
    `S.browser_fallback_url=${encodeURIComponent(fallback)};end`
  );
}
