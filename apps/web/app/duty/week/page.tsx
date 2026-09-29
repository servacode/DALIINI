import { DUTY_TITLES, DutyView } from "../../../components/duty";
import { pageMetadata } from "../../../lib/seo";

/* /duty/week — the duty roster for today and the six days after it, day by day. */
export const revalidate = 60;

export const metadata = pageMetadata({
  title: DUTY_TITLES.week,
  description: "مناوبات الصيدليات لسبعة أيام في كل محافظة، يوماً بيوم، مع أوقات المناوبة.",
  path: "/duty/week",
});

export default function DutyWeekPage() {
  return <DutyView when="week" />;
}
