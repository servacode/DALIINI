import { type IconName, type IllustrationName, iconPaths, illustrationPaths, mirroredIcons } from "@servacode/design-tokens/icons";
import { term } from "@servacode/design-tokens/vocabulary";
import Link from "next/link";
import type { AvailabilityState } from "../lib/api";
import { publicConfig } from "../lib/config";

/*
 * Small presentational pieces shared by the directory pages. None of them carries state or an
 * effect, so they render on the server and add nothing to a page's script; the interactive parts
 * (the cards, the header, the slider, the forms) live in their own client components.
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
/*
 * Whether it is open, in a word and a sign.
 *
 * The four states each carry their own mark, because a coloured dot asks the reader to remember
 * which colour meant which. The ground is the state's own colour at full strength with the word
 * in white on it: a pale tint reads as decoration next to a photograph, and whether a pharmacy
 * is open is the one thing on the card that must be legible from across the room.
 */
const STATUS_ICON: Record<AvailabilityState, IconName> = {
  OPEN: "checkCircle",
  CLOSED: "lock",
  DUTY: "shield",
  TEMP_CLOSED: "alert",
};

export function StatusBadge({ state }: { state: AvailabilityState }) {
  const t = term("availability", state);
  return (
    <span className={`badge badge-iconic badge-solid badge-${t.tone}`}>
      <Icon name={STATUS_ICON[state]} size={14} />
      {t.ar}
    </span>
  );
}

export function Rating({ average, count }: { average: number | null; count: number }) {
  if (average == null || count === 0) return null;
  return <span className="rating muted" aria-label={`التقييم ${average.toFixed(1)} من 5`}>★ {average.toFixed(1)} ({count})</span>;
}

/* The card grid lives in its own client component: a card now opens over the list. */
export { FacilityCards as FacilityList } from "./facility-cards";

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
