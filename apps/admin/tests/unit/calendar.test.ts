import { describe, expect, it } from "vitest";

import {
  addDays,
  damascusClock,
  damascusDay,
  damascusInstant,
  daysBetween,
  endsNextDay,
  shiftSpanOn,
  weekStart,
  weekdayOf,
} from "../../lib/client/calendar";
import { SLUG_PATTERN, bodyBlocks } from "../../lib/client/content";

/**
 * Damascus calendar arithmetic for the duty roster.
 *
 * The operator types a wall-clock time in Damascus and the backend stores an instant; the
 * browser's own zone must not leak into either direction, or a night shift entered at 22:00
 * would be saved three hours off on a laptop left on UTC. These run in whatever zone the
 * test machine has, which is the point.
 */

describe("days and weeks", () => {
  it("reads the Damascus day of an instant, not the machine's", () => {
    expect(damascusDay("2026-09-28T20:59:00Z")).toBe("2026-09-28");
    expect(damascusDay("2026-09-28T21:00:00Z")).toBe("2026-09-29");
  });

  it("adds days across month ends and counts between them", () => {
    expect(addDays("2026-09-28", 5)).toBe("2026-10-03");
    expect(addDays("2026-03-01", -1)).toBe("2026-02-28");
    expect(daysBetween("2026-09-26", "2026-10-09")).toBe(13);
  });

  it("starts the week on Saturday", () => {
    expect(weekdayOf("2026-09-26")).toBe(6);
    expect(weekStart("2026-09-26")).toBe("2026-09-26");
    expect(weekStart("2026-09-28")).toBe("2026-09-26");
    expect(weekStart("2026-10-02")).toBe("2026-09-26");
    expect(weekStart("2026-10-03")).toBe("2026-10-03");
  });
});

describe("wall clock and instants", () => {
  it("turns a Damascus wall-clock time into the right instant", () => {
    expect(damascusInstant("2026-09-28", "22:00")).toBe("2026-09-28T19:00:00.000Z");
    expect(damascusInstant("2026-09-29", "08:00")).toBe("2026-09-29T05:00:00.000Z");
    expect(damascusInstant("2026-09-28", "00:00")).toBe("2026-09-27T21:00:00.000Z");
  });

  it("reads an instant back as the time a time input shows", () => {
    expect(damascusClock("2026-09-28T19:00:00.000Z")).toBe("22:00");
    expect(damascusClock(damascusInstant("2026-01-15", "07:30"))).toBe("07:30");
  });

  it("rolls an end at or before the start to the next morning", () => {
    expect(endsNextDay("22:00", "08:00")).toBe(true);
    expect(endsNextDay("08:00", "08:00")).toBe(true);
    expect(endsNextDay("08:00", "14:00")).toBe(false);
  });
});

describe("a shift inside one day's cell", () => {
  const night = ["2026-09-28T19:00:00.000Z", "2026-09-29T05:00:00.000Z"] as const;

  it("shows the whole range on the day it starts", () => {
    expect(shiftSpanOn("2026-09-28", ...night)).toBe("٢٢:٠٠ – ٠٨:٠٠");
  });

  it("shows only the end on the morning it runs into", () => {
    expect(shiftSpanOn("2026-09-29", ...night)).toBe("حتى ٠٨:٠٠");
  });

  it("says a day is covered whole when a shift spans it", () => {
    expect(
      shiftSpanOn("2026-09-29", "2026-09-28T05:00:00.000Z", "2026-09-30T05:00:00.000Z"),
    ).toBe("طوال اليوم");
  });

  it("does not carry a shift that ends at midnight into the next day", () => {
    expect(
      shiftSpanOn("2026-09-28", "2026-09-28T05:00:00.000Z", "2026-09-28T21:00:00.000Z"),
    ).toBe("٠٨:٠٠ – ٠٠:٠٠");
  });
});

describe("content pages", () => {
  it("reads a body the way the site does: blank lines between blocks, questions first", () => {
    expect(
      bodyBlocks("مرحباً بك.\nسطر ثانٍ.\n\nكيف أضيف منشأة؟\nمن التطبيق.\n\n\n  \nآخر فقرة"),
    ).toEqual([
      { kind: "text", lines: ["مرحباً بك.", "سطر ثانٍ."] },
      { kind: "question", question: "كيف أضيف منشأة؟", answer: ["من التطبيق."] },
      { kind: "text", lines: ["آخر فقرة"] },
    ]);
  });

  it("accepts only the slugs the backend accepts", () => {
    for (const slug of ["about", "about-us", "a", "faq-2026"]) expect(SLUG_PATTERN.test(slug)).toBe(true);
    for (const slug of ["-about", "about-", "من-نحن", "a b", "a/b", ""]) {
      expect(SLUG_PATTERN.test(slug)).toBe(false);
    }
  });
});
