import { Empty, FacilityList, Unavailable } from "../../components/ui";
import { getDutyByProvince } from "../../lib/api";
import { pageMetadata } from "../../lib/seo";

/*
 * /duty — pharmacies on duty right now, grouped by active province. Duty
 * changes over the day, so this page uses the same 5-minute ISR window as
 * the rest of the directory rather than being fully static.
 */
export const revalidate = 300;

export const metadata = pageMetadata({
  title: "الصيدليات المناوبة الآن",
  description: "قائمة الصيدليات المناوبة الآن في كل محافظة مع أرقام التواصل والموقع.",
  path: "/duty",
});

export default async function DutyPage() {
  const groups = await getDutyByProvince();
  return (
    <div className="shell page">
      <h1>الصيدليات المناوبة الآن</h1>
      <p className="muted">تُحدَّث القائمة كل بضع دقائق. يُنصح بالاتصال قبل التوجّه.</p>
      {groups === null ? (
        <Unavailable />
      ) : groups.length === 0 ? (
        <Empty>لا توجد محافظات مفعّلة بعد.</Empty>
      ) : (
        groups.map((g) => (
          <section key={g.province.id} aria-labelledby={`p-${g.province.id}`}>
            <h2 id={`p-${g.province.id}`}>{g.province.nameAr}</h2>
            {g.items.length === 0 ? <Empty illustration="noResults">لا توجد صيدليات مناوبة معلنة حالياً.</Empty> : <FacilityList items={g.items} />}
          </section>
        ))
      )}
    </div>
  );
}
