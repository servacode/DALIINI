/*
 * Dates as a reader says them: «اليوم»، «أمس»، «قبل ٣ أيام». Older than a month
 * reads as a calendar date. Pages are cached for minutes, so the precision stops
 * at hours; "now" is the render time.
 *
 * Calendar days and clock times are Damascus local time, like the API's own
 * days (its TIME_ZONE), whatever zone the web server runs in.
 */
export const SITE_TIME_ZONE = "Asia/Damascus";

const relative = new Intl.RelativeTimeFormat("ar-SY", { numeric: "auto" });
const calendar = new Intl.DateTimeFormat("ar-SY", { dateStyle: "long", timeZone: SITE_TIME_ZONE });

const HOUR = 3600_000;
const DAY = 24 * HOUR;

export interface SpokenDate {
  /* «قبل ٣ أيام», or «في ١٢ آذار ٢٠٢٦» when older than a month. */
  text: string;
  /* The full calendar date, for a tooltip. */
  full: string;
  iso: string;
}

function parse(value: string | null | undefined): Date | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function spokenDate(value: string | null | undefined, now: number = Date.now()): SpokenDate | null {
  const date = parse(value);
  if (!date) return null;
  const full = calendar.format(date);
  const ago = Math.max(0, now - date.getTime());
  let text: string;
  if (ago < HOUR) text = "قبل قليل";
  else if (ago < 2 * HOUR) text = "قبل ساعة";
  else if (ago < DAY) text = relative.format(-Math.floor(ago / HOUR), "hour");
  else if (ago < 30 * DAY) text = relative.format(-Math.floor(ago / DAY), "day");
  else text = `في ${full}`;
  return { text, full, iso: date.toISOString() };
}

/* «٢٨ أيلول ٢٠٢٦»: the exact date, for a document's «آخر تحديث». */
export function calendarDate(value: string | null | undefined): { text: string; iso: string } | null {
  const date = parse(value);
  return date ? { text: calendar.format(date), iso: date.toISOString() } : null;
}

/* A roster day, "YYYY-MM-DD", as «الثلاثاء، ٢٩ أيلول». The date is already a local day. */
const dayFormat = new Intl.DateTimeFormat("ar-SY", { weekday: "long", day: "numeric", month: "long", timeZone: "UTC" });

export function dayLabel(day: string): string | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) return null;
  const date = new Date(`${day}T12:00:00Z`);
  return Number.isNaN(date.getTime()) ? null : dayFormat.format(date);
}

const clock = new Intl.DateTimeFormat("en-GB", { hour: "2-digit", minute: "2-digit", hourCycle: "h23", timeZone: SITE_TIME_ZONE });
const localParts = new Intl.DateTimeFormat("en-CA", { year: "numeric", month: "2-digit", day: "2-digit", timeZone: SITE_TIME_ZONE });

/* "YYYY-MM-DD" of an instant, in Damascus. */
function localDay(date: Date): string {
  const parts = Object.fromEntries(localParts.formatToParts(date).map((p) => [p.type, p.value]));
  return `${parts.year}-${parts.month}-${parts.day}`;
}

/*
 * A duty shift as seen from one roster day: its local start and end times, and
 * whether it began before that day or runs past it. A shift ending exactly at
 * midnight ends with the day. Times read like the opening hours: "20:00".
 */
export interface ShiftSpan {
  from: string;
  to: string;
  fromIso: string;
  toIso: string;
  startsBefore: boolean;
  endsAfter: boolean;
  endsAtMidnight: boolean;
}

export function shiftSpan(startsAt: string, endsAt: string, day: string): ShiftSpan | null {
  const start = parse(startsAt);
  const end = parse(endsAt);
  if (!start || !end) return null;
  const to = clock.format(end);
  const endDay = localDay(end);
  const endsAtMidnight = to === "00:00" && endDay > day && localDay(new Date(end.getTime() - 1)) === day;
  return {
    from: clock.format(start),
    to,
    fromIso: start.toISOString(),
    toIso: end.toISOString(),
    startsBefore: localDay(start) < day,
    endsAfter: endDay > day && !endsAtMidnight,
    endsAtMidnight,
  };
}
