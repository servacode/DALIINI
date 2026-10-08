"use client";

import {
  type ReactNode,
  useEffect,
  useEffectEvent,
  useId,
  useRef,
  useState,
} from "react";

import { Icons } from "../icons";
import { BrandMark } from "./index";

/**
 * Shared components added with the operations screens (duty roster, content, broadcast,
 * readiness, analytics).
 *
 * They live beside `index.tsx` rather than inside it only so that concurrent work on the
 * core set merges cleanly; they follow the same rules — tokens only, logical properties,
 * RTL by inheritance, every state visible — and every one is shown on the design page.
 */

const NUMBER = new Intl.NumberFormat("ar-SY");
const PERCENT = new Intl.NumberFormat("ar-SY", { style: "percent", maximumFractionDigits: 1 });

// --------------------------------------------------------------------------------------
// Side panel
// --------------------------------------------------------------------------------------

/**
 * A sheet that slides in from the inline end, for working on one thing (a day's duty
 * shifts, a province's launch checklist) without leaving the screen behind it.
 *
 * It is a modal dialog: focus moves in on open and returns on close, Escape and the scrim
 * close it — unless `locked`, which a screen sets while a confirmation is open on top of it
 * or a save is in flight, so one Escape does not close two things.
 */
export function SidePanel({
  open,
  title,
  description,
  onClose,
  locked,
  footer,
  children,
  testId,
}: {
  open: boolean;
  title: string;
  description?: ReactNode;
  onClose: () => void;
  locked?: boolean;
  footer?: ReactNode;
  children: ReactNode;
  testId?: string;
}) {
  const headingId = useId();
  const sheet = useRef<HTMLElement>(null);
  const close = () => {
    if (!locked) onClose();
  };
  // Read at key time, so the listener sees the current `locked` and `onClose` without the
  // effect re-running (and re-focusing the sheet) on every render of the screen behind it.
  const onEscape = useEffectEvent(close);

  useEffect(() => {
    if (!open) return;
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    sheet.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onEscape();
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("keydown", onKey);
      previous?.focus();
    };
  }, [open]);

  if (!open) return null;

  return (
    <div
      className="sheet-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) close();
      }}
    >
      <aside
        className="sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby={headingId}
        tabIndex={-1}
        ref={sheet}
        data-testid={testId}
      >
        <header className="sheet-header">
          <div className="sheet-title">
            <h2 id={headingId}>{title}</h2>
            {description ? <p>{description}</p> : null}
          </div>
          <button
            type="button"
            className="icon-button"
            aria-label="إغلاق"
            onClick={close}
            disabled={locked}
            data-testid="sheet-close"
          >
            <Icons.close />
          </button>
        </header>
        <div className="sheet-body">{children}</div>
        {footer ? <footer className="sheet-footer">{footer}</footer> : null}
      </aside>
    </div>
  );
}

// --------------------------------------------------------------------------------------
// Change against a previous period
// --------------------------------------------------------------------------------------

/**
 * Percentage change from `previous` to `current`, or null when there is nothing honest to
 * say: a missing value, or a previous period of zero (any rise from nothing is "infinite",
 * which is not a number an operator can use).
 */
export function percentChange(
  current: number | null | undefined,
  previous: number | null | undefined,
): number | null {
  if (current == null || previous == null) return null;
  if (!Number.isFinite(current) || !Number.isFinite(previous)) return null;
  if (previous === 0) return current === 0 ? 0 : null;
  return ((current - previous) / Math.abs(previous)) * 100;
}

/**
 * Arrow and percentage against the previous period, coloured by whether the move is good.
 *
 * More searches is good; more searches with no result, or a slower approval, is not — the
 * caller says which way is better, the colour follows, and the sentence read to a screen
 * reader says the direction in words.
 */
export function Trend({
  current,
  previous,
  lowerIsBetter,
}: {
  current: number | null | undefined;
  previous: number | null | undefined;
  lowerIsBetter?: boolean;
}) {
  const change = percentChange(current, previous);
  if (change === null) {
    return (
      <span className="trend" data-tone="neutral" data-testid="trend">
        لا مقارنة
      </span>
    );
  }
  const rounded = Math.round(change * 10) / 10;
  const direction = rounded > 0 ? "up" : rounded < 0 ? "down" : "flat";
  const tone =
    direction === "flat" ? "neutral" : (direction === "up") !== Boolean(lowerIsBetter) ? "positive" : "negative";
  const amount = PERCENT.format(Math.abs(rounded) / 100);
  const sentence =
    direction === "flat"
      ? "دون تغيير عن الفترة السابقة"
      : `${direction === "up" ? "ارتفاع" : "انخفاض"} ${amount} عن الفترة السابقة`;
  return (
    <span
      className="trend"
      data-tone={tone}
      data-direction={direction}
      title={sentence}
      data-testid="trend"
    >
      <span className="trend-arrow" aria-hidden="true">
        {direction === "up" ? "↑" : direction === "down" ? "↓" : "="}
      </span>
      <span aria-hidden="true">{amount}</span>
      <span className="sr-only">{sentence}</span>
    </span>
  );
}

// --------------------------------------------------------------------------------------
// Form helpers
// --------------------------------------------------------------------------------------

/** Characters used against the limit, counted as the backend counts them (code points). */
export function CharCount({ value, max, id }: { value: string; max: number; id?: string }) {
  const count = Array.from(value).length;
  return (
    <span
      id={id}
      className="char-count"
      data-over={count > max || undefined}
      aria-label={`${NUMBER.format(count)} من ${NUMBER.format(max)} حرفاً`}
    >
      {NUMBER.format(count)}/{NUMBER.format(max)}
    </span>
  );
}

/**
 * One choice among a few, laid out as a segmented control. Native radio buttons underneath,
 * so the arrow keys, the form semantics and the screen-reader announcement are the
 * browser's own.
 */
export function Segmented<T extends string>({
  label,
  name,
  value,
  options,
  onChange,
}: {
  label: string;
  name: string;
  value: T;
  options: readonly { value: T; label: string; hint?: string }[];
  onChange: (value: T) => void;
}) {
  return (
    <fieldset className="segmented-field">
      <legend>{label}</legend>
      <div className="segmented">
        {options.map((option) => (
          <label key={option.value} className="segment">
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={value === option.value}
              data-testid={`${name}-${option.value}`}
              onChange={() => onChange(option.value)}
            />
            <span>
              {option.label}
              {option.hint ? <small>{option.hint}</small> : null}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}

// --------------------------------------------------------------------------------------
// Checklist
// --------------------------------------------------------------------------------------

export type ChecklistItem = Readonly<{
  key: string;
  ok: boolean;
  title: string;
  detail?: string;
  action?: ReactNode;
}>;

/** Conditions and whether each holds: a tick when it does, a warning with the reason when not. */
export function Checklist({ items }: { items: readonly ChecklistItem[] }) {
  return (
    <ul className="checklist" data-testid="checklist">
      {items.map((item) => (
        <li key={item.key} className="checklist-item" data-ok={item.ok}>
          <span className="checklist-icon" aria-hidden="true">
            {item.ok ? <Icons.check /> : <Icons.alert />}
          </span>
          <div className="checklist-text">
            <strong>
              {item.title}
              <span className="sr-only">{item.ok ? "، مكتمل" : "، غير مكتمل"}</span>
            </strong>
            {item.detail ? <span>{item.detail}</span> : null}
            {item.action ? <div className="checklist-action">{item.action}</div> : null}
          </div>
        </li>
      ))}
    </ul>
  );
}

// --------------------------------------------------------------------------------------
// Previews
// --------------------------------------------------------------------------------------

/** A notification as a phone shows it on the lock screen: app, time, title, body. */
export function NotificationPreview({ title, body }: { title: string; body: string }) {
  return (
    <figure className="notification-preview" data-testid="notification-preview">
      <div className="phone-frame">
        <div className="phone-status" aria-hidden="true">
          <span>٩:٤١</span>
        </div>
        <div className="notification-card">
          <div className="notification-head">
            <BrandMark />
            <span>دليني</span>
            <span aria-hidden="true">·</span>
            <span>الآن</span>
          </div>
          <strong className="notification-title" data-empty={!title.trim() || undefined}>
            {title.trim() || "عنوان الإشعار"}
          </strong>
          <p className="notification-body" data-empty={!body.trim() || undefined}>
            {body.trim() || "يظهر نص الإشعار هنا."}
          </p>
        </div>
      </div>
      <figcaption>هكذا يظهر الإشعار على الهاتف.</figcaption>
    </figure>
  );
}

/**
 * One advertisement slide as the app's home slider draws it: a sixteen-by-nine image with
 * rounded corners and the title over its lower edge.
 *
 * `src` is the image to show; `fallback` is tried when it fails (the uploaded file itself,
 * when the public media origin is not reachable from this console). Without either, the
 * frame says why it is empty instead of showing a broken image.
 */
export function SlidePreview({
  src,
  fallback,
  title,
  emptyLabel,
  busy,
}: {
  src: string | null;
  fallback?: string | null;
  title: string;
  emptyLabel: string;
  busy?: boolean;
}) {
  return (
    <figure className="slide-preview" data-testid="slide-preview">
      <div className="slide-frame" aria-busy={busy || undefined}>
        {src ? (
          <SlideImage key={src} src={src} fallback={fallback ?? null} />
        ) : (
          <div className="slide-placeholder">
            <Icons.image />
            <span>{emptyLabel}</span>
          </div>
        )}
        {title.trim() ? <figcaption className="slide-caption">{title.trim()}</figcaption> : null}
        {busy ? (
          <div className="slide-busy" role="status">
            <span className="spinner" aria-hidden="true" />
            <span>جارٍ رفع الصورة…</span>
          </div>
        ) : null}
      </div>
      <div className="slide-dots" aria-hidden="true">
        <span data-active="true" />
        <span />
        <span />
      </div>
    </figure>
  );
}

/*
 * Only a scheme that can carry a picture.
 *
 * The address is a preview of a file the operator just chose or a media URL the API answered
 * with, and `src` follows whatever scheme it is handed — including `javascript:`, which runs when
 * the load fails. Neither source is meant to be hostile, but neither is written by this page.
 *
 * The scheme is read by parsing the address rather than by matching its start, because a prefix
 * test is defeated by the things a parser handles for you: leading whitespace, a tab inside the
 * word, a different case. The base is a fixed, unreachable origin so a relative address resolves
 * without this needing to know where it is running.
 */
const PICTURE_PROTOCOLS = new Set(["http:", "https:", "blob:"]);

function pictureOrNothing(value: string): string {
  const address = value.trim();
  try {
    const parsed = new URL(address, "https://admin.invalid/");
    if (PICTURE_PROTOCOLS.has(parsed.protocol)) return address;
    return parsed.protocol === "data:" && /^image\//i.test(parsed.pathname) ? address : "";
  } catch {
    return "";
  }
}

function SlideImage({ src, fallback }: { src: string; fallback: string | null }) {
  const [failed, setFailed] = useState(false);
  const shown = pictureOrNothing(failed && fallback ? fallback : src);
  return (
    // eslint-disable-next-line @next/next/no-img-element -- a preview of an uploaded file or a public media URL; no optimisation route
    <img
      className="slide-image"
      src={shown}
      alt=""
      onError={() => {
        if (!failed) setFailed(true);
      }}
    />
  );
}

/**
 * One record drawn as a card: a person, a place — anything a row is about rather than a
 * figure about it (DECISION-106).
 *
 * **Full width, one per line, opening in place.** A grid of tiles reflows every neighbour
 * when one of them opens, and the fact a card carries most — a name and a number — reads
 * along a line, not down a column.
 *
 * **The actions sit above everything the card holds**, visible without opening it, because
 * the operator who came to do something should not have to open a record to find out
 * whether they can. What opening adds is the detail behind the summary, which is also what
 * keeps a page of a hundred cards cheap: it is not rendered until it is asked for.
 */
export function RecordCard({
  mark,
  title,
  facts,
  badges,
  actions,
  open,
  onToggle,
  openLabel = "التفاصيل",
  closeLabel = "إخفاء التفاصيل",
  muted,
  testId,
  children,
}: {
  /** A letter or two standing in for a picture: the first letters of the name. */
  mark: ReactNode;
  title: ReactNode;
  /** The quiet line under the title; dots between the parts are drawn by the stylesheet. */
  facts?: readonly ReactNode[];
  badges?: ReactNode;
  actions?: ReactNode;
  open?: boolean;
  onToggle?: () => void;
  openLabel?: string;
  closeLabel?: string;
  muted?: boolean;
  testId?: string;
  children?: ReactNode;
}) {
  const bodyId = useId();
  const classes = ["record", open ? "record-open" : "", muted ? "record-muted" : ""]
    .filter(Boolean)
    .join(" ");
  return (
    <li className={classes} data-testid={testId}>
      <div className="record-head">
        <span className="record-mark" aria-hidden="true">
          {mark}
        </span>
        <span className="record-identity">
          <span className="record-name">{title}</span>
          {facts && facts.length > 0 ? (
            <span className="record-sub">
              {facts.map((fact, index) => (
                // The order is fixed by the caller and the parts carry no identity of
                // their own, so the index is the key there is.
                <span key={index}>{fact}</span>
              ))}
            </span>
          ) : null}
        </span>
        {badges ? <span className="record-badges">{badges}</span> : null}
      </div>
      {actions || onToggle ? (
        <div className="record-actions">
          {actions}
          {onToggle ? (
            <button
              type="button"
              className="button-ghost record-more"
              aria-expanded={Boolean(open)}
              aria-controls={bodyId}
              onClick={onToggle}
            >
              {open ? closeLabel : openLabel}
            </button>
          ) : null}
        </div>
      ) : null}
      {open ? (
        <div className="record-body" id={bodyId}>
          {children}
        </div>
      ) : null}
    </li>
  );
}

/** A heading and its content inside an opened card; several sit side by side where there is room. */
export function RecordSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="record-section">
      <h3>{title}</h3>
      {children}
    </section>
  );
}

/**
 * When a session of this account last proved itself, said as what it is.
 *
 * Never «online»: nothing in this system knows whether an app is open. A session writes its
 * heartbeat when its refresh rotates, which is at most once per access-token lifetime, so
 * the truest thing that can be said is «active recently» and, otherwise, when it last was.
 */
export function LastSeen({
  at,
  recent,
}: {
  at: string | null | undefined;
  recent: boolean;
}) {
  if (!at) return <span className="muted">لم يدخل من أي جهاز</span>;
  return (
    <span>
      <span className={recent ? "seen-dot" : "seen-dot seen-off"} aria-hidden="true" />
      {recent ? "نشط الآن" : `آخر ظهور ${relativeTime(at)}`}
    </span>
  );
}

/** «منذ ٣ ساعات», «أمس», «قبل ٥ أيام» — Arabic, and never a bare timestamp in a card. */
export function relativeTime(value: string): string {
  const then = new Date(value).getTime();
  if (Number.isNaN(then)) return "";
  const minutes = Math.max(0, Math.round((Date.now() - then) / 60000));
  if (minutes < 60) return `منذ ${NUMERALS.format(minutes)} دقيقة`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `منذ ${NUMERALS.format(hours)} ساعة`;
  const days = Math.round(hours / 24);
  if (days === 1) return "أمس";
  if (days < 30) return `منذ ${NUMERALS.format(days)} يوم`;
  const months = Math.round(days / 30);
  if (months < 12) return `منذ ${NUMERALS.format(months)} شهر`;
  return `منذ ${NUMERALS.format(Math.round(months / 12))} سنة`;
}

const NUMERALS = new Intl.NumberFormat("ar-SY");
