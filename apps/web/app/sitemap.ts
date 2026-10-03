import type { MetadataRoute } from "next";
import { getCategories, getContentPage, getFacilities, getProvinces } from "../lib/api";
import { absoluteUrl } from "../lib/config";
import { categoryPath, facilityPath } from "../lib/paths";

/*
 * Sitemap for static pages plus every province, province×category listing and
 * published facility. Facilities are walked through the cursor API per
 * category, capped at MAX_FACILITIES (one sitemap file holds up to 50k URLs;
 * split into generateSitemaps once the directory grows past the cap).
 *
 * Every API helper returns null on failure, so an unreachable API yields a
 * sitemap of static pages instead of failing the build or the request.
 */
export const revalidate = 3600;

const MAX_FACILITIES = 10000;
const PAGE_SIZE = 100;

/*
 * The console's built-in pages served at /p/<slug>. Pages the team adds under
 * other slugs cannot be listed through the public API, so they are not here.
 */
const CONTENT_PAGES = ["about", "instructions"];

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const entries: MetadataRoute.Sitemap = [
    { url: absoluteUrl("/"), changeFrequency: "daily", priority: 1 },
    { url: absoluteUrl("/duty"), changeFrequency: "hourly", priority: 0.9 },
    ...["/duty/today", "/duty/tomorrow", "/duty/week"].map((path) => ({
      url: absoluteUrl(path),
      changeFrequency: "hourly" as const,
      priority: 0.8,
    })),
    { url: absoluteUrl("/emergency"), changeFrequency: "weekly", priority: 0.7 },
    ...["/owners", "/how-we-verify", "/faq", "/contact"].map((path) => ({
      url: absoluteUrl(path),
      changeFrequency: "monthly" as const,
      priority: 0.5,
    })),
    ...["/support", "/privacy", "/terms", "/delete-account"].map((path) => ({
      url: absoluteUrl(path),
      changeFrequency: "monthly" as const,
      priority: 0.3,
    })),
  ];

  const pages = await Promise.all(CONTENT_PAGES.map((slug) => getContentPage(slug)));
  for (const page of pages) {
    if (page?.kind === "PAGE") entries.push({ url: absoluteUrl(`/p/${page.slug}`), changeFrequency: "monthly", priority: 0.4 });
  }

  const provinces = (await getProvinces()) ?? [];
  const seen = new Set<string>();
  for (const province of provinces) {
    entries.push({ url: absoluteUrl(`/${province.code}`), changeFrequency: "weekly", priority: 0.8 });
    for (const category of (await getCategories(province.id)) ?? []) {
      entries.push({ url: absoluteUrl(categoryPath(province.code, category)), changeFrequency: "daily", priority: 0.7 });
      let cursor: string | undefined;
      do {
        if (seen.size >= MAX_FACILITIES) break;
        const page = await getFacilities({ provinceId: province.id, categoryId: category.id, cursor, limit: PAGE_SIZE });
        if (!page) break;
        for (const f of page.items) {
          if (seen.has(f.id)) continue;
          seen.add(f.id);
          entries.push({ url: absoluteUrl(facilityPath(f)), changeFrequency: "weekly", priority: 0.6 });
        }
        cursor = page.hasMore && page.nextCursor ? page.nextCursor : undefined;
      } while (cursor);
    }
  }
  return entries;
}
