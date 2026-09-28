import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Breadcrumbs, Empty, FacilityList, Unavailable } from "../../../components/ui";
import { getCategories, getFacilities, getProvinceByCode, isUuid } from "../../../lib/api";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../../lib/seo";

/*
 * /[province]/[category] — facilities of one category in one province.
 * Categories have no slug in the public API, so the stable category UUID is
 * the path segment. Pagination follows the API's opaque cursor via a plain
 * "load more" link (?cursor=…), so it works without client JavaScript.
 * Cursor pages are marked noindex; the canonical is always the first page.
 */
export const revalidate = 300;

type Props = {
  params: Promise<{ province: string; category: string }>;
  searchParams: Promise<{ cursor?: string | string[] }>;
};

async function resolve(code: string, categoryId: string) {
  const province = await getProvinceByCode(code);
  if (!province) return { province };
  if (!isUuid(categoryId)) return { province, category: undefined };
  const categories = await getCategories(province.id);
  return { province, category: categories === null ? null : categories.find((c) => c.id === categoryId) };
}

export async function generateMetadata({ params, searchParams }: Props): Promise<Metadata> {
  const { province: code, category: categoryId } = await params;
  const { cursor } = await searchParams;
  const { province, category } = await resolve(code, categoryId);
  if (!province || !category) return UNAVAILABLE_METADATA;
  return pageMetadata({
    title: `${category.nameAr} في ${province.nameAr}`,
    description: `قائمة ${category.nameAr} في ${province.nameAr} مع حالة الدوام الآن والتقييمات وأرقام التواصل.`,
    path: `/${province.code}/${category.id}`,
    noindex: Boolean(cursor),
  });
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const { province: code, category: categoryId } = await params;
  const { cursor: rawCursor } = await searchParams;
  const cursor = typeof rawCursor === "string" ? rawCursor : undefined;
  const { province, category } = await resolve(code, categoryId);
  if (province === undefined || category === undefined) notFound();
  if (province === null || category === null) return <div className="shell page"><Unavailable /></div>;

  const page = await getFacilities({ provinceId: province.id, categoryId: category.id, cursor });
  const base = `/${province.code}/${category.id}`;

  return (
    <div className="shell page">
      <Breadcrumbs items={[{ href: `/${province.code}`, label: province.nameAr }, { label: category.nameAr }]} />
      <h1>{category.nameAr} في {province.nameAr}</h1>
      {page === null ? (
        <Unavailable />
      ) : page.items.length === 0 ? (
        <Empty>لا توجد منشآت منشورة في هذا التصنيف بعد.</Empty>
      ) : (
        <>
          <FacilityList items={page.items} />
          <div className="actions more">
            {cursor ? <Link className="button button-alt" href={base}>العودة إلى البداية</Link> : null}
            {page.hasMore && page.nextCursor ? (
              <Link className="button" href={`${base}?cursor=${encodeURIComponent(page.nextCursor)}`} rel="next">
                عرض المزيد
              </Link>
            ) : null}
          </div>
        </>
      )}
    </div>
  );
}
