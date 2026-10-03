import { DUTY_TITLES, DutyView } from "../../components/duty";
import { pageMetadata } from "../../lib/seo";

/*
 * /duty — pharmacies on duty right now, grouped by active province. "Right now" goes stale
 * faster than anything else on the site, so it is refreshed every minute, like the roster pages
 * beside it (DUTY_REVALIDATE_SECONDS), rather than on the directory's five.
 */
export const revalidate = 60;

export const metadata = pageMetadata({
  title: DUTY_TITLES.now,
  description: "قائمة الصيدليات المناوبة الآن في كل محافظة مع أرقام التواصل والموقع.",
  path: "/duty",
});

export default function DutyPage() {
  return <DutyView when="now" />;
}
