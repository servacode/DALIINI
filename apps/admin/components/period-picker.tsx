"use client";

import { useState } from "react";

import { addDays, daysBetween, formatDayMonth } from "../lib/client/calendar";

/**
 * The reporting period for the analytics screens: three presets and a custom range.
 *
 * Dates are whole Damascus days, inclusive at both ends, which is how the backend reads a
 * bare `to` date ("includes that whole day"). The previous period the numbers are compared
 * against is chosen by the backend: the same length, ending where this one starts.
 */

export type Period = Readonly<{ from: string; to: string }>;

/** The backend's ceiling for one period (`ANALYTICS_MAX_DAYS`). */
const MAX_DAYS = 366;

const PRESETS = [
  [7, "٧ أيام"],
  [30, "٣٠ يوماً"],
  [90, "٩٠ يوماً"],
] as const;

/** The last `days` days, ending today. */
export function lastDays(days: number, today: string): Period {
  return { from: addDays(today, -(days - 1)), to: today };
}

/** «٢٩ آب – ٢٧ أيلول» */
export function periodLabel(period: Period): string {
  return `${formatDayMonth(period.from)} – ${formatDayMonth(period.to)}`;
}

export function PeriodPicker({
  value,
  today,
  onChange,
}: {
  value: Period;
  today: string;
  onChange: (period: Period) => void;
}) {
  const [draft, setDraft] = useState<Period>(value);
  const [problem, setProblem] = useState<string | null>(null);
  const preset = PRESETS.find(([days]) => {
    const candidate = lastDays(days, today);
    return candidate.from === value.from && candidate.to === value.to;
  })?.[0];

  function apply(next: Period): void {
    if (!next.from || !next.to) {
      setProblem("اختر تاريخ البداية وتاريخ النهاية.");
      return;
    }
    if (next.from > next.to) {
      setProblem("تاريخ البداية يجب أن يسبق تاريخ النهاية.");
      return;
    }
    if (daysBetween(next.from, next.to) + 1 > MAX_DAYS) {
      setProblem("الفترة لا تتجاوز سنة واحدة.");
      return;
    }
    setProblem(null);
    onChange(next);
  }

  return (
    <div className="period-picker" data-testid="period-picker">
      <div className="period-presets" role="group" aria-label="الفترة">
        <span className="period-label" aria-hidden="true">
          الفترة
        </span>
        {PRESETS.map(([days, label]) => (
          <button
            key={days}
            type="button"
            className="button-ghost"
            aria-pressed={preset === days}
            data-testid={`period-${days}`}
            onClick={() => {
              const next = lastDays(days, today);
              setDraft(next);
              apply(next);
            }}
          >
            {label}
          </button>
        ))}
      </div>
      <form
        className="period-custom"
        onSubmit={(event) => {
          event.preventDefault();
          apply(draft);
        }}
      >
        <label className="filter-field">
          <span>من</span>
          <input
            type="date"
            dir="ltr"
            value={draft.from}
            max={draft.to || today}
            data-testid="period-from"
            onChange={(event) => setDraft({ ...draft, from: event.target.value })}
          />
        </label>
        <label className="filter-field">
          <span>إلى</span>
          <input
            type="date"
            dir="ltr"
            value={draft.to}
            min={draft.from || undefined}
            max={today}
            data-testid="period-to"
            onChange={(event) => setDraft({ ...draft, to: event.target.value })}
          />
        </label>
        <button type="submit" className="button-primary">
          تطبيق
        </button>
      </form>
      {problem ? (
        <p className="field-error period-error" role="alert">
          {problem}
        </p>
      ) : null}
    </div>
  );
}
