"use client";

import { type VocabularyGroup, term, vocabulary } from "@servacode/design-tokens/vocabulary";
import brandSymbol from "@servacode/design-tokens/brand/symbol-128.webp";
import Link from "next/link";
import { type ReactNode, useEffect, useEffectEvent, useId, useRef, useState } from "react";

import { type IconName, type IllustrationName, Icons, Illustration } from "../icons";
import { PAGE_SIZES } from "../../lib/client/use-cursor-page";

import {
  type ApiErrorBody,
  fieldErrorsFor,
  messageFor,
  requestIdFor,
} from "../../lib/errors/messages";
import { actionLabel } from "../../lib/client/audit-terms";
import { LOCALE, withoutDirectionMarks } from "../../lib/locale";

/**
 * The shared operational components.
 *
 * Built once here so thirteen screens do not each invent a table, an empty state and a
 * confirm dialog that behave slightly differently. Everything is RTL by inheritance from
 * `<html dir="rtl">` and uses logical CSS properties, so nothing needs mirroring. Colour,
 * spacing and radius come from the design tokens; there are no literal colours.
 *
 * The density is deliberate. This is a console an operator reads all day, not a marketing
 * page: tables are tight, filters sit directly above the data they filter, and a
 * destructive action looks different from a safe one.
 */

// --------------------------------------------------------------------------------------
// Page furniture
// --------------------------------------------------------------------------------------

/** The approved brand symbol (the road and the pin inside the letter); the name is set beside it as text. */
export function BrandMark() {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- a 128px static asset; no optimisation route needed
    <img className="brand-mark" src={brandSymbol.src} width={40} height={40} alt="" aria-hidden="true" />
  );
}

/**
 * The title block every screen opens with.
 *
 * `back` is for detail screens: it returns to the list the record came from, so the way
 * out sits where the eye starts reading (the top inline-start corner) rather than in the
 * browser chrome.
 */
export function PageHeader({
  title,
  description,
  actions,
  eyebrow,
  back,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
  eyebrow?: string;
  back?: { href: string; label: string };
}) {
  // Every section opens on the same emerald band the accounts and facilities do (DECISION-110),
  // in a compact form: one component, so a section that has not been rebuilt yet still looks
  // like part of the same console.
  return (
    <header className="page-hero page-hero-compact page-heading">
      <div className="page-hero-text page-heading-text">
        {back ? (
          <Link href={back.href} className="back-link">
            <Icons.arrowBack />
            {back.label}
          </Link>
        ) : null}
        {eyebrow ? <span className="page-hero-eyebrow">{eyebrow}</span> : null}
        <h1>{title}</h1>
        {description ? <p>{description}</p> : null}
      </div>
      {actions ? <div className="header-actions">{actions}</div> : null}
    </header>
  );
}

/**
 * A titled card. The one container for a block of related content, so every section on
 * every screen has the same header rhythm, padding and edge.
 */
export function Panel({
  title,
  description,
  actions,
  flush,
  children,
  testId,
}: {
  title?: string;
  description?: string;
  actions?: ReactNode;
  /** No inner padding; for a table that should run edge to edge. */
  flush?: boolean;
  children: ReactNode;
  testId?: string;
}) {
  return (
    <section className={flush ? "panel panel-flush" : "panel"} data-testid={testId}>
      {title ? (
        <header className="panel-header">
          <div>
            <h2>{title}</h2>
            {description ? <p>{description}</p> : null}
          </div>
          {actions ? <div className="header-actions">{actions}</div> : null}
        </header>
      ) : null}
      <div className="panel-body">{children}</div>
    </section>
  );
}

/** A headline number. Linked when there is a queue behind it to go and work. */
export function StatCard({
  label,
  value,
  icon,
  hint,
  href,
  tone,
  testId,
  trend,
}: {
  label: string;
  value: ReactNode;
  icon?: IconName;
  hint?: string;
  href?: string;
  tone?: "warning" | "info";
  testId?: string;
  /** Change against a previous period, under the number (see `Trend` in `./extra`). */
  trend?: ReactNode;
}) {
  const Glyph = icon ? Icons[icon] : null;
  const body = (
    <>
      <div className="kpi-head">
        <span className="kpi-label">{label}</span>
        {Glyph ? (
          <span className="kpi-icon">
            <Glyph />
          </span>
        ) : null}
      </div>
      <strong className="kpi-value">{value}</strong>
      {trend ? <span className="kpi-trend">{trend}</span> : null}
      {hint ? <span className="kpi-hint">{hint}</span> : null}
    </>
  );
  return href ? (
    <Link href={href} className="kpi" data-tone={tone} data-testid={testId}>
      {body}
    </Link>
  ) : (
    <div className="kpi" data-tone={tone} data-testid={testId}>
      {body}
    </div>
  );
}

/** Label/value pairs for a record's read-only facts. */
export function KeyValueList({
  items,
}: {
  items: readonly { label: string; value: ReactNode; ltr?: boolean }[];
}) {
  return (
    <dl className="kv">
      {items.map((item) => (
        <div key={item.label} className="kv-row">
          <dt>{item.label}</dt>
          <dd className={item.ltr ? "cell-ltr" : undefined}>{item.value}</dd>
        </div>
      ))}
    </dl>
  );
}

/**
 * The console's one loading state: the platform's mark in the middle, a ring turning round it
 * (DECISION-108).
 *
 * One component for all seventy-odd places that wait, so waiting looks the same everywhere and
 * says whose console this is. `page` is the larger form a whole section shows while it loads.
 * For anyone who has asked their system for less motion the ring stands still and the words
 * carry the meaning.
 */
export function LoadingState({
  label = "جارٍ التحميل…",
  page,
}: {
  label?: string;
  page?: boolean;
}) {
  return (
    <div
      className={page ? "brand-loader brand-loader-page" : "brand-loader"}
      role="status"
      aria-live="polite"
      data-testid="loading-state"
    >
      <span className="brand-loader-ring" aria-hidden="true">
        {/* eslint-disable-next-line @next/next/no-img-element -- a 128px static asset */}
        <img src={brandSymbol.src} width={128} height={128} alt="" />
      </span>
      <span className="brand-loader-label">{label}</span>
    </div>
  );
}

export function EmptyState({
  title,
  hint,
  illustration = "empty",
  action,
}: {
  title: string;
  hint?: string;
  illustration?: IllustrationName;
  action?: ReactNode;
}) {
  return (
    <div className="state-block state-empty" data-testid="empty-state">
      <Illustration name={illustration} size={88} />
      <strong>{title}</strong>
      {hint ? <span className="muted">{hint}</span> : null}
      {action ? <div className="state-action">{action}</div> : null}
    </div>
  );
}

/**
 * A failure, with the correlation id beside it.
 *
 * The id is the whole point of showing anything technical: it is the one string that lets
 * an operator's report be matched to a server log line, and it discloses nothing by itself.
 */
export function ErrorState({
  error,
  onRetry,
}: {
  error: ApiErrorBody | null;
  onRetry?: () => void;
}) {
  const requestId = requestIdFor(error);
  return (
    <div className="state-block state-error" role="alert" data-testid="error-state">
      <strong>{messageFor(error)}</strong>
      {requestId ? (
        <span className="muted request-id">
          معرّف الطلب: <code>{requestId}</code>
        </span>
      ) : null}
      {onRetry ? (
        <button type="button" className="button-ghost" onClick={onRetry}>
          إعادة المحاولة
        </button>
      ) : null}
    </div>
  );
}

export function FieldError({ id, message }: { id: string; message?: string }) {
  if (!message) return null;
  return (
    <p className="field-error" id={id}>
      {message}
    </p>
  );
}

// --------------------------------------------------------------------------------------
// Data
// --------------------------------------------------------------------------------------

export { type Column, DataTable, compareValues, pageSummary } from "./data-table";

export type FilterField = Readonly<{
  name: string;
  label: string;
  type?: "text" | "select" | "date";
  options?: readonly { value: string; label: string }[];
  placeholder?: string;
}>;

/**
 * The filter row above a table.
 *
 * It owns its own draft state and only lifts values on submit, so a half-typed search term
 * does not fire a request per keystroke. Clearing is a single visible action rather than
 * emptying each box.
 */
export function FilterBar({
  fields,
  values,
  onApply,
  delay = 400,
}: {
  fields: readonly FilterField[];
  values: Record<string, string>;
  onApply: (next: Record<string, string>) => void;
  /** How long typing waits for a pause before it asks the server. */
  delay?: number;
}) {
  // The draft follows `values` when they change from outside (a link to the same list with
  // other filters, the back button), adjusted during render rather than in an effect, so the
  // boxes never show one set of filters while the list shows another.
  const [draft, setDraft] = useState(values);
  const [seen, setSeen] = useState(values);
  if (JSON.stringify(seen) !== JSON.stringify(values)) {
    setSeen(values);
    setDraft(values);
  }
  // Only this bar's own fields: an ordering chosen from a table header is not a filter to clear.
  const active = fields.some((field) => values[field.name]);
  const apply = useEffectEvent((next: Record<string, string>) => onApply(next));

  // Filters apply themselves (DECISION-107): a choice the moment it is made, typing after a
  // pause, so the list never waits for a button the operator has to remember to press. The
  // pause is the only delay, and it exists so a name is one request rather than one per letter.
  const typed = JSON.stringify(
    fields.filter((field) => !field.type).map((field) => draft[field.name] ?? ""),
  );
  const applied = JSON.stringify(
    fields.filter((field) => !field.type).map((field) => values[field.name] ?? ""),
  );
  useEffect(() => {
    if (typed === applied) return;
    const timer = window.setTimeout(() => apply(draft), delay);
    return () => window.clearTimeout(timer);
  }, [typed, applied, draft, delay]);

  const choose = (name: string, value: string) => {
    const next = { ...draft, [name]: value };
    setDraft(next);
    onApply(next);
  };

  return (
    <form
      className="filter-bar"
      data-testid="filter-bar"
      onSubmit={(event) => {
        // Enter in a text box applies at once rather than waiting out the pause.
        event.preventDefault();
        onApply(draft);
      }}
    >
      {fields.map((field) => (
        <label key={field.name} className="filter-field">
          <span>{field.label}</span>
          {field.type === "select" ? (
            <select
              name={field.name}
              value={draft[field.name] ?? ""}
              data-testid={`filter-${field.name}`}
              onChange={(event) => choose(field.name, event.target.value)}
            >
              <option value="">الكل</option>
              {(field.options ?? []).map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          ) : field.type === "date" ? (
            <input
              name={field.name}
              type="date"
              dir="ltr"
              value={draft[field.name] ?? ""}
              data-testid={`filter-${field.name}`}
              onChange={(event) => choose(field.name, event.target.value)}
            />
          ) : (
            <input
              name={field.name}
              type="search"
              value={draft[field.name] ?? ""}
              placeholder={field.placeholder}
              data-testid={`filter-${field.name}`}
              onChange={(event) => setDraft({ ...draft, [field.name]: event.target.value })}
            />
          )}
        </label>
      ))}
      {active ? (
        <div className="filter-actions">
          <button
            type="button"
            className="button-ghost"
            data-testid="filter-clear"
            onClick={() => {
              const cleared = Object.fromEntries(fields.map((field) => [field.name, ""]));
              setDraft(cleared);
              onApply(cleared);
            }}
          >
            مسح
          </button>
        </div>
      ) : null}
    </form>
  );
}

/**
 * Cursor pagination, in the contract's terms.
 *
 * `nextCursor` is opaque and is only ever handed back. There is no page count and no page
 * number, because a cursor API has neither; offering them would be a lie the backend cannot
 * honour.
 */
export function Pagination({
  hasMore,
  onNext,
  onFirst,
  atFirst,
  loading,
  limit,
  onLimit,
}: {
  hasMore: boolean;
  onNext: () => void;
  onFirst: () => void;
  atFirst: boolean;
  loading?: boolean;
  limit?: number;
  onLimit?: (limit: number) => void;
}) {
  const sizes = onLimit && limit !== undefined;
  if (atFirst && !hasMore && !sizes) return null;
  return (
    <nav className="pagination" aria-label="تنقل الصفحات" data-testid="pagination">
      {sizes ? (
        <label className="page-size">
          <span>في الصفحة</span>
          <select
            value={limit}
            data-testid="page-size"
            onChange={(event) => onLimit(Number(event.target.value))}
          >
            {PAGE_SIZES.map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </label>
      ) : null}
      <button
        type="button"
        className="button-ghost"
        onClick={onFirst}
        disabled={atFirst || loading}
        data-testid="page-first"
      >
        الصفحة الأولى
      </button>
      <button
        type="button"
        className="button-ghost"
        onClick={onNext}
        disabled={!hasMore || loading}
        data-testid="page-next"
      >
        التالي
      </button>
    </nav>
  );
}

// --------------------------------------------------------------------------------------
// Status and permissions
// --------------------------------------------------------------------------------------

export type Tone = "neutral" | "positive" | "warning" | "danger" | "info" | "brand" | "accent";

export function StatusBadge({ tone = "neutral", children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className={`badge badge-${tone}`} data-testid="status-badge">
      {children}
    </span>
  );
}

/**
 * A state shown in the platform's shared words: the label and colour come from the design
 * package's vocabulary, so "فعّالة" or "مناوب الآن" reads and looks the same in the console,
 * the app and the site.
 */
/** A vocabulary group as `{ VALUE: { label, tone } }`, for tables, filters and badges. */
export function termsFor(group: VocabularyGroup): Record<string, { label: string; tone: Tone }> {
  return Object.fromEntries(
    Object.entries(vocabulary[group]).map(([key, entry]) => [key, { label: entry.ar, tone: entry.tone }]),
  );
}

/** A vocabulary group as `{ VALUE: label }`. */
export function labelsFor(group: VocabularyGroup): Record<string, string> {
  return Object.fromEntries(Object.entries(vocabulary[group]).map(([key, entry]) => [key, entry.ar]));
}

export function TermBadge({ group, value }: { group: VocabularyGroup; value: string | null | undefined }) {
  const t = term(group, value);
  return <StatusBadge tone={t.tone}>{t.ar}</StatusBadge>;
}

/**
 * Hide what the operator cannot do.
 *
 * This is presentation, not authorization. Every endpoint re-checks, and a permission
 * revoked mid-session shows up as a 403 the screen handles — so a stale gate is a cosmetic
 * problem, never a security one.
 */
export function PermissionGate({
  permissions,
  require: required,
  children,
  fallback = null,
}: {
  permissions: readonly string[];
  require: string;
  children: ReactNode;
  fallback?: ReactNode;
}) {
  return permissions.includes(required) ? <>{children}</> : <>{fallback}</>;
}

// --------------------------------------------------------------------------------------
// Forms and confirmation
// --------------------------------------------------------------------------------------

export function FormSection({
  title,
  description,
  children,
  footer,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <section className="panel form-section">
      <header>
        <h2>{title}</h2>
        {description ? <p className="muted">{description}</p> : null}
      </header>
      <div className="form-grid">{children}</div>
      {footer ? <footer className="form-footer">{footer}</footer> : null}
    </section>
  );
}

/**
 * A modal confirmation for anything irreversible or outward-facing.
 *
 * Focus moves into the dialog on open and Escape closes it. The confirm button carries the
 * destructive styling only when the action is destructive, so "approve" and "close
 * permanently" cannot be mistaken for one another at a glance.
 */
export function ConfirmDialog({
  open,
  title,
  body,
  confirmLabel,
  destructive,
  pending,
  error,
  onConfirm,
  onCancel,
  children,
}: {
  open: boolean;
  title: string;
  body?: string;
  confirmLabel: string;
  destructive?: boolean;
  pending?: boolean;
  error?: ApiErrorBody | null;
  onConfirm: () => void;
  onCancel: () => void;
  children?: ReactNode;
}) {
  const headingId = useId();
  const dialog = useRef<HTMLDivElement>(null);
  // Read when Escape is pressed, so the effect below runs once per opening. With `onCancel`
  // among its dependencies it re-ran on every render of the screen behind the dialog (an
  // inline arrow is a new function each time) and pulled focus out of the field being typed in.
  const escape = useEffectEvent(() => {
    if (!pending) onCancel();
  });

  useEffect(() => {
    if (!open) return;
    // Without scrolling, so a long form opens on its first field rather than on its buttons.
    dialog.current?.querySelector<HTMLElement>("[data-autofocus]")?.focus({ preventScroll: true });
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") escape();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  if (!open) return null;

  return (
    <div className="dialog-backdrop" role="presentation">
      <div
        className="panel dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={headingId}
        ref={dialog}
        data-testid="confirm-dialog"
      >
        <h2 id={headingId}>{title}</h2>
        {body ? <p className="muted">{body}</p> : null}
        {children}
        {error ? <ErrorState error={error} /> : null}
        <div className="dialog-actions">
          <button
            type="button"
            className={destructive ? "button-danger" : "button-primary"}
            onClick={onConfirm}
            disabled={pending}
            aria-busy={pending || undefined}
            data-autofocus
            data-testid="confirm-accept"
          >
            {pending ? "جارٍ التنفيذ…" : confirmLabel}
          </button>
          <button
            type="button"
            className="button-ghost"
            onClick={onCancel}
            disabled={pending}
            data-testid="confirm-cancel"
          >
            إلغاء
          </button>
        </div>
      </div>
    </div>
  );
}

// --------------------------------------------------------------------------------------
// Audit presentation
// --------------------------------------------------------------------------------------

/**
 * Before and after, side by side, showing only the keys that actually moved.
 *
 * Snapshots are redacted before they are stored, so a value reading `[REDACTED]` is the
 * audit trail working rather than a rendering fault.
 */
export function DiffViewer({
  before,
  after,
}: {
  before: Record<string, unknown> | null | undefined;
  after: Record<string, unknown> | null | undefined;
}) {
  const keys = Array.from(
    new Set([...Object.keys(before ?? {}), ...Object.keys(after ?? {})]),
  ).sort();
  const changed = keys.filter(
    (key) => JSON.stringify(before?.[key]) !== JSON.stringify(after?.[key]),
  );
  if (changed.length === 0) {
    return <span className="muted">لا تغييرات مسجّلة.</span>;
  }
  return (
    <dl className="diff" data-testid="diff-viewer">
      {changed.map((key) => (
        <div key={key} className="diff-row">
          <dt>{key}</dt>
          <dd>
            <span className="diff-before" dir={isPoint(before?.[key]) ? "ltr" : undefined}>
              {format(before?.[key])}
            </span>
            <span aria-hidden="true">←</span>
            <span className="diff-after" dir={isPoint(after?.[key]) ? "ltr" : undefined}>
              {format(after?.[key])}
            </span>
          </dd>
        </div>
      ))}
    </dl>
  );
}

function format(value: unknown): string {
  if (value === null || value === undefined) return "—";
  if (typeof value === "boolean") return value ? "نعم" : "لا";
  if (isPoint(value)) return `${value.latitude.toFixed(5)}, ${value.longitude.toFixed(5)}`;
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

/** A map point reads as coordinates, not as the JSON it travels in. */
function isPoint(value: unknown): value is { latitude: number; longitude: number } {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as { latitude?: unknown }).latitude === "number" &&
    typeof (value as { longitude?: unknown }).longitude === "number"
  );
}

export type AuditEntry = Readonly<{
  id?: string;
  action: string;
  actorId?: string | null;
  targetType?: string;
  targetId?: string;
  requestId?: string;
  createdAt: string;
  metadata?: Record<string, unknown>;
}>;

export function AuditTimeline({ entries }: { entries: readonly AuditEntry[] }) {
  if (entries.length === 0) {
    return <EmptyState title="لا سجل تدقيق بعد" />;
  }
  return (
    <ol className="timeline" data-testid="audit-timeline">
      {entries.map((entry, index) => (
        <li key={entry.id ?? `${entry.action}-${index}`}>
          <time dateTime={entry.createdAt} className="cell-ltr">
            {formatDateTime(entry.createdAt)}
          </time>
          <strong title={entry.action}>{actionLabel(entry.action)}</strong>
          {entry.requestId ? (
            <span className="muted request-id cell-ltr">{entry.requestId}</span>
          ) : null}
        </li>
      ))}
    </ol>
  );
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "—";
  return withoutDirectionMarks(
    new Intl.DateTimeFormat(LOCALE, {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: "Asia/Damascus",
    }).format(parsed),
  );
}

// --------------------------------------------------------------------------------------
// Feedback
// --------------------------------------------------------------------------------------

/**
 * A transient confirmation after a mutation.
 *
 * Announced politely so a screen reader reports it without interrupting, and dismissible,
 * because an operator running a queue should not have to wait out an animation.
 */
export function Toast({
  message,
  tone = "positive",
  onDismiss,
}: {
  message: string | null;
  tone?: Tone;
  onDismiss: () => void;
}) {
  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(onDismiss, 5000);
    return () => window.clearTimeout(timer);
  }, [message, onDismiss]);

  if (!message) return null;
  return (
    <div className={`toast toast-${tone}`} role="status" aria-live="polite" data-testid="toast">
      <span>{message}</span>
      <button type="button" className="toast-close" onClick={onDismiss} aria-label="إغلاق">
        ×
      </button>
    </div>
  );
}

/** Turn a failed mutation into per-field messages for a form. */
export { fieldErrorsFor, messageFor };
