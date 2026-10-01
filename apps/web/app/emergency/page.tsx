import type { IconName } from "@servacode/design-tokens/icons";
import type { Metadata } from "next";
import Link from "next/link";
import { Empty, Icon, Unavailable } from "../../components/ui";
import { getEmergencyNumbers, getProvinces, type EmergencyKind, type EmergencyNumber } from "../../lib/api";
import { pageMetadata } from "../../lib/seo";

/*
 * /emergency — numbers to call in an emergency: the national ones, then those
 * of the chosen province (?province=<code>, a plain GET form; with one active
 * province it is chosen already). Each number is a tel: link. The numbers come
 * from the console (emergency-numbers/); the page never invents one, so while
 * the API is unreachable it says so instead.
 */

type Props = { searchParams: Promise<{ province?: string | string[] }> };

const one = (v: string | string[] | undefined) => (typeof v === "string" ? v : Array.isArray(v) ? v[0] : undefined);

const KIND_ICON: Record<EmergencyKind, IconName> = {
  AMBULANCE: "emergency",
  FIRE: "alert",
  POLICE: "shield",
  HOSPITAL: "building",
  OTHER: "phone",
};

const DESCRIPTION = "أرقام الإسعاف والإطفاء والشرطة وأرقام الطوارئ في محافظتك، للاتصال بلمسة واحدة.";

async function resolve(code: string | undefined) {
  const provinces = await getProvinces();
  const chosen = provinces?.length === 1 ? provinces[0] : provinces?.find((p) => p.code.toLowerCase() === code);
  return { provinces, chosen, choosable: (provinces?.length ?? 0) > 1 };
}

export async function generateMetadata({ searchParams }: Props): Promise<Metadata> {
  const code = one((await searchParams).province)?.trim().toLowerCase() || undefined;
  const { chosen, choosable } = await resolve(code);
  /* With several provinces each one's numbers are a page of their own; any other query is not. */
  const own = Boolean(chosen && choosable && code);
  return pageMetadata({
    title: own && chosen ? `أرقام الطوارئ في ${chosen.nameAr}` : "أرقام الطوارئ",
    description: DESCRIPTION,
    path: own && chosen ? `/emergency?province=${encodeURIComponent(chosen.code)}` : "/emergency",
    noindex: Boolean(code) && !own,
  });
}

function NumberList({ items }: { items: EmergencyNumber[] }) {
  return (
    <ul className="list numbers">
      {items.map((n) => (
        <li key={n.id} className="card row number">
          <span className="number-label">
            <span className="feature-icon"><Icon name={KIND_ICON[n.kind] ?? "phone"} size={22} /></span>
            <strong>{n.labelAr}</strong>
          </span>
          <a className="button call" href={`tel:${n.phone.replace(/[^\d+]/g, "")}`}>
            <Icon name="phone" />
            <span className="sr-only">اتصل ب{n.labelAr} على الرقم </span>
            <span className="ltr">{n.phone}</span>
          </a>
        </li>
      ))}
    </ul>
  );
}

export default async function EmergencyPage({ searchParams }: Props) {
  const code = one((await searchParams).province)?.trim().toLowerCase() || undefined;
  const { provinces, chosen, choosable } = await resolve(code);
  const numbers = await getEmergencyNumbers(chosen?.id);
  const national = numbers?.filter((n) => n.scope === "NATIONAL") ?? [];
  const local = chosen ? (numbers?.filter((n) => n.scope === "PROVINCE" && n.provinceId === chosen.id) ?? []) : [];

  return (
    <div className="shell page">
      <h1 className="with-icon"><Icon name="emergency" size={30} />أرقام الطوارئ</h1>
      <p className="page-intro">اضغط على الرقم للاتصال مباشرة. إن كانت حياة أحد في خطر، فاتصل بالإسعاف أولاً.</p>
      <div className="card note">
        <p>
          تحقق من الأرقام مع الجهات الرسمية. وجدت رقماً خاطئاً؟{" "}
          <Link href="/contact?kind=correction">أبلغنا لنصححه</Link>.
        </p>
      </div>

      {choosable && provinces ? (
        <form action="/emergency" method="get" className="card search-form more">
          <div className="field">
            <label htmlFor="emergency-province">المحافظة</label>
            <select id="emergency-province" name="province" defaultValue={chosen?.code ?? ""}>
              <option value="">اختر محافظتك</option>
              {provinces.map((p) => <option key={p.id} value={p.code}>{p.nameAr}</option>)}
            </select>
          </div>
          <button type="submit" className="button">اعرض أرقام المحافظة</button>
        </form>
      ) : null}

      {numbers === null ? (
        <div className="more"><Unavailable /></div>
      ) : (
        <>
          <section aria-labelledby="national-title">
            <h2 id="national-title">أرقام وطنية</h2>
            {national.length > 0 ? <NumberList items={national} /> : <Empty>لا توجد أرقام وطنية منشورة بعد.</Empty>}
          </section>
          {chosen ? (
            <section aria-labelledby="province-title">
              <h2 id="province-title">أرقام خاصة ب{chosen.nameAr}</h2>
              {local.length > 0 ? (
                <NumberList items={local} />
              ) : (
                <Empty>لم تُنشر أرقام خاصة ب{chosen.nameAr} بعد. استعمل الأرقام الوطنية.</Empty>
              )}
            </section>
          ) : choosable ? (
            <p className="muted more">اختر محافظتك لعرض أرقامها الخاصة أيضاً.</p>
          ) : null}
        </>
      )}
    </div>
  );
}
