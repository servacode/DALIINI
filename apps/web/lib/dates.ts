/*
 * Dates as a reader says them: «اليوم»، «أمس»، «قبل ٣ أيام». Older than a month
 * reads as a calendar date. Pages are cached for minutes, so the precision stops
 * at hours; "now" is the render time.
 */
const relative = new Intl.RelativeTimeFormat("ar-SY", { numeric: "auto" });
const calendar = new Intl.DateTimeFormat("ar-SY", { dateStyle: "long" });

const HOUR = 3600_000;
const DAY = 24 * HOUR;

export interface SpokenDate {
  /* «قبل ٣ أيام», or «في ١٢ آذار ٢٠٢٦» when older than a month. */
  text: string;
  /* The full calendar date, for a tooltip. */
  full: string;
  iso: string;
}

export function spokenDate(value: string | null | undefined, now: number = Date.now()): SpokenDate | null {
  if (!value) return null;
  const date = new Date(value);
  const time = date.getTime();
  if (Number.isNaN(time)) return null;
  const full = calendar.format(date);
  const ago = Math.max(0, now - time);
  let text: string;
  if (ago < HOUR) text = "قبل قليل";
  else if (ago < 2 * HOUR) text = "قبل ساعة";
  else if (ago < DAY) text = relative.format(-Math.floor(ago / HOUR), "hour");
  else if (ago < 30 * DAY) text = relative.format(-Math.floor(ago / DAY), "day");
  else text = `في ${full}`;
  return { text, full, iso: date.toISOString() };
}
