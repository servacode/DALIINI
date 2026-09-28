import type { Metadata } from "next";
import Link from "next/link";
import { SearchForm } from "../../components/search-form";
import { Empty, FacilityList, Unavailable } from "../../components/ui";
import {
  MIN_QUERY_LENGTH,
  getCategoriesFor,
  getProvinces,
  isUuid,
  searchFacilities,
  type FacilityPage,
  type Province,
} from "../../lib/api";
import { pageMetadata } from "../../lib/seo";

/*
 * /search — a plain GET form rendered on the server. With one province chosen
 * the results page through the API cursor ("عرض المزيد"); across all provinces
 * each province shows its first few matches with a link to see all of them.
 * Result pages (anything with a term) stay out of search engines.
 */

type Params = { q?: string | string[]; province?: string | string[]; category?: string | string[]; cursor?: string | string[] };
type Props = { searchParams: Promise<Params> };

const one = (v: string | string[] | undefined) => (typeof v === "string" ? v : Array.isArray(v) ? v[0] : undefined);
const PER_PROVINCE = 5;

export async function generateMetadata({ searchParams }: Props): Promise<Metadata> {
  const q = one((await searchParams).q)?.trim().slice(0, 100);
  return pageMetadata({
    title: q ? `نتائج البحث عن «${q}»` : "ابحث في دليني",
    description: "ابحث عن صيدلية أو عيادة أو خدمة بالاسم أو الحي أو التصنيف، في كل المحافظات المتاحة.",
    path: "/search",
    noindex: Boolean(q),
  });
}

function queryString(params: Record<string, string | undefined>): string {
  const search = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v) search.set(k, v);
  return `/search?${search.toString()}`;
}

export default async function SearchPage({ searchParams }: Props) {
  const params = await searchParams;
  const q = (one(params.q) ?? "").trim().slice(0, 100);
  const provinceCode = one(params.province)?.toLowerCase();
  const rawCategory = one(params.category);
  const categoryId = rawCategory && isUuid(rawCategory) ? rawCategory : undefined;
  const cursor = one(params.cursor);

  const provinces = await getProvinces();
  if (provinces === null) {
    return (
      <div className="shell page">
        <h1>ابحث في دليني</h1>
        <SearchForm q={q} provinces={[]} categories={[]} />
        <div className="more"><Unavailable /></div>
      </div>
    );
  }

  /* One active province means there is nothing to choose. */
  const chosen: Province | undefined =
    provinces.length === 1 ? provinces[0] : provinces.find((p) => p.code.toLowerCase() === provinceCode);
  const categories = (await getCategoriesFor(chosen ? [chosen] : provinces)) ?? [];
  const category = categories.find((c) => c.id === categoryId);

  return (
    <div className="shell page">
      <h1>ابحث في دليني</h1>
      <p className="muted page-intro">ابحث بالاسم أو الحي أو نوع الخدمة، مثل «صيدلية» أو «أسنان».</p>
      <SearchForm q={q} province={chosen?.code} category={category?.id} provinces={provinces} categories={categories} />
      <Results q={q} provinces={provinces} chosen={chosen} categoryId={category?.id} categoryName={category?.nameAr} cursor={cursor} />
    </div>
  );
}

async function Results({
  q,
  provinces,
  chosen,
  categoryId,
  categoryName,
  cursor,
}: {
  q: string;
  provinces: Province[];
  chosen?: Province;
  categoryId?: string;
  categoryName?: string;
  cursor?: string;
}) {
  if (!q) {
    return (
      <p className="results-summary muted">
        اكتب ما تبحث عنه ثم اضغط «ابحث». تبحث عن صيدلية مفتوحة الليلة؟ <Link href="/duty">اعرض المناوبات</Link>
      </p>
    );
  }
  if (q.length < MIN_QUERY_LENGTH) {
    return <Empty illustration="noResults">اكتب حرفين على الأقل ثم ابحث مرة أخرى.</Empty>;
  }
  if (provinces.length === 0) {
    return <Empty>لم تُفعَّل أي محافظة بعد.</Empty>;
  }

  const where = [categoryName ? `ضمن ${categoryName}` : null, chosen ? `في ${chosen.nameAr}` : null].filter(Boolean).join(" ");
  const noResults = (
    <Empty illustration="noResults">
      لا توجد نتائج عن «{q}»{where ? ` ${where}` : ""}. جرّب كلمة أقصر أو تصنيفاً آخر
      {chosen && provinces.length > 1 ? <>، أو <Link href={queryString({ q })}>ابحث في كل المحافظات</Link></> : null}.
    </Empty>
  );

  if (chosen) {
    const page = await searchFacilities({ q, provinceId: chosen.id, categoryId, cursor });
    if (page === null) return <Unavailable />;
    if (page.items.length === 0) return noResults;
    const base = { q, province: provinces.length > 1 ? chosen.code : undefined, category: categoryId };
    return (
      <section aria-labelledby="results-title">
        <h2 id="results-title" className="results-summary">
          نتائج البحث عن «{q}»{where ? ` ${where}` : ""}
        </h2>
        <FacilityList items={page.items} />
        <div className="actions more">
          {cursor ? <Link className="button button-alt" href={queryString(base)}>العودة إلى البداية</Link> : null}
          {page.hasMore && page.nextCursor ? (
            <Link className="button" href={queryString({ ...base, cursor: page.nextCursor })} rel="next">عرض المزيد</Link>
          ) : null}
        </div>
      </section>
    );
  }

  const pages: (FacilityPage | null)[] = await Promise.all(
    provinces.map((p) => searchFacilities({ q, provinceId: p.id, categoryId, limit: PER_PROVINCE })),
  );
  if (pages.every((p) => p === null)) return <Unavailable />;
  const groups = provinces
    .map((province, i) => ({ province, page: pages[i] }))
    .filter((g): g is { province: Province; page: FacilityPage } => Boolean(g.page && g.page.items.length > 0));
  if (groups.length === 0) return noResults;

  return (
    <section aria-labelledby="results-title">
      <h2 id="results-title" className="results-summary">
        نتائج البحث عن «{q}»{categoryName ? ` ضمن ${categoryName}` : ""}
      </h2>
      {groups.map(({ province, page }) => (
        <section key={province.id} aria-labelledby={`r-${province.id}`}>
          <h3 id={`r-${province.id}`}>{province.nameAr}</h3>
          <FacilityList items={page.items} />
          {page.hasMore ? (
            <p className="more">
              <Link href={queryString({ q, province: province.code, category: categoryId })}>
                كل النتائج في {province.nameAr}
              </Link>
            </p>
          ) : null}
        </section>
      ))}
      {pages.some((p) => p === null) ? <p className="muted">تعذّر البحث في بعض المحافظات. حاول مرة أخرى بعد قليل.</p> : null}
    </section>
  );
}
