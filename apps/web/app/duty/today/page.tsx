import { DUTY_TITLES, DutyView } from "../../../components/duty";
import { pageMetadata } from "../../../lib/seo";

/* /duty/today — every pharmacy on today's duty roster, with its shift times. */
export const revalidate = 60;

export const metadata = pageMetadata({
  title: DUTY_TITLES.today,
  description: "الصيدليات المسجّلة في مناوبات اليوم في كل محافظة، مع أوقات المناوبة وأرقام التواصل والموقع.",
  path: "/duty/today",
});

export default function DutyTodayPage() {
  return <DutyView when="today" />;
}
