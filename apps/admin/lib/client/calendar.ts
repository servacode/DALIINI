import { LOCALE } from "../locale";

/**
 * Calendar arithmetic on the Damascus clock.
 *
 * The duty roster is a list of Damascus calendar days, and an operator types a shift as a
 * wall-clock time in Damascus. The browser may be anywhere (a laptop left on UTC, a phone on
 * Beirut time), so nothing here uses the browser's own zone: days are plain `YYYY-MM-DD`
 * strings handled as UTC dates, and the only conversions between a Damascus wall clock and
 * an instant go through `Intl` with the zone named explicitly. The offset is looked up per
 * instant rather than assumed, so a future change of Syria's rules needs no code change.
 */

export const DAMASCUS = "Asia/Damascus";

const DAY_MS = 86_400_000;

type Instant = Date | string | number;

function parts(instant: Instant): Record<string, string> {
  return Object.fromEntries(
    new Intl.DateTimeFormat("en-GB", {
      timeZone: DAMASCUS,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(new Date(instant))
      .map((part) => [part.type, part.value]),
  );
}

function utcOf(day: string): number {
  const [year, month, date] = day.split("-").map(Number);
  return Date.UTC(year!, (month ?? 1) - 1, date ?? 1);
}

function dayOfUtc(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

/** The Damascus calendar day an instant falls on. */
export function damascusDay(instant: Instant): string {
  const p = parts(instant);
  return `${p.year}-${p.month}-${p.day}`;
}

/** The Damascus wall clock of an instant, as a time input spells it (`22:00`). */
export function damascusClock(instant: Instant): string {
  const p = parts(instant);
  return `${p.hour}:${p.minute}`;
}

/** Minutes Damascus is ahead of UTC at this instant. */
function offsetMinutes(ms: number): number {
  const p = parts(ms);
  const wall = Date.UTC(
    Number(p.year),
    Number(p.month) - 1,
    Number(p.day),
    Number(p.hour),
    Number(p.minute),
    Number(p.second),
  );
  return Math.round((wall - Math.floor(ms / 1000) * 1000) / 60_000);
}

/**
 * The instant a Damascus wall-clock time names, as ISO 8601 UTC.
 *
 * Two passes: the offset is read at a first guess and then again at the corrected instant,
 * which settles the answer even across a daylight-saving change.
 */
export function damascusInstant(day: string, time: string): string {
  const [hour, minute] = time.split(":").map(Number);
  const wall = utcOf(day) + ((hour ?? 0) * 60 + (minute ?? 0)) * 60_000;
  const first = wall - offsetMinutes(wall) * 60_000;
  const settled = wall - offsetMinutes(first) * 60_000;
  return new Date(settled).toISOString();
}

export function addDays(day: string, count: number): string {
  return dayOfUtc(utcOf(day) + count * DAY_MS);
}

/** Whole days from `from` to `to` (negative when `to` is earlier). */
export function daysBetween(from: string, to: string): number {
  return Math.round((utcOf(to) - utcOf(from)) / DAY_MS);
}

/** 0 for Sunday … 6 for Saturday, as `Date#getUTCDay`. */
export function weekdayOf(day: string): number {
  return new Date(utcOf(day)).getUTCDay();
}

/** The Saturday on or before a day: the Syrian week starts on Saturday. */
export function weekStart(day: string): string {
  return addDays(day, -((weekdayOf(day) + 1) % 7));
}

/** Weekday keys in the shared vocabulary, in Syrian week order. */
export const WEEK_ORDER = [
  "SATURDAY",
  "SUNDAY",
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
] as const;

const DAY_LABEL = new Intl.DateTimeFormat(LOCALE, {
  weekday: "long",
  day: "numeric",
  month: "long",
  timeZone: "UTC",
});
const DAY_MONTH = new Intl.DateTimeFormat(LOCALE, {
  day: "numeric",
  month: "long",
  timeZone: "UTC",
});
const DAY_NUMBER = new Intl.DateTimeFormat(LOCALE, { day: "numeric", timeZone: "UTC" });
const MONTH = new Intl.DateTimeFormat(LOCALE, { month: "long", timeZone: "UTC" });
const CLOCK = new Intl.DateTimeFormat(LOCALE, {
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
  timeZone: DAMASCUS,
});

/** «السبت، 26 أيلول» */
export function formatDayLabel(day: string): string {
  return DAY_LABEL.format(new Date(utcOf(day)));
}

/** «26 أيلول» */
export function formatDayMonth(day: string): string {
  return DAY_MONTH.format(new Date(utcOf(day)));
}

/** «26» */
export function formatDayNumber(day: string): string {
  return DAY_NUMBER.format(new Date(utcOf(day)));
}

/** «أيلول» */
export function formatMonth(day: string): string {
  return MONTH.format(new Date(utcOf(day)));
}

/** «22:00», on the Damascus clock. */
export function formatClock(instant: Instant): string {
  return CLOCK.format(new Date(instant));
}

/**
 * How a shift reads inside one day's cell.
 *
 * An overnight shift appears on both days it touches; the second day says only when it
 * ends, and a shift that covers the whole day says so, so a cell never shows a range that
 * seems to run backwards.
 */
export function shiftSpanOn(
  day: string,
  startsAt: string,
  endsAt: string,
): string {
  const startDay = damascusDay(startsAt);
  // The end is exclusive: a shift ending at midnight belongs to the day before it.
  const endDay = damascusDay(new Date(new Date(endsAt).getTime() - 1));
  const startsBefore = startDay < day;
  const endsAfter = endDay > day;
  if (startsBefore && endsAfter) return "طوال اليوم";
  if (startsBefore) return `حتى ${formatClock(endsAt)}`;
  return `${formatClock(startsAt)} – ${formatClock(endsAt)}`;
}

/** True when an end time on the same form reads as the next morning (22:00 → 08:00). */
export function endsNextDay(start: string, end: string): boolean {
  return end <= start;
}
