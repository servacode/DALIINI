import { describe, expect, it } from "vitest";

import {
  WEEKDAYS_AR,
  WEEKDAY_DISPLAY_ORDER,
  calendarDate,
  dayLabel,
  shiftSpan,
  spokenDate,
} from "../../lib/dates";

/**
 * Time as a reader here experiences it.
 *
 * Two things make this worth testing rather than reading: every calendar day and clock time is
 * Damascus local whatever zone the server runs in, and a duty shift routinely crosses midnight.
 * A shift shown on the wrong day sends somebody to a closed pharmacy at night, which is the one
 * thing this platform exists to prevent.
 */

// A fixed instant to measure "ago" from, so these never depend on when they run.
const NOW = Date.parse("2026-10-01T12:00:00Z");

describe("how long ago something was", () => {
  it("says «قبل قليل» inside the hour, because the pages are cached in minutes anyway", () => {
    expect(spokenDate("2026-10-01T11:30:00Z", NOW)?.text).toBe("قبل قليل");
  });

  it("counts hours, then days", () => {
    expect(spokenDate("2026-10-01T09:00:00Z", NOW)?.text).toContain("٣");
    expect(spokenDate("2026-09-28T12:00:00Z", NOW)?.text).toContain("٣");
  });

  it("gives up on «ago» after a month and names the date", () => {
    const spoken = spokenDate("2026-07-01T12:00:00Z", NOW);
    expect(spoken?.text.startsWith("في ")).toBe(true);
    expect(spoken?.text).toContain(spoken!.full);
  });

  it("never says something happened in the future, however the clocks disagree", () => {
    // A server minutes ahead of the timestamp it is rendering must not say «بعد ساعة».
    expect(spokenDate("2026-10-01T12:30:00Z", NOW)?.text).toBe("قبل قليل");
  });

  it("has nothing to say about nothing, or about an unreadable value", () => {
    expect(spokenDate(null, NOW)).toBeNull();
    expect(spokenDate("not a date", NOW)).toBeNull();
    expect(calendarDate(undefined)).toBeNull();
  });
});

describe("a roster day's label", () => {
  it("names the weekday and the day of the month", () => {
    const label = dayLabel("2026-10-01");
    expect(label).toBeTruthy();
    expect(label).toContain("١");
  });

  it("refuses anything that is not a plain calendar day", () => {
    expect(dayLabel("2026-10-01T00:00:00Z")).toBeNull();
    expect(dayLabel("01/10/2026")).toBeNull();
    expect(dayLabel("")).toBeNull();
  });
});

describe("a duty shift seen from one roster day", () => {
  it("reads the local clock, not the server's", () => {
    // 17:00Z is 20:00 in Damascus; a server in London must still say 20:00.
    const span = shiftSpan("2026-10-01T17:00:00Z", "2026-10-01T21:00:00Z", "2026-10-01");
    expect(span?.from).toBe("20:00");
    expect(span?.to).toBe("00:00");
  });

  it("treats a shift ending exactly at midnight as ending with the day", () => {
    // Not «يمتد إلى الغد»: it ends tonight, and saying otherwise sends somebody out at 00:30.
    const span = shiftSpan("2026-10-01T17:00:00Z", "2026-10-01T21:00:00Z", "2026-10-01");
    expect(span?.endsAtMidnight).toBe(true);
    expect(span?.endsAfter).toBe(false);
  });

  it("marks a shift that truly runs past the day", () => {
    const span = shiftSpan("2026-10-01T19:00:00Z", "2026-10-02T03:00:00Z", "2026-10-01");
    expect(span?.endsAfter).toBe(true);
    expect(span?.endsAtMidnight).toBe(false);
  });

  it("marks a shift that began the day before", () => {
    const span = shiftSpan("2026-09-30T20:00:00Z", "2026-10-01T03:00:00Z", "2026-10-01");
    expect(span?.startsBefore).toBe(true);
  });

  it("has nothing to show for an unreadable shift", () => {
    expect(shiftSpan("nonsense", "2026-10-01T03:00:00Z", "2026-10-01")).toBeNull();
  });
});

describe("the week", () => {
  it("has seven names, each from the shared vocabulary", () => {
    expect(WEEKDAYS_AR).toHaveLength(7);
    expect(new Set(WEEKDAYS_AR).size).toBe(7);
    expect(WEEKDAYS_AR.every((name) => name.trim().length > 0)).toBe(true);
  });

  it("is displayed from Saturday, as the week is counted here", () => {
    expect(WEEKDAY_DISPLAY_ORDER).toHaveLength(7);
    expect(new Set(WEEKDAY_DISPLAY_ORDER).size).toBe(7);
    // The backend counts Monday as 0, so Saturday is 5.
    expect(WEEKDAY_DISPLAY_ORDER[0]).toBe(5);
  });
});
