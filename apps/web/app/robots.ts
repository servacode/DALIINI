import type { MetadataRoute } from "next";
import { absoluteUrl } from "../lib/config";

export default function robots(): MetadataRoute.Robots {
  return {
    // `/_next/` stays crawlable: it is the site's own scripts and styles, and a crawler that may
    // not fetch them renders every page unstyled and judges it by that.
    rules: [{ userAgent: "*", allow: "/", disallow: ["/api/"] }],
    sitemap: absoluteUrl("/sitemap.xml"),
  };
}
