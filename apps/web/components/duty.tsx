import Link from "next/link";
import { getDutyByProvince, getDutyRosterByProvince, type DutyDay, type DutyShift } from "../lib/api";
import { dayLabel, shiftSpan } from "../lib/dates";
import { facilityPath } from "../lib/paths";
import { Empty, FacilityList, Icon, Rating, StatusBadge, Unavailable } from "./ui";

/*
 * The duty pages. «الآن» (/duty) lists pharmacies whose shift is running at this
 * moment (public/facilities/?dutyNow). «اليوم», «غداً» and «الأسبوع» read the
 * roster by date (public/duty/, one call per province for the seven days from
 * the API's own today), with each pharmacy's shift times.
 */
export type DutyWhen = "now" | "today" | "tomorrow" | "week";
type RosterWhen = Exclude<DutyWhen, "now">;

const TABS: { when: DutyWhen; href: string; label: string }[] = [
  { when: "now", href: "/duty", label: "الآن" },
  { when: "today", href: "/duty/today", label: "اليوم" },
  { when: "tomorrow", href: "/duty/tomorrow", label: "غداً" },
  { when: "week", href: "/duty/week", label: "الأسبوع" },
];

export const DUTY_TITLES: Record<DutyWhen, string> = {
  now: "الصيدليات المناوبة الآن",
  today: "الصيدليات المناوبة اليوم",
  tomorrow: "الصيدليات المناوبة غداً",
  week: "مناوبات الأسبوع",
};

const INTRO: Record<DutyWhen, string> = {
  now: "صيدليات مناوبتها جارية في هذه اللحظة. تُحدَّث القائمة كل بضع دقائق، واتصل قبل التوجّه.",
  today: "كل الصيدليات المسجّلة في مناوبات اليوم مع أوقاتها، ومنها ما لم تبدأ مناوبته بعد. اتصل قبل التوجّه.",
  tomorrow: "الصيدليات المسجّلة في مناوبات الغد مع أوقاتها. قد تتغير المناوبات، فاتصل قبل التوجّه.",
  week: "مناوبات اليوم والأيام الستة التالية، يوماً بيوم. قد تتغير المناوبات، فاتصل قبل التوجّه.",
};

const NO_DUTY_DAY = "لا توجد مناوبات معلنة لهذا اليوم.";

/* Which of the seven roster days a tab shows. */
const DAY_RANGE: Record<RosterWhen, [number, number]> = { today: [0, 1], tomorrow: [1, 2], week: [0, 7] };

function DutyTabs({ when }: { when: DutyWhen }) {
  return (
    <nav aria-label="وقت المناوبة">
      <ul className="tabs">
        {TABS.map((t) => (
          <li key={t.when}>
            <Link href={t.href} aria-current={t.when === when ? "page" : undefined}>
              <Icon name={t.when === "now" ? "clock" : "calendar"} size={16} />
              {t.label}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export async function DutyView({ when }: { when: DutyWhen }) {
  return (
    <div className="shell page">
      {when === "now" ? <NowRoster /> : <DateRoster when={when} />}
      <p className="more muted">
        من أين تأتي المناوبات؟ <Link href="/how-we-verify#duty">كيف نتحقق من المعلومات</Link>
      </p>
    </div>
  );
}

function Heading({ when, date }: { when: DutyWhen; date?: string | null }) {
  return (
    <>
      <h1>{DUTY_TITLES[when]}</h1>
      {date ? <p className="day-date">{date}</p> : null}
      <DutyTabs when={when} />
      <p className="muted">{INTRO[when]}</p>
    </>
  );
}

async function NowRoster() {
  const groups = await getDutyByProvince();
  return (
    <>
      <Heading when="now" />
      {groups === null ? (
        <Unavailable />
      ) : groups.length === 0 ? (
        <Empty>لا توجد محافظات مفعّلة بعد.</Empty>
      ) : (
        groups.map((g) => (
          <section key={g.province.id} aria-labelledby={`p-${g.province.id}`}>
            <h2 id={`p-${g.province.id}`}>{g.province.nameAr}</h2>
            {g.items.length === 0 ? (
              <Empty illustration="noResults">لا توجد صيدليات مناوبة معلنة حالياً.</Empty>
            ) : (
              <FacilityList items={g.items} />
            )}
          </section>
        ))
      )}
    </>
  );
}

async function DateRoster({ when }: { when: RosterWhen }) {
  const groups = await getDutyRosterByProvince();
  const [from, to] = DAY_RANGE[when];
  /* Every province shares the API's calendar, so the first loaded roster dates the page. */
  const first = groups?.find((g) => g.days !== null)?.days?.[from];
  return (
    <>
      <Heading when={when} date={when === "week" ? null : first ? dayLabel(first.date) : null} />
      {groups === null ? (
        <Unavailable />
      ) : groups.length === 0 ? (
        <Empty>لا توجد محافظات مفعّلة بعد.</Empty>
      ) : (
        groups.map((g) => {
          const days = g.days?.slice(from, to) ?? null;
          return (
            <section key={g.province.id} aria-labelledby={`p-${g.province.id}`}>
              <h2 id={`p-${g.province.id}`}>{g.province.nameAr}</h2>
              {days === null ? (
                <Unavailable />
              ) : when === "week" ? (
                days.some((day) => day.items.length > 0) ? (
                  days.map((day, i) => <WeekDay key={day.date} day={day} index={i} provinceId={g.province.id} />)
                ) : (
                  <Empty illustration="noResults">لا توجد مناوبات معلنة للأيام السبعة القادمة.</Empty>
                )
              ) : days[0] && days[0].items.length > 0 ? (
                <RosterList day={days[0]} showState={when === "today"} />
              ) : (
                <Empty illustration="noResults">{NO_DUTY_DAY}</Empty>
              )}
            </section>
          );
        })
      )}
    </>
  );
}

/* One day of the week view: «اليوم · الثلاثاء، ٢٩ أيلول», then its pharmacies. */
function WeekDay({ day, index, provinceId }: { day: DutyDay; index: number; provinceId: string }) {
  const id = `d-${provinceId}-${day.date}`;
  const name = dayLabel(day.date) ?? day.date;
  return (
    <section className="duty-day" aria-labelledby={id}>
      <h3 id={id}>
        {index === 0 ? <>اليوم <span className="muted">{name}</span></> : index === 1 ? <>غداً <span className="muted">{name}</span></> : name}
      </h3>
      {day.items.length === 0 ? <p className="muted duty-none">{NO_DUTY_DAY}</p> : <RosterList day={day} />}
    </section>
  );
}

/*
 * The day's pharmacies with their shift times. The availability badge is shown
 * for today only: it describes this moment, which says nothing about tomorrow.
 */
function RosterList({ day, showState = false }: { day: DutyDay; showState?: boolean }) {
  const shifts = new Map<string, DutyShift[]>();
  for (const s of day.shifts) shifts.set(s.facilityId, [...(shifts.get(s.facilityId) ?? []), s]);
  return (
    <ul className="list">
      {day.items.map((f) => (
        <li key={f.id} className="card row">
          <div>
            <Link href={facilityPath(f)} className="title-link">{f.nameAr}</Link>
            <div className="meta">
              <span>{f.category.nameAr}</span>
              {f.city ? <span>· {f.city.nameAr}</span> : null}
              <Rating average={f.ratingAverage} count={f.ratingCount} />
            </div>
            <ShiftTimes shifts={shifts.get(f.id) ?? []} day={day.date} />
          </div>
          {showState ? <StatusBadge state={f.availability.state} /> : null}
        </li>
      ))}
    </ul>
  );
}

/* «من 20:00 إلى 08:00 من اليوم التالي» — times are read left to right, like the opening hours. */
function ShiftTimes({ shifts, day }: { shifts: DutyShift[]; day: string }) {
  const spans = shifts.map((s) => shiftSpan(s.startsAt, s.endsAt, day)).filter((s) => s !== null);
  if (spans.length === 0) return null;
  return (
    <p className="shift">
      <Icon name="clock" size={16} />
      <span>
        {spans.map((s, i) => {
          const from = <time className="ltr" dateTime={s.fromIso}>{s.from}</time>;
          const to = <time className="ltr" dateTime={s.toIso}>{s.to}</time>;
          const midnight = <time dateTime={s.toIso}>منتصف الليل</time>;
          let text;
          if (s.startsBefore && s.endsAfter) text = <>طوال اليوم</>;
          else if (s.startsBefore) text = <>منذ اليوم السابق حتى {s.endsAtMidnight ? midnight : to}</>;
          else if (s.endsAfter) text = <>من {from} إلى {to} من اليوم التالي</>;
          else text = <>من {from} إلى {s.endsAtMidnight ? midnight : to}</>;
          return <span key={`${s.fromIso}-${s.toIso}`}>{i > 0 ? "، و" : null}{text}</span>;
        })}
      </span>
    </p>
  );
}
