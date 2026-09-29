import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Breadcrumbs, Empty, FacilityList, Unavailable } from "../../../components/ui";
import {
  type Category,
  type TagRef,
  getCategories,
  getCategoryTags,
  getFacilities,
  getProvinceByCode,
  isUuid,
  tagId,
} from "../../../lib/api";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../../lib/seo";

/*
 * /[province]/[category] — facilities of one category in one province.
 * Categories have no slug in the public API, so the stable category UUID is
 * the path segment. Pagination follows the API's opaque cursor via a plain
 * "load more" link (?cursor=…), so it works without client JavaScript.
 * Cursor pages are marked noindex; the canonical is always the first page.
 *
 * Where the category offers them (its specialtyFilter and serviceFilter
 * capabilities), specialty and service chips narrow the list: plain links to
 * ?specialty=<id> and ?service=<id>, the API's integer keys. A narrowed list is
 * noindex too, and keeps its filters on every "load more" link.
 */
export const revalidate = 300;

type Query = {
  cursor?: string | string[];
  specialty?: string | string[];
  service?: string | string[];
};

type Props = {
  params: Promise<{ province: string; category: string }>;
  searchParams: Promise<Query>;
};

type Filters = { specialty?: number; service?: number };

async function resolve(code: string, categoryId: string) {
  const province = await getProvinceByCode(code);
  if (!province) return { province };
  if (!isUuid(categoryId)) return { province, category: undefined };
  const categories = await getCategories(province.id);
  return { province, category: categories === null ? null : categories.find((c) => c.id === categoryId) };
}

/* The filters the address asks for, kept only where the category offers them. */
function filtersFor(category: Category, query: Query): Filters {
  return {
    specialty: category.capabilities.specialtyFilter ? tagId(query.specialty) : undefined,
    service: category.capabilities.serviceFilter ? tagId(query.service) : undefined,
  };
}

function hrefFor(base: string, filters: Filters, cursor?: string): string {
  const query = new URLSearchParams();
  if (filters.specialty) query.set("specialty", String(filters.specialty));
  if (filters.service) query.set("service", String(filters.service));
  if (cursor) query.set("cursor", cursor);
  const suffix = query.toString();
  return suffix ? `${base}?${suffix}` : base;
}

/* One row of chips: «all» first, then each choice; the current one is marked. */
function FilterChips({
  label,
  all,
  items,
  selected,
  hrefOf,
}: {
  label: string;
  all: string;
  items: TagRef[];
  selected?: number;
  hrefOf: (id?: number) => string;
}) {
  return (
    <nav aria-label={label}>
      <ul className="tabs">
        <li>
          <Link href={hrefOf(undefined)} aria-current={selected === undefined ? "page" : undefined}>
            {all}
          </Link>
        </li>
        {items.map((item) => (
          <li key={item.id}>
            <Link href={hrefOf(item.id)} aria-current={selected === item.id ? "page" : undefined}>
              {item.nameAr}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export async function generateMetadata({ params, searchParams }: Props): Promise<Metadata> {
  const { province: code, category: categoryId } = await params;
  const { cursor, specialty, service } = await searchParams;
  const { province, category } = await resolve(code, categoryId);
  if (!province || !category) return UNAVAILABLE_METADATA;
  return pageMetadata({
    title: `${category.nameAr} في ${province.nameAr}`,
    description: `قائمة ${category.nameAr} في ${province.nameAr} مع حالة الدوام الآن والتقييمات وأرقام التواصل.`,
    path: `/${province.code}/${category.id}`,
    noindex: Boolean(cursor || specialty || service),
  });
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const { province: code, category: categoryId } = await params;
  const query = await searchParams;
  const cursor = typeof query.cursor === "string" ? query.cursor : undefined;
  const { province, category } = await resolve(code, categoryId);
  if (province === undefined || category === undefined) notFound();
  if (province === null || category === null) return <div className="shell page"><Unavailable /></div>;

  const { specialtyFilter, serviceFilter } = category.capabilities;
  const filters = filtersFor(category, query);
  const [page, tags] = await Promise.all([
    getFacilities({
      provinceId: province.id,
      categoryId: category.id,
      cursor,
      specialtyId: filters.specialty,
      serviceTagId: filters.service,
    }),
    specialtyFilter || serviceFilter ? getCategoryTags(category.id) : undefined,
  ]);
  const base = `/${province.code}/${category.id}`;
  const specialties = specialtyFilter ? (tags?.specialties ?? []) : [];
  const services = serviceFilter ? (tags?.services ?? []) : [];
  const narrowed = Boolean(filters.specialty || filters.service);

  return (
    <div className="shell page">
      <Breadcrumbs items={[{ href: `/${province.code}`, label: province.nameAr }, { label: category.nameAr }]} />
      <h1>{category.nameAr} في {province.nameAr}</h1>
      {specialties.length > 0 || services.length > 0 ? (
        <div className="filters">
          {specialties.length > 0 ? (
            <FilterChips
              label="التصفية حسب التخصص"
              all="كل التخصصات"
              items={specialties}
              selected={filters.specialty}
              hrefOf={(id) => hrefFor(base, { ...filters, specialty: id })}
            />
          ) : null}
          {services.length > 0 ? (
            <FilterChips
              label="التصفية حسب الخدمة"
              all="كل الخدمات"
              items={services}
              selected={filters.service}
              hrefOf={(id) => hrefFor(base, { ...filters, service: id })}
            />
          ) : null}
        </div>
      ) : null}
      {page === null ? (
        <Unavailable />
      ) : page.items.length === 0 ? (
        narrowed ? (
          <Empty>
            لا توجد منشآت منشورة تطابق هذا الاختيار بعد. <Link href={base}>اعرض القائمة كاملة</Link>
          </Empty>
        ) : (
          <Empty>لا توجد منشآت منشورة في هذا التصنيف بعد.</Empty>
        )
      ) : (
        <>
          <FacilityList items={page.items} />
          <div className="actions more">
            {cursor ? <Link className="button button-alt" href={hrefFor(base, filters)}>العودة إلى البداية</Link> : null}
            {page.hasMore && page.nextCursor ? (
              <Link className="button" href={hrefFor(base, filters, page.nextCursor)} rel="next">
                عرض المزيد
              </Link>
            ) : null}
          </div>
        </>
      )}
    </div>
  );
}
