import { DUTY_TITLES, DutyView } from "../../components/duty";
import { pageMetadata } from "../../lib/seo";

/*
 * /duty — pharmacies on duty right now, grouped by active province. Duty
 * changes over the day, so this page uses the same 5-minute ISR window as
 * the rest of the directory rather than being fully static.
 */
export const revalidate = 300;

export const metadata = pageMetadata({
  title: DUTY_TITLES.now,
  description: "قائمة الصيدليات المناوبة الآن في كل محافظة مع أرقام التواصل والموقع.",
  path: "/duty",
});

export default function DutyPage() {
  return <DutyView when="now" />;
}
