import type { Metadata } from "next";
import { notFound, permanentRedirect } from "next/navigation";
import { PublishedArticle } from "../../../components/content";
import { Unavailable } from "../../../components/ui";
import { getContentPage, isContentSlug } from "../../../lib/api";
import { contentSummary } from "../../../lib/content";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../../lib/seo";

/*
 * /p/[slug] — any page of kind PAGE the team publishes from the console
 * (content/pages/<slug>/), such as /p/about. Rendered on demand and cached
 * (ISR). An unknown or unpublished slug, or a page of another kind, is a 404
 * (noindex). The built-in pages that have their own route go there instead.
 */
export const revalidate = 300;

type Props = { params: Promise<{ slug: string }> };

const OWN_ROUTE: Record<string, string> = { privacy: "/privacy", terms: "/terms", faq: "/faq", contact: "/contact" };

function normalise(raw: string): string {
  try {
    return decodeURIComponent(raw).trim().toLowerCase();
  } catch {
    return raw.toLowerCase();
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const slug = normalise((await params).slug);
  const page = OWN_ROUTE[slug] ? undefined : await getContentPage(slug);
  if (!page || page.kind !== "PAGE") return UNAVAILABLE_METADATA;
  return pageMetadata({ title: page.titleAr, description: contentSummary(page.bodyAr), path: `/p/${page.slug}` });
}

export default async function ContentRoute({ params }: Props) {
  const { slug: raw } = await params;
  const slug = normalise(raw);
  if (!isContentSlug(slug)) notFound();
  if (OWN_ROUTE[slug]) permanentRedirect(OWN_ROUTE[slug]);
  /* One address per page: /p/About and /p/%61bout land on /p/about. */
  if (slug !== raw) permanentRedirect(`/p/${slug}`);

  const page = await getContentPage(slug);
  if (page === null) return <div className="shell page"><Unavailable /></div>;
  if (page === undefined || page.kind !== "PAGE") notFound();
  return <PublishedArticle page={page} />;
}
