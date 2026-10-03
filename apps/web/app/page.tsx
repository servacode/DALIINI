import type { Metadata } from "next";
import { Suspense } from "react";
import Link from "next/link";
import { FacilityFilters } from "../components/facility-filters";
import { Slider } from "../components/slider";
import { DownloadCta, FacilityList, JsonLd, Unavailable } from "../components/ui";
import {
  getCategories,
  getContentPage,
  getFacilities,
  getProvinces,
  getSlides,
} from "../lib/api";
import { SITE_NAME, absoluteUrl, publicConfig } from "../lib/config";

/*
 * The opening line lives in the console under this slug, so the team can change what the page
 * says without a deployment. Until somebody writes it, the province's own name carries it.
 */
const INTRO_SLUG = "home-intro";

/*
 * The page reads its address (`?p`, `?c`, the filters), so it renders for each request; what is
 * cached, for five minutes, is every answer it asks the API for.
 */
export const revalidate = 300;

export const metadata: Metadata = { alternates: { canonical: "/" } };

/**
 * One page that changes under you.
 *
 * The province is chosen in the bar, its categories appear here, and pressing one brings that
 * category's facilities up below — without leaving the page. Both choices live in the address
 * (`?p=` and `?c=`), so the page is still rendered on the server, still works with the back
 * button, and a link to "pharmacies in Raqqa" is a link somebody can send.
 */
export default async function HomePage({
  searchParams,
}: {
  searchParams: Promise<{ p?: string; c?: string; open?: string; duty?: string }>;
}) {
  const { p, c, open, duty } = await searchParams;
  const provinces = await getProvinces();
  const chosen =
    provinces?.find((province) => province.code === p) ?? provinces?.[0] ?? null;

  const [slides, categories, intro] = await Promise.all([
    getSlides(chosen?.id),
    chosen ? getCategories(chosen.id) : Promise.resolve(null),
    /* The opening line, when the team has written one in the console. */
    getContentPage(INTRO_SLUG),
  ]);

  const category =
    categories?.find((item) => item.id === c) ?? categories?.[0] ?? null;
  const page =
    chosen && category
      ? await getFacilities({
          provinceId: chosen.id,
          categoryId: category.id,
          openNow: open === "1",
          dutyNow: duty === "1",
          limit: 12,
        })
      : null;

  return (
    <>
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "Organization",
          name: SITE_NAME,
          url: absoluteUrl("/"),
          logo: absoluteUrl("/icon.png"),
          email: publicConfig.supportEmail.includes("@") ? publicConfig.supportEmail : undefined,
        }}
      />

      <Slider slides={slides} />

      <div className="shell page page-full page-sections">
        {provinces === null ? (
          <Unavailable />
        ) : !chosen ? (
          <p>لم تُفعَّل أي محافظة بعد.</p>
        ) : (
          <>
            <section className="ask" aria-labelledby="categories-title">
              {/* The page's one h1: what this directory is, for the province in hand. Every
                  page needs exactly one, and this page had none at all. */}
              <h1 id="categories-title">
                {intro?.titleAr ? intro.titleAr : `دليلك الشامل في ${chosen.nameAr}`}
              </h1>
              {categories === null ? (
                <Unavailable />
              ) : categories.length === 0 ? (
                <p>لا توجد تصنيفات مُفعَّلة في هذه المحافظة بعد.</p>
              ) : (
                <ul className="category-strip">
                  {categories.map((item) => (
                    <li key={item.id}>
                      <Link
                        href={`/?p=${chosen.code}&c=${item.id}`}
                        scroll={false}
                        aria-current={item.id === category?.id ? "true" : undefined}
                      >
                        {item.nameAr}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            {category ? (
              <section aria-labelledby="facilities-title">
                {/* The category is named on the chip that is lit above, so naming it again in
                    sight only says the same word twice. It is still a heading, because a page
                    whose levels jump from h1 to h3 has lost its shape for anyone reading it
                    through a screen reader rather than looking at it. */}
                <h2 id="facilities-title" className="sr-only">{category.nameAr}</h2>
                <Suspense fallback={null}>
                  <FacilityFilters />
                </Suspense>
                {page === null ? (
                  <Unavailable />
                ) : page.items.length === 0 ? (
                  <p>
                    {open === "1" || duty === "1"
                      ? "لا توجد منشآت تطابق التصفية الآن."
                      : "لا توجد منشآت في هذا التصنيف بعد."}
                  </p>
                ) : (
                  <FacilityList items={page.items} />
                )}
              </section>
            ) : null}
          </>
        )}

        <DownloadCta />
      </div>
    </>
  );
}
