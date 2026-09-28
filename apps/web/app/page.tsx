import type { Metadata } from "next";
import Link from "next/link";
import { SearchBox } from "../components/search-form";
import { DownloadCta, FacilityList, JsonLd, Unavailable } from "../components/ui";
import { getDutyByProvince, getProvinces } from "../lib/api";
import { SITE_NAME, absoluteUrl, publicConfig } from "../lib/config";

/* ISR: the landing page is rebuilt at most every five minutes. */
export const revalidate = 300;

export const metadata: Metadata = { alternates: { canonical: "/" } };

export default async function HomePage() {
  const [provinces, duty] = await Promise.all([getProvinces(), getDutyByProvince()]);
  const dutyGroups = (duty ?? []).filter((g) => g.items.length > 0);

  return (
    <div className="shell">
      <JsonLd
        data={{
          "@context": "https://schema.org",
          "@type": "Organization",
          name: SITE_NAME,
          url: absoluteUrl("/"),
          logo: absoluteUrl("/icon.png"),
          email: publicConfig.supportEmail.includes("@") ? publicConfig.supportEmail : undefined,
        }}
      />
      <section className="hero">
        <span className="eyebrow">دليني</span>
        <h1>اعثر على الصيدليات والعيادات والخدمات القريبة منك بسهولة.</h1>
        <p>دليل محلي يعرض أوقات الدوام، والصيدليات المناوبة، وأرقام التواصل، ومواقع المنشآت على الخريطة — بمعلومات تُراجَع باستمرار.</p>
        <SearchBox id="hero-q" variant="hero" />
        <div className="actions">
          <Link className="button" href="/duty">الصيدليات المناوبة الآن</Link>
          <Link className="button button-alt" href="/owners">أضف منشأتك</Link>
        </div>
      </section>

      <section aria-labelledby="provinces-title">
        <h2 id="provinces-title">المحافظات المتاحة</h2>
        {provinces === null ? (
          <Unavailable />
        ) : provinces.length === 0 ? (
          <p>لم تُفعَّل أي محافظة بعد.</p>
        ) : (
          <ul className="grid">
            {provinces.map((p) => (
              <li key={p.id}>
                <Link className="card tile" href={`/${p.code}`}>
                  {p.nameAr}
                  <small>تصفّح التصنيفات</small>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      {dutyGroups.length > 0 ? (
        <section aria-labelledby="duty-title">
          <h2 id="duty-title">صيدليات مناوبة الآن</h2>
          {dutyGroups.map((g) => (
            <div key={g.province.id}>
              {dutyGroups.length > 1 ? <h3>{g.province.nameAr}</h3> : null}
              <FacilityList items={g.items.slice(0, 6)} />
            </div>
          ))}
          <p className="more"><Link href="/duty">عرض كل المناوبات</Link></p>
        </section>
      ) : null}

      <DownloadCta />
    </div>
  );
}
