import Link from "next/link";
import type { AvailabilityState, CompactFacility } from "../lib/api";
import { publicConfig } from "../lib/config";

/*
 * Small presentational pieces shared by the directory pages. All are server
 * components rendering plain HTML/CSS so pages ship no client JavaScript.
 */

const STATE_LABEL: Record<AvailabilityState, string> = {
  OPEN: "مفتوح الآن",
  CLOSED: "مغلق الآن",
  DUTY: "مناوب الآن",
  TEMP_CLOSED: "مغلق مؤقتاً",
};

export function StatusBadge({ state }: { state: AvailabilityState }) {
  return <span className={`badge badge-${state.toLowerCase()}`}>{STATE_LABEL[state] ?? state}</span>;
}

export function Rating({ average, count }: { average: number | null; count: number }) {
  if (average == null || count === 0) return null;
  return <span className="muted" aria-label={`التقييم ${average.toFixed(1)} من 5`}>★ {average.toFixed(1)} ({count})</span>;
}

export function FacilityList({ items }: { items: CompactFacility[] }) {
  return (
    <ul className="list">
      {items.map((f) => (
        <li key={f.id} className="card row">
          <div>
            <Link href={`/f/${f.id}`} className="title-link">{f.nameAr}</Link>
            <div className="meta">
              <span>{f.category.nameAr}</span>
              {f.city ? <span>· {f.city.nameAr}</span> : null}
              <Rating average={f.ratingAverage} count={f.ratingCount} />
            </div>
          </div>
          <StatusBadge state={f.availability.state} />
        </li>
      ))}
    </ul>
  );
}

export function Unavailable() {
  return (
    <div className="card notice" role="status">
      <strong>تعذّر تحميل البيانات حالياً.</strong>
      <p>الخدمة غير متاحة مؤقتاً، يرجى المحاولة بعد قليل أو استخدام التطبيق.</p>
    </div>
  );
}

export function Empty({ children }: { children: React.ReactNode }) {
  return <div className="card notice"><p>{children}</p></div>;
}

export function Breadcrumbs({ items }: { items: { href?: string; label: string }[] }) {
  return (
    <nav aria-label="مسار التنقل" className="crumbs">
      <Link href="/">الرئيسية</Link>
      {items.map((item) => (
        <span key={item.label}>
          {" / "}
          {item.href ? <Link href={item.href}>{item.label}</Link> : <span aria-current="page">{item.label}</span>}
        </span>
      ))}
    </nav>
  );
}

export function DownloadCta() {
  const { playStoreUrl, appStoreUrl } = publicConfig;
  if (!playStoreUrl && !appStoreUrl) return null;
  return (
    <section className="card cta" aria-labelledby="download-title">
      <h2 id="download-title">حمّل تطبيق دليني</h2>
      <p>ابحث حسب موقعك، واحفظ المنشآت، وقيّم تجربتك من هاتفك.</p>
      <div className="actions">
        {playStoreUrl ? <a className="button" href={playStoreUrl} rel="noopener">Google Play</a> : null}
        {appStoreUrl ? <a className="button button-alt" href={appStoreUrl} rel="noopener">App Store</a> : null}
      </div>
    </section>
  );
}

/* Serialises structured data; `<` is escaped so content cannot close the tag. */
export function JsonLd({ data }: { data: object }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }}
    />
  );
}
