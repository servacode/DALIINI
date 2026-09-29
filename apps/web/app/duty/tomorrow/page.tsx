import { DUTY_TITLES, DutyView } from "../../../components/duty";
import { pageMetadata } from "../../../lib/seo";

/* /duty/tomorrow — tomorrow's duty roster, with each pharmacy's shift times. */
export const revalidate = 60;

export const metadata = pageMetadata({
  title: DUTY_TITLES.tomorrow,
  description: "الصيدليات المناوبة غداً في كل محافظة، مع أوقات المناوبة وأرقام التواصل والموقع.",
  path: "/duty/tomorrow",
});

export default function DutyTomorrowPage() {
  return <DutyView when="tomorrow" />;
}
