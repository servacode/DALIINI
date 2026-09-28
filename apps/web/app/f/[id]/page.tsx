import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { cache } from "react";
import { Breadcrumbs, Icon, JsonLd, Rating, StatusBadge, Unavailable } from "../../../components/ui";
import { getFacility, type FacilityDetail, type HoursEntry } from "../../../lib/api";
import { absoluteUrl } from "../../../lib/config";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../../lib/seo";

/*
 * /f/[id] — the shareable facility page. The id is the facility UUID, which
 * never changes when a facility is renamed, so shared links stay valid.
 */
export const revalidate = 300;

type Props = { params: Promise<{ id: string }> };

/* Dedupe the detail fetch between generateMetadata and the page. */
const load = cache((id: string) => getFacility(id));

/* Backend weekdays follow Python's date.weekday(): 0 = Monday … 6 = Sunday. */
const WEEKDAYS_AR = ["الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"];
const WEEKDAYS_SCHEMA = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
/* Display order starting Saturday, the usual first day of the week locally. */
const DISPLAY_ORDER = [5, 6, 0, 1, 2, 3, 4];

const hhmm = (time: string) => time.slice(0, 5);

function describe(f: FacilityDetail): string {
  const place = [f.neighborhood?.nameAr, f.city?.nameAr].filter(Boolean).join("، ");
  return (
    f.descriptionAr?.slice(0, 160) ||
    `${f.category.nameAr}${place ? ` في ${place}` : ""}: أوقات الدوام، رقم الهاتف، والموقع على الخريطة.`
  );
}

/* WhatsApp wants international digits only; tolerate "+963 9…" style input. */
function waLink(raw: string | null | undefined): string | null {
  const digits = raw?.replace(/[^\d]/g, "");
  return digits && digits.length >= 8 ? `https://wa.me/${digits}` : null;
}

function schemaType(f: FacilityDetail): string {
  const name = `${f.category.nameEn ?? ""} ${f.category.nameAr}`.toLowerCase();
  if (name.includes("pharm") || name.includes("صيدل")) return "Pharmacy";
  if (/(clinic|doctor|hospital|lab|عياد|طبيب|مشفى|مستشفى|مخبر)/.test(name)) return "MedicalBusiness";
  return "LocalBusiness";
}

function structuredData(f: FacilityDetail) {
  return {
    "@context": "https://schema.org",
    "@type": schemaType(f),
    "@id": absoluteUrl(`/f/${f.id}`),
    url: absoluteUrl(`/f/${f.id}`),
    name: f.nameAr,
    alternateName: f.nameEn ?? undefined,
    description: f.descriptionAr ?? undefined,
    telephone: f.phone ?? undefined,
    address: {
      "@type": "PostalAddress",
      streetAddress: f.addressAr ?? undefined,
      addressLocality: f.city?.nameAr,
      addressCountry: "SY",
    },
    geo: f.location
      ? { "@type": "GeoCoordinates", latitude: f.location.latitude, longitude: f.location.longitude }
      : undefined,
    openingHoursSpecification: f.hours.map((h) => ({
      "@type": "OpeningHoursSpecification",
      dayOfWeek: `https://schema.org/${WEEKDAYS_SCHEMA[h.weekday]}`,
      opens: hhmm(h.opensAt),
      closes: hhmm(h.closesAt),
    })),
    aggregateRating:
      f.ratingAverage != null && f.ratingCount > 0
        ? { "@type": "AggregateRating", ratingValue: f.ratingAverage, ratingCount: f.ratingCount, bestRating: 5 }
        : undefined,
  };
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const f = await load(id);
  if (!f) return UNAVAILABLE_METADATA;
  return pageMetadata({ title: `${f.nameAr} — ${f.category.nameAr}`, description: describe(f), path: `/f/${f.id}` });
}

function HoursTable({ hours }: { hours: HoursEntry[] }) {
  const byDay = new Map<number, HoursEntry[]>();
  for (const h of [...hours].sort((a, b) => a.sequence - b.sequence)) {
    byDay.set(h.weekday, [...(byDay.get(h.weekday) ?? []), h]);
  }
  return (
    <table>
      <tbody>
        {DISPLAY_ORDER.map((day) => (
          <tr key={day}>
            <th scope="row">{WEEKDAYS_AR[day]}</th>
            <td className="ltr">
              {byDay.get(day)?.map((h) => `${hhmm(h.opensAt)}–${hhmm(h.closesAt)}`).join("، ") ?? "مغلق"}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default async function FacilityPage({ params }: Props) {
  const { id } = await params;
  const f = await load(id);
  if (f === undefined) notFound();
  if (f === null) return <div className="shell page"><Unavailable /></div>;

  const wa = waLink(f.whatsapp);
  const loc = f.location ? `${f.location.latitude},${f.location.longitude}` : null;
  const verified = f.lastVerifiedAt ? new Date(f.lastVerifiedAt) : null;

  return (
    <article className="shell page">
      <JsonLd data={structuredData(f)} />
      <Breadcrumbs items={[{ label: f.nameAr }]} />
      <div className="row">
        <h1>{f.nameAr}</h1>
        <StatusBadge state={f.availability.state} />
      </div>
      <div className="meta">
        <span>{f.category.nameAr}</span>
        {f.city ? <span>· {f.city.nameAr}</span> : null}
        <Rating average={f.ratingAverage} count={f.ratingCount} />
      </div>
      {f.availability.state === "DUTY" ? <p><strong>هذه الصيدلية مناوبة الآن.</strong></p> : null}
      {f.descriptionAr ? <p>{f.descriptionAr}</p> : null}

      <div className="actions more">
        {f.phone ? <a className="button" href={`tel:${f.phone.replace(/\s+/g, "")}`}><Icon name="phone" />اتصال</a> : null}
        {wa ? <a className="button button-alt" href={wa} rel="noopener"><Icon name="whatsapp" />واتساب</a> : null}
        {loc ? (
          <a className="button button-alt" href={`https://www.google.com/maps/dir/?api=1&destination=${loc}`} rel="noopener">
            <Icon name="directions" />
            الاتجاهات
          </a>
        ) : null}
      </div>

      <h2>المعلومات</h2>
      <dl className="card facts">
        {f.addressAr || f.neighborhood ? (
          <div><dt>العنوان</dt><dd>{[f.addressAr, f.neighborhood?.nameAr, f.city?.nameAr].filter(Boolean).join("، ")}</dd></div>
        ) : null}
        {f.phone ? <div><dt>الهاتف</dt><dd className="ltr"><a href={`tel:${f.phone.replace(/\s+/g, "")}`}>{f.phone}</a></dd></div> : null}
        {loc ? <div><dt>الموقع</dt><dd><a href={`geo:${loc}`}>فتح في تطبيق الخرائط</a></dd></div> : null}
        {verified && !Number.isNaN(verified.getTime()) ? (
          <div><dt>آخر تحقق</dt><dd>{verified.toLocaleDateString("ar-SY", { dateStyle: "long" })}</dd></div>
        ) : null}
      </dl>

      {f.hours.length > 0 ? (
        <>
          <h2>أوقات الدوام</h2>
          <div className="card"><HoursTable hours={f.hours} /></div>
        </>
      ) : null}
    </article>
  );
}
