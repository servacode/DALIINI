"use client";

import { type VocabularyGroup, term, vocabulary } from "@servacode/design-tokens/vocabulary";
import Link from "next/link";
import { type ReactNode, useEffect, useId, useRef, useState } from "react";

import { type IconName, type IllustrationName, Icons, Illustration } from "../icons";

import {
  type ApiErrorBody,
  fieldErrorsFor,
  messageFor,
  requestIdFor,
} from "../../lib/errors/messages";

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

export function BrandMark() {
  return (
    <span className="brand-mark" aria-hidden="true">
      د
    </span>
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
  return (
    <header className="page-heading">
      <div className="page-heading-text">
        {back ? (
          <Link href={back.href} className="back-link">
            <Icons.arrowBack />
            {back.label}
          </Link>
        ) : null}
        {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
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
}: {
  label: string;
  value: ReactNode;
  icon?: IconName;
  hint?: string;
  href?: string;
  tone?: "warning" | "info";
  testId?: string;
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

export function LoadingState({ label = "جارٍ التحميل…" }: { label?: string }) {
  return (
    <div className="state-block" role="status" aria-live="polite" data-testid="loading-state">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
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

export type Column<T> = Readonly<{
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  /** Identifiers and timestamps read left-to-right even in an RTL table. */
  ltr?: boolean;
  width?: string;
}>;

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  caption,
  empty,
}: {
  columns: readonly Column<T>[];
  rows: readonly T[];
  rowKey: (row: T) => string;
  caption: string;
  empty?: ReactNode;
}) {
  if (rows.length === 0) {
    return <>{empty ?? <EmptyState title="لا توجد نتائج" />}</>;
  }
  return (
    <div className="table-wrap">
      <table className="data-table" data-testid="data-table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col" style={column.width ? { width: column.width } : undefined}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)}>
              {columns.map((column) => (
                <td key={column.key} className={column.ltr ? "cell-ltr" : undefined}>
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

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
}: {
  fields: readonly FilterField[];
  values: Record<string, string>;
  onApply: (next: Record<string, string>) => void;
}) {
  // Seeded once. Every later change to `values` comes from this component's own `onApply`,
  // which sets the draft alongside it, so mirroring the prop in an effect would only add a
  // cascading render.
  const [draft, setDraft] = useState(values);
  const active = Object.values(values).some((value) => value);

  return (
    <form
      className="filter-bar"
      data-testid="filter-bar"
      onSubmit={(event) => {
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
              onChange={(event) =>
                setDraft({ ...draft, [field.name]: event.target.value })
              }
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
              onChange={(event) =>
                setDraft({ ...draft, [field.name]: event.target.value })
              }
            />
          ) : (
            <input
              name={field.name}
              type="search"
              value={draft[field.name] ?? ""}
              placeholder={field.placeholder}
              data-testid={`filter-${field.name}`}
              onChange={(event) =>
                setDraft({ ...draft, [field.name]: event.target.value })
              }
            />
          )}
        </label>
      ))}
      <div className="filter-actions">
        <button type="submit" className="button-primary">
          تطبيق
        </button>
        {active ? (
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
        ) : null}
      </div>
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
}: {
  hasMore: boolean;
  onNext: () => void;
  onFirst: () => void;
  atFirst: boolean;
  loading?: boolean;
}) {
  if (atFirst && !hasMore) return null;
  return (
    <nav className="pagination" aria-label="تنقل الصفحات" data-testid="pagination">
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

export type Tone = "neutral" | "positive" | "warning" | "danger" | "info" | "brand";

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

  useEffect(() => {
    if (!open) return;
    dialog.current?.querySelector<HTMLElement>("[data-autofocus]")?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !pending) onCancel();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, pending, onCancel]);

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
            <span className="diff-before">{format(before?.[key])}</span>
            <span aria-hidden="true">←</span>
            <span className="diff-after">{format(after?.[key])}</span>
          </dd>
        </div>
      ))}
    </dl>
  );
}

function format(value: unknown): string {
  if (value === null || value === undefined) return "—";
  if (typeof value === "boolean") return value ? "نعم" : "لا";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
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
          <strong>{entry.action}</strong>
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
  return new Intl.DateTimeFormat("ar-SY", {
    dateStyle: "short",
    timeStyle: "short",
    timeZone: "Asia/Damascus",
  }).format(parsed);
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
