"use client";

import {
  type CSSProperties,
  type ReactNode,
  useEffect,
  useEffectEvent,
  useId,
  useRef,
  useState,
} from "react";

import { type IconName, Icon, Icons } from "../icons";
import { BrandMark } from "./index";
import { LOCALE } from "../../lib/locale";

/**
 * Shared components added with the operations screens (duty roster, content, broadcast,
 * readiness, analytics).
 *
 * They live beside `index.tsx` rather than inside it only so that concurrent work on the
 * core set merges cleanly; they follow the same rules — tokens only, logical properties,
 * RTL by inheritance, every state visible — and every one is shown on the design page.
 */

const NUMBER = new Intl.NumberFormat(LOCALE);
const PERCENT = new Intl.NumberFormat(LOCALE, { style: "percent", maximumFractionDigits: 1 });

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
          <span>9:41</span>
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
 * A person, or a place, drawn as an identity card (DECISION-106; drawn to the owner's mockup,
 * DECISION-108).
 *
 * **One shape, always, and everything on it.** No part opens, appears or grows for one record
 * and not another: band, picture, name, two tiles, the state, the places, two dates and the
 * actions sit in the same places on every card. A record with nothing to show fills the same
 * frame with quiet words. That is what lets a row of cards be compared at a glance.
 *
 * The band's colour says what kind of record this is before a word is read, the word in its
 * corner names it, and the ring round the picture says whether the record is in use.
 */
export function ProfileCard({
  mark,
  name,
  tag,
  kind = "plain",
  live,
  tiles,
  pills,
  places,
  dates,
  actions,
  focused,
  index = 0,
  cover,
  testId,
}: {
  /** A letter or two standing in for a picture: the first letters of the name. */
  mark: ReactNode;
  name: ReactNode;
  /** The word in the band's corner, and its icon. */
  tag: Readonly<{ label: string; icon: IconName }>;
  kind?: "plain" | "owner" | "blocked";
  /** Rings the picture, and lets it breathe: in use recently. */
  live?: boolean;
  /** Exactly two: what identifies the record beside its name. */
  tiles: readonly [ProfileTile, ProfileTile];
  pills: readonly ProfilePill[];
  places: ProfilePlaces;
  /** Exactly two: when it began, and when it was last seen. */
  dates: readonly [ProfileDate, ProfileDate];
  actions?: ReactNode;
  focused?: boolean;
  /** The card's place in the list, so the cards rise into place one after another. */
  index?: number;
  /** A photograph the band wears instead of its colour: a facility's own picture. */
  cover?: string | null;
  testId?: string;
}) {
  const classes = [
    "profile",
    kind === "owner" ? "profile-owner" : "",
    kind === "blocked" ? "profile-blocked" : "",
    live ? "profile-live" : "",
    focused ? "profile-focus" : "",
    cover ? "profile-cover" : "",
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <li
      className={classes}
      data-testid={testId}
      // Capped, so the twentieth card does not wait a second to appear.
      style={
        {
          "--i": Math.min(index, 12),
          ...(cover ? { "--cover": `url("${cover.replace(/"/g, "%22")}")` } : {}),
        } as CSSProperties
      }
    >
      <div className="profile-band">
        <span className="profile-tag">
          <Icon name={tag.icon} width={14} height={14} />
          {tag.label}
        </span>
      </div>
      <div className="profile-body">
        <span className="profile-mark" aria-hidden="true">
          {mark}
        </span>
        <span className="profile-name">{name}</span>

        <div className="profile-tiles">
          {tiles.map((tile) => (
            <span key={tile.label} className="profile-tile">
              <span className="profile-tile-icon" data-tone={tile.tone} aria-hidden="true">
                <Icon name={tile.icon} width={16} height={16} />
              </span>
              <span className="profile-tile-text">
                <strong dir={tile.ltr ? "ltr" : undefined}>{tile.value}</strong>
                <span title={tile.label}>{tile.label}</span>
              </span>
            </span>
          ))}
        </div>

        <div className="profile-pills">
          {pills.map((pill) => (
            <span key={pill.label} className="profile-pill" data-tone={pill.tone}>
              {pill.icon ? (
                <Icon name={pill.icon} width={14} height={14} />
              ) : (
                <span className="profile-pill-dot" aria-hidden="true" />
              )}
              {pill.label}
            </span>
          ))}
        </div>

        <hr className="profile-rule" />

        <section className="profile-places" aria-label={places.title}>
          <div className="profile-places-head">
            <span className="profile-places-icon" aria-hidden="true">
              <Icon name={places.icon ?? "building"} width={18} height={18} />
            </span>
            <div>
              <strong>{places.title}</strong>
              {/* Cut to one line on the card; the whole of it on hover. */}
              <span title={places.subtitle}>{places.subtitle}</span>
            </div>
          </div>
          {places.items.length === 0 ? (
            <span className="profile-places-empty">
              <Icon name="building" width={18} height={18} />
              {places.empty}
            </span>
          ) : (
            places.items.map((item) => (
              <span key={item.key} className="profile-place">
                <span className="profile-place-thumb" aria-hidden="true">
                  {item.image ? (
                    // eslint-disable-next-line @next/next/no-img-element -- public media, no loader
                    <img src={item.image} alt="" loading="lazy" />
                  ) : (
                    <Icon name={item.icon ?? "building"} width={18} height={18} />
                  )}
                </span>
                <span className="profile-place-text">
                  <strong>{item.label}</strong>
                  {item.aside ? <span>{item.aside}</span> : null}
                </span>
                {item.state ? (
                  <span className="profile-place-state" data-tone={item.stateTone}>
                    {item.state}
                  </span>
                ) : null}
              </span>
            ))
          )}
          {places.more && places.more > 0 ? (
            <span className="profile-places-more">{`و${NUMERALS.format(places.more)} غيرها`}</span>
          ) : null}
        </section>

        <hr className="profile-rule" />

        <div className="profile-dates">
          {dates.map((date) => (
            <span key={date.label} className="profile-date">
              <span className="profile-date-icon" data-tone={date.tone} aria-hidden="true">
                <Icon name={date.icon} width={15} height={15} />
              </span>
              <div>
                <span>{date.label}</span>
                <strong>{date.value}</strong>
              </div>
            </span>
          ))}
        </div>

        {actions ? <div className="profile-actions">{actions}</div> : null}
      </div>
    </li>
  );
}

export type ProfileTile = Readonly<{
  icon: IconName;
  /** `gold` for a place; the brand green otherwise. */
  tone?: "gold";
  value: ReactNode;
  label: string;
  /** A number read left to right, such as a phone number. */
  ltr?: boolean;
}>;

export type ProfilePill = Readonly<{
  label: string;
  tone?: "positive" | "danger" | "warning" | "info";
  /** An icon instead of the dot. */
  icon?: IconName;
}>;

export type ProfilePlaces = Readonly<{
  /** The panel's icon; a building unless said otherwise. */
  icon?: IconName;
  title: string;
  subtitle: string;
  items: readonly ProfilePlace[];
  /** How many more there are than the card shows. */
  more?: number;
  /** What the panel says when there is nothing in it. */
  empty: string;
}>;

export type ProfilePlace = Readonly<{
  key: string;
  label: string;
  /** A word under the name: «مالك», «مدير». */
  aside?: string;
  image?: string | null;
  /** Shown when there is no image. */
  icon?: IconName;
  state?: string;
  /** The vocabulary tone of the state: positive, warning, danger, info. */
  stateTone?: string;
}>;

export type ProfileDate = Readonly<{
  icon: IconName;
  tone?: "info";
  label: string;
  value: string;
}>;

/** «منذ 3 ساعات», «أمس», «منذ 5 أيام» — Arabic, and never a bare timestamp on a card. */
export function relativeTime(value: string): string {
  const then = new Date(value).getTime();
  if (Number.isNaN(then)) return "";
  const minutes = Math.max(0, Math.round((Date.now() - then) / 60000));
  if (minutes < 1) return "الآن";
  if (minutes < 60) return `منذ ${counted(minutes, "دقيقة", "دقيقتين", "دقائق")}`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `منذ ${counted(hours, "ساعة", "ساعتين", "ساعات")}`;
  const days = Math.round(hours / 24);
  if (days === 1) return "أمس";
  if (days < 30) return `منذ ${counted(days, "يوم", "يومين", "أيام", "يوماً")}`;
  const months = Math.round(days / 30);
  if (months < 12) return `منذ ${counted(months, "شهر", "شهرين", "أشهر", "شهراً")}`;
  return `منذ ${counted(Math.round(months / 12), "سنة", "سنتين", "سنوات", "سنة")}`;
}

/** «دقيقة», «دقيقتين», «3 دقائق», «11 دقيقة»: the noun Arabic puts after each count. */
export function counted(n: number, one: string, two: string, few: string, many: string = one): string {
  if (n === 1) return one;
  if (n === 2) return two;
  if (n <= 10) return `${NUMERALS.format(n)} ${few}`;
  return `${NUMERALS.format(n)} ${many}`;
}

const NUMERALS = new Intl.NumberFormat(LOCALE);

// --------------------------------------------------------------------------------------
// Section hero, live toolbar, form dialog (DECISION-107)
// --------------------------------------------------------------------------------------

/** A section's opening: an emerald band in the identity cards' own language. */
export function PageHero({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <header className="page-hero">
      <div className="page-hero-text">
        {eyebrow ? <span className="page-hero-eyebrow">{eyebrow}</span> : null}
        <h1>{title}</h1>
        {description ? <p>{description}</p> : null}
      </div>
      {action}
    </header>
  );
}

/**
 * A search field that applies itself.
 *
 * Typing waits for a pause before it asks the server, so a name is one request rather than
 * one per letter, and clearing it applies at once.
 */
export function SearchBox({
  value,
  onChange,
  placeholder,
  delay = 350,
  testId,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  delay?: number;
  testId?: string;
}) {
  const [draft, setDraft] = useState(value);
  const [seen, setSeen] = useState(value);
  // Follows the address bar when it changes from outside (back button, a shared link),
  // adjusted during render so the box never shows one search while the list shows another.
  if (seen !== value) {
    setSeen(value);
    setDraft(value);
  }
  const apply = useEffectEvent((next: string) => onChange(next));
  useEffect(() => {
    if (draft === value) return;
    const timer = window.setTimeout(() => apply(draft.trim()), delay);
    return () => window.clearTimeout(timer);
  }, [draft, value, delay]);

  return (
    <label className="search-box">
      <Icon name="search" />
      <input
        type="search"
        value={draft}
        placeholder={placeholder}
        aria-label={placeholder}
        data-testid={testId}
        onChange={(event) => setDraft(event.target.value)}
      />
      {draft ? (
        <button
          type="button"
          aria-label="مسح البحث"
          onClick={() => {
            setDraft("");
            onChange("");
          }}
        >
          <Icon name="close" width={16} height={16} />
        </button>
      ) : null}
    </label>
  );
}

/** One choice out of a few, applied the moment it is pressed. */
export function FilterChips<T extends string>({
  label,
  value,
  options,
  onChange,
  testId,
}: {
  label: string;
  value: T;
  options: readonly { value: T; label: string }[];
  onChange: (value: T) => void;
  testId?: string;
}) {
  return (
    <div className="filter-chips" role="group" aria-label={label} data-testid={testId}>
      {options.map((option) => (
        <button
          key={option.value || "all"}
          type="button"
          className="filter-chip"
          aria-pressed={value === option.value}
          data-testid={testId ? `${testId}-${option.value || "all"}` : undefined}
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

/**
 * A short form in a centered window: a branded head, the fields, and the two buttons.
 *
 * Escape and the backdrop close it unless it is sending, and focus starts on the first field.
 */
export function FormDialog({
  open,
  icon,
  title,
  description,
  onClose,
  locked,
  footer,
  children,
  wide,
  testId,
}: {
  open: boolean;
  icon: IconName;
  title: string;
  description?: string;
  onClose: () => void;
  locked?: boolean;
  footer?: ReactNode;
  children: ReactNode;
  /** A longer form: a wider window whose body scrolls. */
  wide?: boolean;
  testId?: string;
}) {
  const headingId = useId();
  const box = useRef<HTMLDivElement>(null);
  const close = useEffectEvent(() => {
    if (!locked) onClose();
  });
  useEffect(() => {
    if (!open) return;
    box.current
      ?.querySelector<HTMLElement>("input, select, textarea, button")
      ?.focus({ preventScroll: true });
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") close();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  if (!open) return null;
  return (
    <div
      className="dialog-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !locked) onClose();
      }}
    >
      <div
        className={wide ? "panel dialog form-dialog form-dialog-wide" : "panel dialog form-dialog"}
        role="dialog"
        aria-modal="true"
        aria-labelledby={headingId}
        ref={box}
        data-testid={testId}
      >
        <div className="form-dialog-head">
          <span className="form-dialog-icon" aria-hidden="true">
            <Icon name={icon} width={22} height={22} />
          </span>
          <span>
            <h2 id={headingId}>{title}</h2>
            {description ? <p>{description}</p> : null}
          </span>
        </div>
        <div className="form-dialog-body">{children}</div>
        {footer ? <div className="form-dialog-foot">{footer}</div> : null}
      </div>
    </div>
  );
}

// --------------------------------------------------------------------------------------
// Item card (DECISION-113)
// --------------------------------------------------------------------------------------

/**
 * Everything the console lists that is not a person or a facility — a role, a category, a
 * province, an advertisement, an emergency number, a page — as a card in the identity cards'
 * language, lighter: a tinted head with the record's icon, name and state; up to three facts;
 * whatever the record needs said about itself; and its actions at the foot.
 *
 * The same rule as the identity card: one shape, always. A fact with no value says so in quiet
 * words rather than leaving a gap, so a row of cards lines up.
 */
export function ItemCard({
  icon,
  glyph,
  title,
  subtitle,
  state,
  tone = "neutral",
  facts,
  children,
  actions,
  cover,
  index = 0,
  muted,
  focused,
  testId,
}: {
  icon: IconName;
  /** Big text in place of the icon: an emergency number, a province's initial. */
  glyph?: ReactNode;
  title: ReactNode;
  subtitle?: ReactNode;
  state?: Readonly<{ label: string; tone?: ItemTone }>;
  /** The head's tint: what kind of record this is, before a word is read. */
  tone?: ItemTone;
  facts?: readonly ItemFact[];
  children?: ReactNode;
  actions?: ReactNode;
  /** A picture across the head: an advertisement's own image. */
  cover?: string | null;
  index?: number;
  /** Switched off: the card steps back. */
  muted?: boolean;
  focused?: boolean;
  testId?: string;
}) {
  // A picture that will not load is dropped, not shown as a broken image.
  const [failed, setFailed] = useState<string | null>(null);
  const shownCover = cover && cover !== failed ? cover : null;
  const classes = ["item-card", muted ? "item-card-muted" : "", focused ? "profile-focus" : ""]
    .filter(Boolean)
    .join(" ");
  return (
    <li
      className={classes}
      data-tone={tone}
      data-testid={testId}
      style={{ "--i": Math.min(index, 12) } as CSSProperties}
    >
      {shownCover ? (
        <div className="item-card-cover">
          {/* eslint-disable-next-line @next/next/no-img-element -- public media, no loader */}
          <img src={shownCover} alt="" loading="lazy" onError={() => setFailed(shownCover)} />
        </div>
      ) : cover !== undefined ? (
        <div className="item-card-cover item-card-cover-empty" aria-hidden="true">
          <Icon name="image" width={28} height={28} />
          <span>لا صورة</span>
        </div>
      ) : null}
      <div className="item-card-head">
        <span className="item-card-icon" aria-hidden={glyph ? undefined : true}>
          {glyph ?? <Icon name={icon} width={22} height={22} />}
        </span>
        <span className="item-card-title">
          <strong>{title}</strong>
          {subtitle ? <span>{subtitle}</span> : null}
        </span>
        {state ? (
          <span className="item-card-state" data-tone={state.tone ?? "neutral"}>
            {state.label}
          </span>
        ) : null}
      </div>
      {facts && facts.length > 0 ? (
        <div className="item-card-facts" data-count={facts.length}>
          {facts.map((fact) => (
            <span key={fact.label} className="item-card-fact">
              <strong dir={fact.ltr ? "ltr" : undefined}>{fact.value}</strong>
              <span>{fact.label}</span>
            </span>
          ))}
        </div>
      ) : null}
      {children ? <div className="item-card-body">{children}</div> : null}
      {actions ? <div className="profile-actions item-card-actions">{actions}</div> : null}
    </li>
  );
}

export type ItemTone = "neutral" | "brand" | "gold" | "info" | "danger" | "positive" | "warning";

export type ItemFact = Readonly<{
  label: string;
  value: ReactNode;
  /** A number or code read left to right. */
  ltr?: boolean;
}>;

/** A share of a whole as a slim bar with its words: «5 من 32 صلاحية». */
export function ItemMeter({ value, total, label }: { value: number; total: number; label: string }) {
  const ratio = total > 0 ? Math.min(1, value / total) : 0;
  return (
    <div className="item-meter">
      <span className="item-meter-track" aria-hidden="true">
        <span style={{ inlineSize: `${Math.round(ratio * 100)}%` }} />
      </span>
      <span className="item-meter-label">{label}</span>
    </div>
  );
}

/** A few words as chips, the rest counted: «المراجعات · المنشآت · و3 غيرها». */
export function ItemChips({ items, max = 4, empty }: { items: readonly string[]; max?: number; empty?: string }) {
  if (items.length === 0) return empty ? <p className="item-card-quiet">{empty}</p> : null;
  const shown = items.slice(0, max);
  const rest = items.length - shown.length;
  return (
    <ul className="item-chips">
      {shown.map((item) => (
        <li key={item}>{item}</li>
      ))}
      {rest > 0 ? <li className="item-chips-more">{`و${NUMBER.format(rest)} غيرها`}</li> : null}
    </ul>
  );
}
