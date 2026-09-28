export const publicConfig = {
  rootDomain: process.env.NEXT_PUBLIC_ROOT_DOMAIN ?? "ROOT_DOMAIN",
  supportEmail: process.env.SUPPORT_EMAIL ?? "SUPPORT_EMAIL",
  privacyEmail: process.env.PRIVACY_CONTACT_EMAIL ?? "PRIVACY_CONTACT_EMAIL",
  /* Store links are optional; the download CTA hides any link left unset. */
  playStoreUrl: process.env.NEXT_PUBLIC_PLAY_STORE_URL?.trim() || null,
  appStoreUrl: process.env.NEXT_PUBLIC_APP_STORE_URL?.trim() || null,
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
