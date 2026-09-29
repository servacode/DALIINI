import { type IconName, type IllustrationName, iconPaths, illustrationPaths, mirroredIcons } from "@servacode/design-tokens/icons";
import { term } from "@servacode/design-tokens/vocabulary";
import Link from "next/link";
import type { AvailabilityState, CompactFacility } from "../lib/api";
import { publicConfig } from "../lib/config";

/*
 * Small presentational pieces shared by the directory pages. All are server
 * components rendering plain HTML/CSS so pages ship no client JavaScript.
 */

/** The platform's shared icon, drawn from the design package; decorative, so hidden from readers. */
export function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      data-mirror={mirroredIcons.has(name) || undefined}
    >
      {iconPaths[name].map((d) => <path key={d} d={d} />)}
    </svg>
  );
}

/** A two-tone state illustration that follows the light or dark theme. */
export function Illustration({ name, size = 96 }: { name: IllustrationName; size?: number }) {
  const layers = illustrationPaths[name];
  return (
    <svg className="illustration" viewBox="0 0 120 120" width={size} height={size} fill="none" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
      {layers.soft.map((d) => <path key={d} d={d} className="il-soft" />)}
      {layers.line.map((d) => <path key={d} d={d} className="il-line" />)}
      {layers.accent.map((d) => <path key={d} d={d} className="il-accent" />)}
    </svg>
  );
}

/** Open, closed, on duty: the label and colour come from the shared vocabulary. */
export function StatusBadge({ state }: { state: AvailabilityState }) {
  const t = term("availability", state);
  return <span className={`badge badge-${t.tone}`}>{t.ar}</span>;
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
    <div className="card state" role="status">
      <Illustration name="offline" size={88} />
      <strong>تعذّر تحميل البيانات حالياً.</strong>
      <p>الخدمة غير متاحة مؤقتاً، يرجى المحاولة بعد قليل أو استخدام التطبيق.</p>
    </div>
  );
}

export function Empty({ children, illustration = "empty" }: { children: React.ReactNode; illustration?: IllustrationName }) {
  return (
    <div className="card state">
      <Illustration name={illustration} size={80} />
      <p>{children}</p>
    </div>
  );
}

export function Breadcrumbs({ items }: { items: { href?: string; label: string }[] }) {
  return (
    <nav aria-label="مسار التنقل" className="crumbs">
      <Link href="/">الرئيسية</Link>
      {items.map((item, i) => (
        <span key={item.label}>
          {" / "}
          {item.href ? (
            <Link href={item.href}>{item.label}</Link>
          ) : (
            <span aria-current={i === items.length - 1 ? "page" : undefined}>{item.label}</span>
          )}
        </span>
      ))}
    </nav>
  );
}

export function DownloadCta({
  title = "حمّل تطبيق دليني",
  body = "ابحث حسب موقعك، واحفظ المنشآت، وقيّم تجربتك من هاتفك.",
}: { title?: string; body?: string }) {
  const { playStoreUrl, appStoreUrl } = publicConfig;
  if (!playStoreUrl && !appStoreUrl) return null;
  return (
    <section className="card cta" aria-labelledby="download-title">
      <h2 id="download-title">{title}</h2>
      <p>{body}</p>
      <div className="actions">
        {playStoreUrl ? <a className="button" href={playStoreUrl} rel="noopener"><Icon name="download" />Google Play</a> : null}
        {appStoreUrl ? <a className="button button-alt" href={appStoreUrl} rel="noopener"><Icon name="download" />App Store</a> : null}
      </div>
    </section>
  );
}

/** A short page section: heading and body, used by the explanatory pages. */
export function Section({ id, title, children }: { id: string; title: string; children: React.ReactNode }) {
  return (
    <section aria-labelledby={id}>
      <h2 id={id}>{title}</h2>
      {children}
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
