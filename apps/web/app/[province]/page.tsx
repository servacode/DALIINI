import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Breadcrumbs, Empty, Unavailable } from "../../components/ui";
import { getCategories, getProvinceByCode } from "../../lib/api";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../lib/seo";

/*
 * /[province] — categories active in one province, addressed by the
 * province's stable `code`. Rendered on demand and cached (ISR); nothing is
 * prerendered at build time so `next build` never needs the API.
 */
export const revalidate = 300;

type Props = { params: Promise<{ province: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { province: code } = await params;
  const province = await getProvinceByCode(code);
  if (!province) return UNAVAILABLE_METADATA;
  return pageMetadata({
    title: `دليل ${province.nameAr}`,
    description: `تصفّح الصيدليات والعيادات والمنشآت والخدمات في ${province.nameAr} مع أوقات الدوام وطرق التواصل.`,
    path: `/${province.code}`,
  });
}

export default async function ProvincePage({ params }: Props) {
  const { province: code } = await params;
  const province = await getProvinceByCode(code);
  if (province === undefined) notFound();
  if (province === null) return <div className="shell page"><Unavailable /></div>;

  const categories = await getCategories(province.id);

  return (
    <div className="shell page">
      <Breadcrumbs items={[{ label: province.nameAr }]} />
      <h1>دليل {province.nameAr}</h1>
      <p>اختر تصنيفاً لعرض المنشآت المتاحة.</p>
      {categories === null ? (
        <Unavailable />
      ) : categories.length === 0 ? (
        <Empty>لا توجد تصنيفات مفعّلة في هذه المحافظة بعد.</Empty>
      ) : (
        <ul className="grid">
          {categories.map((c) => (
            <li key={c.id}>
              <Link className="card tile" href={`/${province.code}/${c.id}`}>
                {c.nameAr}
                <small>{c.group.nameAr}</small>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
