import type { Metadata } from "next";
import { SITE_NAME } from "./config";

/*
 * Per-page metadata. Next.js replaces (not merges) nested `openGraph` and
 * `twitter` objects, so every page sets the full set here to keep link
 * previews on WhatsApp/Facebook consistent. Relative URLs resolve against
 * `metadataBase` from the root layout. Because the override drops the
 * file-based app/opengraph-image, it is re-attached explicitly.
 */
const OG_IMAGE = { url: "/opengraph-image", width: 1200, height: 630, alt: SITE_NAME };

/* Metadata for pages whose data could not be loaded: keep them out of the index. */
export const UNAVAILABLE_METADATA: Metadata = { title: { absolute: SITE_NAME }, robots: { index: false, follow: false } };

export function pageMetadata(opts: { title: string; description: string; path: string; noindex?: boolean }): Metadata {
  return {
    title: opts.title,
    description: opts.description,
    alternates: { canonical: opts.path },
    openGraph: {
      type: "website",
      locale: "ar_SY",
      siteName: SITE_NAME,
      url: opts.path,
      title: opts.title,
      description: opts.description,
      images: [OG_IMAGE],
    },
    twitter: { card: "summary_large_image", title: opts.title, description: opts.description, images: [OG_IMAGE] },
    robots: opts.noindex ? { index: false, follow: true } : undefined,
  };
}
