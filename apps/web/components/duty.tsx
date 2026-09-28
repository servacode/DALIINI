import Link from "next/link";
import { getDutyByProvince, type DutyWhen } from "../lib/api";
import { Empty, FacilityList, Icon, Unavailable } from "./ui";

/*
 * The duty pages: /duty (a shift is running now) and /duty/today (on today's
 * roster). The public API answers only these two questions; later dates need a
 * roster endpoint first.
 */
const TABS: { when: DutyWhen; href: string; label: string }[] = [
  { when: "now", href: "/duty", label: "الآن" },
  { when: "today", href: "/duty/today", label: "اليوم" },
];

export const DUTY_TITLES: Record<DutyWhen, string> = {
  now: "الصيدليات المناوبة الآن",
  today: "الصيدليات المناوبة اليوم",
};

export async function DutyView({ when }: { when: DutyWhen }) {
  const groups = await getDutyByProvince(when);
  return (
    <div className="shell page">
      <h1>{DUTY_TITLES[when]}</h1>
      <nav aria-label="وقت المناوبة">
        <ul className="tabs">
          {TABS.map((t) => (
            <li key={t.when}>
              <Link href={t.href} aria-current={t.when === when ? "page" : undefined}>
                {t.when === "now" ? <Icon name="clock" size={16} /> : <Icon name="calendar" size={16} />}
                {t.label}
              </Link>
            </li>
          ))}
        </ul>
      </nav>
      <p className="muted">
        {when === "now"
          ? "صيدليات مناوبتها جارية في هذه اللحظة. تُحدَّث القائمة كل بضع دقائق، واتصل قبل التوجّه."
          : "كل الصيدليات المسجّلة في مناوبات اليوم، ومنها ما لم تبدأ مناوبته بعد. اتصل قبل التوجّه."}
      </p>
      {groups === null ? (
        <Unavailable />
      ) : groups.length === 0 ? (
        <Empty>لا توجد محافظات مفعّلة بعد.</Empty>
      ) : (
        groups.map((g) => (
          <section key={g.province.id} aria-labelledby={`p-${g.province.id}`}>
            <h2 id={`p-${g.province.id}`}>{g.province.nameAr}</h2>
            {g.items.length === 0 ? (
              <Empty illustration="noResults">
                {when === "now" ? "لا توجد صيدليات مناوبة معلنة حالياً." : "لا توجد مناوبات معلنة لهذا اليوم."}
              </Empty>
            ) : (
              <FacilityList items={g.items} />
            )}
          </section>
        ))
      )}
      <p className="more muted">
        من أين تأتي المناوبات؟ <Link href="/how-we-verify#duty">كيف نتحقق من المعلومات</Link>
      </p>
    </div>
  );
}
