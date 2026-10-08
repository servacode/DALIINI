"use client";

import {
  type ReactNode,
  useEffect,
  useEffectEvent,
  useId,
  useRef,
  useState,
} from "react";

import { Icon, Icons } from "../icons";
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
 * A person, or a place, drawn as an identity card (DECISION-106).
 *
 * **One shape, always, and everything on it.** There is no button that opens a card and no
 * part that appears for one account and not another: the band, the picture, the name, the
 * three figures, the list and the actions are on every card in the same places. An account
 * with nothing to show fills the same frame with quiet words. That is what lets a row of
 * four be compared at a glance, and it is what the owner asked for in as many words.
 *
 * The band's colour says what kind of record this is before a word is read, and the ring
 * round the picture says whether it is in use. The list holds three lines at most, in a
 * fixed height, and says how many more there are rather than growing.
 */
export function ProfileCard({
  mark,
  name,
  phone,
  meta,
  tag,
  kind = "plain",
  live,
  status,
  stats,
  listTitle,
  items,
  more,
  empty,
  actions,
  focused,
  testId,
}: {
  /** A letter or two standing in for a picture: the first letters of the name. */
  mark: ReactNode;
  name: ReactNode;
  phone?: ReactNode;
  /** One line under the number: a province, a category. */
  meta?: ReactNode;
  /** The word in the band's corner that names what its colour means. */
  tag: string;
  kind?: "plain" | "owner" | "blocked";
  /** Rings the picture: in use recently. */
  live?: boolean;
  /** The pill under the name: «نشط الآن», «آخر ظهور منذ…». */
  status: ReactNode;
  /** Exactly three figures, compared across a row of cards. */
  stats: readonly [ProfileStat, ProfileStat, ProfileStat];
  listTitle: string;
  items: readonly ProfileItem[];
  /** How many more there are than the list shows. */
  more?: number;
  /** What the list says when there is nothing in it. */
  empty: string;
  actions?: ReactNode;
  focused?: boolean;
  testId?: string;
}) {
  const classes = [
    "profile",
    kind === "owner" ? "profile-owner" : "",
    kind === "blocked" ? "profile-blocked" : "",
    live ? "profile-live" : "",
    focused ? "profile-focus" : "",
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <li className={classes} data-testid={testId}>
      <div className="profile-band">
        <span className="profile-tag">{tag}</span>
      </div>
      <span className="profile-mark" aria-hidden="true">
        {mark}
      </span>
      <div className="profile-id">
        <span className="profile-name">{name}</span>
        {phone ? <span className="profile-phone">{phone}</span> : null}
        {meta ? (
          <span className="profile-meta">
            <Icon name="mapPin" width={13} height={13} />
            {meta}
          </span>
        ) : null}
        <span className="profile-status">{status}</span>
      </div>
      <div className="profile-stats">
        {stats.map((stat) => (
          <span key={stat.label} className="profile-stat">
            <strong>{stat.value}</strong>
            <span>{stat.label}</span>
          </span>
        ))}
      </div>
      <div className="profile-list">
        <span className="profile-list-title">{listTitle}</span>
        {items.length === 0 ? (
          <span className="profile-empty">
            <Icon name="building" width={14} height={14} />
            {empty}
          </span>
        ) : (
          items.map((item) => (
            <span key={item.key} className="profile-item">
              <span className="profile-item-dot" data-tone={item.tone} title={item.toneLabel} />
              <span className="profile-item-name">{item.label}</span>
              {item.aside ? <span className="profile-item-role">{item.aside}</span> : null}
            </span>
          ))
        )}
        {more && more > 0 ? (
          <span className="profile-item-more">{`و${NUMERALS.format(more)} غيرها`}</span>
        ) : null}
      </div>
      {actions ? <div className="profile-actions">{actions}</div> : null}
    </li>
  );
}

export type ProfileStat = Readonly<{ value: ReactNode; label: string }>;

export type ProfileItem = Readonly<{
  key: string;
  label: string;
  /** A word beside the name: «مالك», «مدير». */
  aside?: string;
  /** The dot's colour, from the status vocabulary's tone. */
  tone?: string;
  /** What the dot means, for whoever hovers it and for a screen reader. */
  toneLabel?: string;
}>;

/** «منذ ٣ ساعات», «أمس», «قبل ٥ أيام» — Arabic, and never a bare timestamp on a card. */
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
