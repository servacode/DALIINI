import type { Metadata } from "next";
import { directionsLink, localPhone, telLink, whatsAppFor } from "../../../../lib/links";
import { notFound, permanentRedirect } from "next/navigation";
import { cache } from "react";
import Link from "next/link";
import { ShareLinks } from "../../../../components/share";
import { AndroidOnly } from "../../../../components/android-only";
import { Breadcrumbs, Icon, JsonLd, Rating, StatusBadge, Unavailable } from "../../../../components/ui";
import { getCategories, getFacility, getProvinces, type FacilityDetail, type HoursEntry } from "../../../../lib/api";
import { absoluteUrl, appOpenUrl } from "../../../../lib/config";
import { WEEKDAYS_AR, WEEKDAY_DISPLAY_ORDER, damascusWeekday, spokenDate } from "../../../../lib/dates";
import { categoryPath, decodedSegment, facilityPath } from "../../../../lib/paths";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../../../lib/seo";

/*
 * /f/[id]/[slug] — the shareable facility page. The id is the facility UUID, which never changes
 * when a facility is renamed, so shared links stay valid; the slug after it is the name in words.
 * Any other words, or none (every link shared before slugs existed), redirect permanently to the
 * current address, so there is one canonical page per facility (DECISION-069).
 */
export const revalidate = 300;

type Props = { params: Promise<{ id: string; slug?: string[] }> };

/* Dedupe the detail fetch between generateMetadata and the page. */
const load = cache((id: string) => getFacility(id));

/* Schema.org counts the week in English, in the backend's own order (Monday first). */
const WEEKDAYS_SCHEMA = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];

const hhmm = (time: string) => time.slice(0, 5);

function describe(f: FacilityDetail): string {
  const place = [f.neighborhood?.nameAr, f.city?.nameAr].filter(Boolean).join("، ");
  return (
    f.descriptionAr?.slice(0, 160) ||
    `${f.category.nameAr}${place ? ` في ${place}` : ""}: أوقات الدوام، رقم الهاتف، والموقع على الخريطة.`
  );
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
    "@id": absoluteUrl(facilityPath(f)),
    url: absoluteUrl(facilityPath(f)),
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
  return pageMetadata({ title: `${f.nameAr} — ${f.category.nameAr}`, description: describe(f), path: facilityPath(f) });
}

function HoursTable({ hours, today }: { hours: HoursEntry[]; today: number }) {
  const byDay = new Map<number, HoursEntry[]>();
  for (const h of [...hours].sort((a, b) => a.sequence - b.sequence)) {
    byDay.set(h.weekday, [...(byDay.get(h.weekday) ?? []), h]);
  }
  return (
    <table className="hours-table">
      <tbody>
        {WEEKDAY_DISPLAY_ORDER.map((day) => (
          <tr key={day} aria-current={day === today ? "date" : undefined}>
            <th scope="row">
              {WEEKDAYS_AR[day]}
              {day === today ? <span className="today-mark">اليوم</span> : null}
            </th>
            <td className={byDay.has(day) ? "ltr" : "muted"}>
              {byDay.get(day)?.map((h) => `${hhmm(h.opensAt)}–${hhmm(h.closesAt)}`).join("، ") ?? "مغلق"}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/*
 * The detail payload names the category but not the province. With a single
 * active province (the launch set-up) the category listing is unambiguous, so
 * the breadcrumb links to it; otherwise the category is shown as plain text.
 */
async function crumbs(f: FacilityDetail) {
  const provinces = await getProvinces();
  const only = provinces?.length === 1 ? provinces[0] : null;
  if (!only) return [{ label: f.category.nameAr }, { label: f.nameAr }];
  const category = (await getCategories(only.id))?.find((c) => c.id === f.category.id);
  return [
    { href: `/${only.code}`, label: only.nameAr },
    { href: categoryPath(only.code, category ?? f.category), label: f.category.nameAr },
    { label: f.nameAr },
  ];
}

/*
 * «تم التحقق قبل ٣ أيام · آخر تأكيد للمعلومات أمس · كيف نتحقق؟»
 *
 * «تم التحقق» is when staff last approved the details (lastVerifiedAt).
 * «آخر تأكيد للمعلومات» is infoConfirmedAt, the later of that approval and the
 * owner's own confirmation that the hours are still right; it is shown when it
 * says something the first does not. «آخر تحديث» stands in only when neither
 * date exists.
 */
function TrustLine({ f }: { f: FacilityDetail }) {
  const verified = spokenDate(f.lastVerifiedAt);
  const confirmedAt = spokenDate(f.infoConfirmedAt);
  const confirmed =
    confirmedAt && (!verified || (Date.parse(confirmedAt.iso) > Date.parse(verified.iso) && confirmedAt.text !== verified.text))
      ? confirmedAt
      : null;
  const updated = !verified && !confirmed ? spokenDate(f.updatedAt) : null;
  return (
    <p className="trust">
      {verified ? (
        <span className="with-icon">
          <Icon name="verified" size={16} />
          <span>تم التحقق <time dateTime={verified.iso} title={verified.full}>{verified.text}</time></span>
        </span>
      ) : null}
      {confirmed ? (
        <span className="with-icon">
          <Icon name="refresh" size={16} />
          <span>آخر تأكيد للمعلومات <time dateTime={confirmed.iso} title={confirmed.full}>{confirmed.text}</time></span>
        </span>
      ) : null}
      {updated ? (
        <span>آخر تحديث <time dateTime={updated.iso} title={updated.full}>{updated.text}</time></span>
      ) : null}
      <Link href="/how-we-verify#last-verified">كيف نتحقق؟</Link>
    </p>
  );
}

export default async function FacilityPage({ params }: Props) {
  const { id, slug } = await params;
  const f = await load(id);
  if (f === undefined) notFound();
  if (f === null) return <div className="shell page"><Unavailable /></div>;
  if ((slug ?? []).map(decodedSegment).join("/") !== (f.slug ?? "")) permanentRedirect(facilityPath(f));

  const tel = telLink(f.phone);
  const wa = whatsAppFor(f.whatsapp, f.phone);
  const directions = directionsLink(f.location);
  const shown = localPhone(f.phone);
  const loc = f.location ? `${f.location.latitude},${f.location.longitude}` : null;
  const openInApp = appOpenUrl(f.id);
  const url = absoluteUrl(facilityPath(f));
  const address = [f.addressAr, f.neighborhood?.nameAr, f.city?.nameAr].filter(Boolean).join("، ");
  const images = f.images ?? [];

  return (
    <article className="shell page facility-page">
      <JsonLd data={structuredData(f)} />
      <Breadcrumbs items={await crumbs(f)} />

      {/* The shopfront first, when there is one: it is how a passer-by recognises the door. */}
      {images.length > 0 ? (
        <div className="facility-gallery" data-count={Math.min(images.length, 3)}>
          {images.slice(0, 6).map((image, index) => (
            /* eslint-disable-next-line @next/next/no-img-element -- remote media, no loader */
            <img
              key={image.id}
              src={image.url}
              alt={index === 0 ? `واجهة ${f.nameAr}` : `صورة ${index + 1} من ${f.nameAr}`}
              loading={index === 0 ? "eager" : "lazy"}
            />
          ))}
        </div>
      ) : null}

      <header className="facility-header">
        <h1>{f.nameAr}</h1>
        <div className="facility-sub">
          <StatusBadge state={f.availability.state} />
          <span>{f.category.nameAr}</span>
          {f.neighborhood || f.city ? <span>{(f.neighborhood ?? f.city)?.nameAr}</span> : null}
          <Rating average={f.ratingAverage} count={f.ratingCount} />
        </div>
        <TrustLine f={f} />
        {/* On a phone the panel with this button is replaced by the dock; the offer stays here. */}
        {openInApp ? (
          <AndroidOnly>
            <a className="button button-alt button-sm facility-open-app" href={openInApp}>
              <Icon name="externalLink" size={16} />
              افتح في التطبيق
            </a>
          </AndroidOnly>
        ) : null}
      </header>

      <div className="facility-layout">
        <div className="facility-content">
          {f.availability.state === "DUTY" ? (
            <p className="notice notice-duty">
              <Icon name="shield" size={20} />
              <strong>هذه الصيدلية مناوبة الآن.</strong>
            </p>
          ) : null}
          {f.descriptionAr ? <p className="facility-description">{f.descriptionAr}</p> : null}

          <section aria-labelledby="facts-title">
            <h2 id="facts-title">المعلومات</h2>
            <dl className="card facts">
              {address ? <div><dt>العنوان</dt><dd>{address}</dd></div> : null}
              {tel && shown ? (
                <div><dt>الهاتف</dt><dd className="ltr"><a href={tel}>{shown}</a></dd></div>
              ) : null}
              {loc ? (
                <div>
                  <dt>الموقع</dt>
                  <dd>
                    <a href={`geo:${loc}`}>افتح في تطبيق الخرائط</a>
                    {directions ? <> · <a href={directions} target="_blank" rel="noopener noreferrer">الطريق إليها</a></> : null}
                  </dd>
                </div>
              ) : null}
              {f.specialties.length > 0 ? (
                <div><dt>التخصصات</dt><dd>{f.specialties.map((s) => s.nameAr).join("، ")}</dd></div>
              ) : null}
              {f.services.length > 0 ? (
                <div><dt>الخدمات</dt><dd>{f.services.map((s) => s.nameAr).join("، ")}</dd></div>
              ) : null}
            </dl>
          </section>

          {f.hours.length > 0 ? (
            <section aria-labelledby="hours-title">
              <h2 id="hours-title">أوقات الدوام</h2>
              <div className="card"><HoursTable hours={f.hours} today={damascusWeekday()} /></div>
            </section>
          ) : null}

          <section aria-labelledby="report-title">
            <h2 id="report-title" className="with-icon"><Icon name="flag" size={20} />وجدت معلومة خاطئة؟</h2>
            <div className="card note">
              <p>أرسل لنا التصحيح وسيراجعه فريقنا، أو افتح هذه المنشأة في تطبيق دليني واضغط «الإبلاغ عن مشكلة».</p>
              <Link className="button button-alt button-sm" href={`/contact?kind=correction&facility=${f.id}`}>
                <Icon name="edit" size={16} />
                تصحيح معلومة
              </Link>
            </div>
          </section>
        </div>

        {/*
          * The ways to reach it, beside the page on a desk and pinned to the bottom of the screen
          * on a phone, so the call is one tap from anywhere on a long page.
          */}
        <aside className="facility-aside" aria-label="التواصل">
          <div className="card facility-contact">
            {tel ? (
              <a className="button facility-call" href={tel}>
                <Icon name="phone" />
                <span className="ltr">{shown}</span>
              </a>
            ) : null}
            <div className="facility-contact-row">
              {wa ? (
                <a className="button button-alt" href={wa} target="_blank" rel="noopener noreferrer">
                  <Icon name="whatsapp" />
                  واتساب
                </a>
              ) : null}
              {directions ? (
                <a className="button button-alt" href={directions} target="_blank" rel="noopener noreferrer">
                  <Icon name="directions" />
                  الطريق
                </a>
              ) : null}
            </div>
            {openInApp ? (
              <AndroidOnly>
                <a className="button button-alt" href={openInApp}>
                  <Icon name="externalLink" />
                  افتح في التطبيق
                </a>
              </AndroidOnly>
            ) : null}
          </div>
          <ShareLinks title={f.nameAr} url={url} />
        </aside>
      </div>

      {tel || wa || directions ? (
        <nav className="facility-dock" aria-label="تواصل سريع">
          {tel ? (
            <a className="button" href={tel}>
              <Icon name="phone" />
              اتصال
            </a>
          ) : null}
          {wa ? (
            <a className="button button-whatsapp" href={wa} target="_blank" rel="noopener noreferrer">
              <Icon name="whatsapp" />
              واتساب
            </a>
          ) : null}
          {directions ? (
            <a className="button button-alt" href={directions} target="_blank" rel="noopener noreferrer">
              <Icon name="directions" />
              الطريق
            </a>
          ) : null}
        </nav>
      ) : null}
    </article>
  );
}
