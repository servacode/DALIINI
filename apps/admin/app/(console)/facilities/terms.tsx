import { termsFor } from "../../../components/ui";

/** What each data-quality issue is called, everywhere the console names one. */
export const QUALITY_ISSUES: Record<string, string> = {
  NO_PHOTOS: "بلا صور",
  NO_HOURS: "بلا أوقات دوام",
  NO_LOCATION: "بلا موقع",
  NO_PHONE: "بلا هاتف",
  STALE: "لم تُحدَّث منذ 90 يوماً",
  OPEN_REPORTS: "عليها بلاغات",
  NOT_VERIFIED_RECENTLY: "لم يُتحقق منها مؤخراً",
};

export const STATUS = termsFor("facilityStatus");

export function QualityMeter({ score }: { score: number }) {
  const tone = score >= 80 ? "positive" : score >= 50 ? "warning" : "danger";
  return (
    <span className="quality" data-tone={tone} title={`مؤشر الجودة ${score} من 100`}>
      <span className="quality-bar" aria-hidden="true">
        <span style={{ inlineSize: `${Math.max(4, score)}%` }} />
      </span>
      <span className="tabular">{score}</span>
    </span>
  );
}
