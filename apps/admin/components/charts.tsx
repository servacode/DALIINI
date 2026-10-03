"use client";

import { useEffect, useId, useRef, useState } from "react";

/**
 * Charts drawn by hand in SVG (DECISION-075).
 *
 * The console has no charting library, and two shapes do not justify one: a line over days
 * and a set of bars. Both follow the page's colours and theme because they are drawn with
 * its tokens, and both read right to left like the page: the earliest day is on the right.
 *
 * A chart is never the only copy of its numbers. Every line chart carries the same values as
 * a table for a screen reader, and the legend gives each series its total.
 */

export type LineSeries = Readonly<{
  key: string;
  label: string;
  /** A CSS colour, normally a token: `var(--ad-brand)`. */
  color: string;
  values: readonly number[];
}>;

const NUMBER = new Intl.NumberFormat("ar-SY");
const DAY = new Intl.DateTimeFormat("ar-SY", { day: "numeric", month: "short", timeZone: "UTC" });
const FULL_DAY = new Intl.DateTimeFormat("ar-SY", {
  weekday: "long",
  day: "numeric",
  month: "long",
  timeZone: "UTC",
});

function dayLabel(iso: string, full = false): string {
  // A day may arrive as "2026-09-24" or, through the generated client, as a midnight timestamp.
  const date = new Date(`${iso.slice(0, 10)}T00:00:00Z`);
  return Number.isNaN(date.getTime()) ? iso : (full ? FULL_DAY : DAY).format(date);
}

/** Round numbers for the vertical axis: 0, and three steps up to a value at or above `max`. */
export function niceTicks(max: number, count = 4): number[] {
  if (max <= 0) return [0, 1];
  const raw = max / (count - 1);
  const magnitude = 10 ** Math.floor(Math.log10(raw));
  const step = [1, 2, 2.5, 5, 10].map((f) => f * magnitude).find((s) => s >= raw) ?? raw;
  const top = Math.max(1, Math.ceil(max / step)) * step;
  const ticks: number[] = [];
  for (let value = 0; value <= top + step / 2; value += step) ticks.push(Math.round(value * 100) / 100);
  return ticks;
}

/** The width the chart actually has, so text stays its real size on a phone and a desktop. */
function useWidth(): [React.RefObject<HTMLDivElement | null>, number] {
  const ref = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);
  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    if (typeof ResizeObserver === "undefined") {
      // An old browser: measure once. The legend and the table hold the numbers regardless.
      const frame = requestAnimationFrame(() => setWidth(node.clientWidth));
      return () => cancelAnimationFrame(frame);
    }
    const observer = new ResizeObserver(([entry]) => {
      if (entry) setWidth(Math.round(entry.contentRect.width));
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, []);
  return [ref, width];
}

export function LineChart({
  days,
  series,
  caption,
  height = 240,
}: {
  /** ISO dates, oldest first, one per value. */
  days: readonly string[];
  series: readonly LineSeries[];
  caption: string;
  height?: number;
}) {
  const [box, width] = useWidth();
  const [hover, setHover] = useState<number | null>(null);
  const titleId = useId();

  const pad = { top: 12, bottom: 28, start: 44, end: 12 };
  const plotW = Math.max(0, width - pad.start - pad.end);
  const plotH = height - pad.top - pad.bottom;
  const max = Math.max(0, ...series.flatMap((s) => s.values));
  const ticks = niceTicks(max);
  const top = ticks[ticks.length - 1] || 1;
  const n = days.length;
  // Right to left: the first day sits at the right edge of the plot, inside the axis labels.
  const x = (i: number) =>
    n <= 1 ? pad.end + plotW / 2 : pad.end + plotW - (i / (n - 1)) * plotW;
  const y = (value: number) => pad.top + plotH - (value / top) * plotH;
  const labelEvery = Math.max(1, Math.ceil(n / Math.max(2, Math.floor(plotW / 70))));

  function pick(clientX: number, rect: DOMRect): void {
    if (n === 0) return;
    const offset = clientX - rect.left;
    const ratio = n <= 1 ? 0 : (pad.end + plotW - offset) / plotW;
    setHover(Math.min(n - 1, Math.max(0, Math.round(ratio * (n - 1)))));
  }

  return (
    <figure className="chart" aria-labelledby={titleId}>
      <figcaption id={titleId} className="sr-only">
        {caption}
      </figcaption>
      <div className="chart-plot" ref={box} style={{ height }}>
        {width > 0 ? (
          <svg
            width={width}
            height={height}
            // Coordinates are laid out by hand; the text inside still shapes as Arabic.
            style={{ direction: "ltr" }}
            role="img"
            aria-label={caption}
            onPointerMove={(event) => pick(event.clientX, event.currentTarget.getBoundingClientRect())}
            onPointerLeave={() => setHover(null)}
          >
            {ticks.map((tick) => (
              <g key={tick}>
                <line
                  className="chart-grid"
                  x1={pad.end}
                  x2={pad.end + plotW}
                  y1={y(tick)}
                  y2={y(tick)}
                />
                <text className="chart-axis" x={width - 4} y={y(tick)} dy="0.32em" textAnchor="end">
                  {NUMBER.format(tick)}
                </text>
              </g>
            ))}
            {days.map((day, i) =>
              // Every few days, and always the last one; a regular label too close to the last
              // is dropped so the two never overlap.
              (i % labelEvery === 0 && n - 1 - i >= labelEvery / 2) || i === n - 1 ? (
                <text
                  key={day}
                  className="chart-axis"
                  x={x(i)}
                  y={height - 8}
                  // The days at either edge are anchored inward, so their labels stay inside.
                  textAnchor={n > 1 && i === 0 ? "end" : n > 1 && i === n - 1 ? "start" : "middle"}
                >
                  {dayLabel(day)}
                </text>
              ) : null,
            )}
            {series.map((s) => (
              <path
                key={s.key}
                className="chart-line"
                d={s.values.map((v, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(v)}`).join(" ")}
                stroke={s.color}
              />
            ))}
            {hover !== null ? (
              <g>
                <line className="chart-guide" x1={x(hover)} x2={x(hover)} y1={pad.top} y2={pad.top + plotH} />
                {series.map((s) => (
                  <circle
                    key={s.key}
                    cx={x(hover)}
                    cy={y(s.values[hover] ?? 0)}
                    r={4}
                    fill={s.color}
                    className="chart-dot"
                  />
                ))}
              </g>
            ) : null}
          </svg>
        ) : null}
        {hover !== null && width > 0 ? (
          <div
            className="chart-tip"
            style={{ left: Math.min(Math.max(8, x(hover) - 90), Math.max(8, width - 188)) }}
            role="presentation"
          >
            <strong>{dayLabel(days[hover] ?? "", true)}</strong>
            {series.map((s) => (
              <span key={s.key}>
                <i style={{ background: s.color }} />
                {s.label}: <b className="tabular">{NUMBER.format(s.values[hover] ?? 0)}</b>
              </span>
            ))}
          </div>
        ) : null}
      </div>
      <ul className="chart-legend">
        {series.map((s) => (
          <li key={s.key}>
            <i style={{ background: s.color }} />
            {s.label}
            <b className="tabular">{NUMBER.format(s.values.reduce((sum, v) => sum + v, 0))}</b>
          </li>
        ))}
      </ul>
      <table className="sr-only">
        <caption>{caption}</caption>
        <thead>
          <tr>
            <th scope="col">اليوم</th>
            {series.map((s) => (
              <th key={s.key} scope="col">
                {s.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {days.map((day, i) => (
            <tr key={day}>
              <th scope="row">{dayLabel(day, true)}</th>
              {series.map((s) => (
                <td key={s.key}>{s.values[i] ?? 0}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}

export type Bar = Readonly<{ key: string; label: string; value: number; color: string; href?: string }>;

/** Horizontal bars: direction-neutral, labelled, and each one optionally a link to its list. */
export function BarChart({ bars, caption }: { bars: readonly Bar[]; caption: string }) {
  const max = Math.max(1, ...bars.map((bar) => bar.value));
  return (
    <figure className="bar-chart">
      <figcaption className="sr-only">{caption}</figcaption>
      <ul>
        {bars.map((bar) => {
          const body = (
            <>
              <span className="bar-label">{bar.label}</span>
              <span className="bar-track" aria-hidden="true">
                <span
                  className="bar-fill"
                  style={{ inlineSize: `${Math.max(bar.value > 0 ? 2 : 0, (bar.value / max) * 100)}%`, background: bar.color }}
                />
              </span>
              <b className="bar-value tabular">{NUMBER.format(bar.value)}</b>
            </>
          );
          return (
            <li key={bar.key}>
              {bar.href ? (
                <a className="bar-row" href={bar.href}>
                  {body}
                </a>
              ) : (
                <span className="bar-row">{body}</span>
              )}
            </li>
          );
        })}
      </ul>
    </figure>
  );
}
