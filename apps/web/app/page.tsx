import type { Metadata } from "next";
import { Suspense } from "react";
import Link from "next/link";
import { FacilityFilters } from "../components/facility-filters";
import { FacilityMap } from "../components/facility-map";
import { SearchBox } from "../components/search-form";
import { Slider } from "../components/slider";
import { CategoryIcon, FacilityList, Icon, JsonLd, Unavailable } from "../components/ui";
import {
  getCategories,
  getContentPage,
  getFacilities,
  getProvinces,
  getSlides,
} from "../lib/api";
import { SITE_NAME, absoluteUrl, publicConfig } from "../lib/config";
import { pointsOf } from "../lib/map-points";
import { categoryPath } from "../lib/paths";

/*
 * The opening line lives in the console under this slug, so the team can change what the page
 * says without a deployment. Until somebody writes it, the province's own name carries it.
 */
const INTRO_SLUG = "home-intro";

/* How many of the pharmacies on duty right now the page shows before «كل المناوبات». */
const DUTY_PREVIEW = 6;

/*
 * The page reads its address (`?p`, `?c`, the filters), so it renders for each request; what is
 * cached, for five minutes, is every answer it asks the API for.
 */
export const revalidate = 300;

export const metadata: Metadata = { alternates: { canonical: "/" } };

/**
 * The home page answers, in order, what someone opens a health directory to ask (DECISION-070):
 *
 *  1. **Where do I find …?** A search box, first and large, on the brand's band.
 *  2. **Who is on duty right now?** The night question, answered before anything is browsed.
 *  3. **What is there?** The categories, each with its own mark and its own page.
 *  4. **Show me some.** One category's facilities, narrowed to open or on duty, without leaving.
 *  5. **And then?** The app, and, for an owner, how to add their own place.
 *
 * The province is chosen in the bar and carried in the address (`?p=`), as is the category
 * shown in (4) (`?c=`), so the page is rendered on the server, works with the back button, and
 * "pharmacies in Raqqa" is a link somebody can send.
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

  const [slides, categories, intro, onDuty] = await Promise.all([
    getSlides(chosen?.id),
    chosen ? getCategories(chosen.id) : Promise.resolve(null),
    /* The opening line, when the team has written one in the console. */
    getContentPage(INTRO_SLUG),
    chosen ? getFacilities({ provinceId: chosen.id, dutyNow: true, limit: DUTY_PREVIEW }) : null,
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
  const hasDuty = Boolean(categories?.some((item) => item.capabilities.duty));
  const appUrl = publicConfig.appDownloadUrl;

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

      <section className="hero home-hero" aria-labelledby="home-title">
        <div className="home-hero-inner">
          {/* The page's one h1: what this directory is, for the province in hand. */}
          <h1 id="home-title">
            {intro?.titleAr ? intro.titleAr : chosen ? `دليلك الصحي في ${chosen.nameAr}` : SITE_NAME}
          </h1>
          <p className="hero-lead">
            صيدليات وعيادات ومخابر، ومن هو مفتوح أو مناوب الآن، برقم هاتفه وطريقه.
          </p>
          <SearchBox id="home-q" variant="hero" />
          <nav className="hero-quick" aria-label="اختصارات">
            {hasDuty ? (
              <Link href="/duty">
                <Icon name="shield" size={16} />
                المناوبات الآن
              </Link>
            ) : null}
            {chosen && category ? (
              <Link href={`/?p=${chosen.code}&c=${category.id}&open=1#facilities-title`}>
                <Icon name="clock" size={16} />
                مفتوح الآن
              </Link>
            ) : null}
            <Link href="/emergency">
              <Icon name="emergency" size={16} />
              أرقام الطوارئ
            </Link>
          </nav>
        </div>
      </section>

      <div className="shell page page-full page-sections home-sections">
        {provinces === null ? (
          <Unavailable />
        ) : !chosen ? (
          <p>لم تُفعَّل أي محافظة بعد.</p>
        ) : (
          <>
            {hasDuty ? (
              <section className="home-section" aria-labelledby="duty-title">
                <div className="section-head">
                  <h2 id="duty-title" className="with-icon">
                    <Icon name="shield" size={22} />
                    مناوب الآن في {chosen.nameAr}
                  </h2>
                  <Link href="/duty/today" className="section-more">
                    جدول المناوبات
                    <Icon name="arrowBack" size={16} />
                  </Link>
                </div>
                {onDuty === null ? (
                  <Unavailable />
                ) : onDuty.items.length === 0 ? (
                  <p className="muted">
                    لا توجد صيدلية مناوبة مسجّلة في هذه الساعة. <Link href="/duty/today">مناوبات اليوم</Link>
                  </p>
                ) : (
                  <FacilityList items={onDuty.items} />
                )}
              </section>
            ) : null}

            {slides.length > 0 ? <Slider slides={slides} /> : null}

            {categories && categories.length > 0 ? (
              <section className="home-section" aria-labelledby="categories-title">
                <div className="section-head">
                  <h2 id="categories-title">تصفّح حسب التصنيف</h2>
                </div>
                <ul className="category-tiles">
                  {categories.map((item) => (
                    <li key={item.id}>
                      <Link href={categoryPath(chosen.code, item)} className="category-tile">
                        <span className="category-tile-icon">
                          <CategoryIcon iconKey={item.iconKey} size={26} />
                        </span>
                        <span>{item.nameAr}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              </section>
            ) : null}

            {categories === null ? (
              <Unavailable />
            ) : categories.length === 0 ? (
              <p>لا توجد تصنيفات مُفعَّلة في هذه المحافظة بعد.</p>
            ) : category ? (
              <section className="home-section" aria-labelledby="facilities-title">
                <div className="section-head">
                  <h2 id="facilities-title">في {chosen.nameAr} الآن</h2>
                  <Link href={categoryPath(chosen.code, category)} className="section-more">
                    كل {category.nameAr}
                    <Icon name="arrowBack" size={16} />
                  </Link>
                </div>
                <ul className="category-strip">
                  {categories.map((item) => (
                    <li key={item.id}>
                      <Link
                        href={`/?p=${chosen.code}&c=${item.id}#facilities-title`}
                        scroll={false}
                        aria-current={item.id === category.id ? "true" : undefined}
                      >
                        {item.nameAr}
                      </Link>
                    </li>
                  ))}
                </ul>
                <Suspense fallback={null}>
                  <FacilityFilters />
                </Suspense>
                {page === null ? (
                  <Unavailable />
                ) : page.items.length === 0 ? (
                  <p className="muted">
                    {open === "1" || duty === "1"
                      ? "لا توجد منشآت تطابق التصفية الآن."
                      : "لا توجد منشآت في هذا التصنيف بعد."}
                  </p>
                ) : (
                  <>
                    <FacilityMap
                      points={pointsOf(page.items)}
                      label={`${category.nameAr} في ${chosen.nameAr} على الخريطة`}
                    />
                    <FacilityList items={page.items} />
                  </>
                )}
              </section>
            ) : null}
          </>
        )}

        <section className="promo-pair" aria-label="التطبيق وأصحاب المنشآت">
          {appUrl ? (
            <div className="promo promo-app">
              <Icon name="download" size={28} />
              <h2>دليني على هاتفك</h2>
              <p>اعرف المناوب قربك، واحفظ منشآتك، واستلم تنبيه المناوبة، حتى بلا اتصال.</p>
              <a className="button" href={appUrl} rel="noopener">
                <Icon name="download" />
                حمّل التطبيق
              </a>
            </div>
          ) : null}
          <div className="promo promo-owners">
            <Icon name="building" size={28} />
            <h2>صاحب صيدلية أو عيادة؟</h2>
            <p>أضف منشأتك وحدّث دوامها ومناوباتها بنفسك، ويراها الناس بعد التحقق منها.</p>
            <Link className="button button-alt" href="/owners">
              <Icon name="plus" />
              أضف منشأتك
            </Link>
          </div>
        </section>
      </div>
    </>
  );
}
