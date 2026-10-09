"use client";

import { useState } from "react";

import { ExportButton } from "../../../components/export-button";
import {
  type Column,
  DataTable,
  DiffViewer,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  formatDateTime,
} from "../../../components/ui";
import {
  FilterChips,
  FormDialog,
  SearchBox,
  relativeTime,
} from "../../../components/ui/extra";
import { ACTION_OPTIONS, actionLabel, targetLabel } from "../../../lib/client/audit-terms";
import { addDays, damascusDay } from "../../../lib/client/calendar";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useUrlFilters } from "../../../lib/client/use-url-filters";

type AuditRow = Readonly<{
  id: string;
  actorId: string | null;
  action: string;
  targetType: string;
  targetId: string;
  requestId: string;
  metadata: Record<string, unknown>;
  createdAt: string;
}>;

const SINCE = [
  { value: "", label: "أي وقت" },
  { value: "1", label: "اليوم" },
  { value: "7", label: "7 أيام" },
  { value: "30", label: "30 يوماً" },
] as const;
type Since = (typeof SINCE)[number]["value"];

/** A long id cut to what a person reads aloud; the whole of it on hover and in the details. */
function short(id: string): string {
  return id.length > 12 ? `${id.slice(0, 8)}…` : id;
}

/**
 * The audit trail, in words, with its filters and the before/after of each change.
 *
 * Each action reads as a sentence («أُوقفت منشأة») with its stable code kept beside it, small,
 * because the code is what an export, a filter and a support request quote (DECISION-113).
 *
 * Snapshots are redacted at write time by `audit/services.py`, so a value reading
 * `[REDACTED]` is the redaction working. Nothing is un-redacted here, and nothing is
 * reconstructed from elsewhere.
 *
 * `requestId` is shown because it is the join between this row and a server log line — and
 * because it is what an operator quotes when reporting something that went wrong.
 */
export default function AuditPage() {
  // In the address bar, so "this facility's history" or "this request" is a link.
  const [filters, setFilters] = useUrlFilters({
    action: "",
    resource: "",
    actor: "",
    requestId: "",
    from: "",
    to: "",
  });
  const [today] = useState(() => damascusDay(new Date()));
  const audit = useCursorPage<AuditRow>("audit", filters);
  const [open, setOpen] = useState<AuditRow | null>(null);

  const since: Since = (["1", "7", "30"] as const).find(
    (days) => filters.from === addDays(today, -(Number(days) - 1)),
  ) ?? "";

  const columns: readonly Column<AuditRow>[] = [
    {
      key: "when",
      header: "الوقت",
      required: true,
      render: (row) => (
        <span className="cell-stack" title={formatDateTime(row.createdAt)}>
          <strong>{relativeTime(row.createdAt)}</strong>
          <span className="muted cell-ltr">{formatDateTime(row.createdAt)}</span>
        </span>
      ),
    },
    {
      key: "action",
      header: "الإجراء",
      required: true,
      render: (row) => (
        <span className="cell-stack">
          <strong>{actionLabel(row.action)}</strong>
          <code className="audit-code" dir="ltr">
            {row.action}
          </code>
        </span>
      ),
    },
    {
      key: "target",
      header: "العنصر",
      render: (row) => (
        <span className="cell-stack">
          <span>{targetLabel(row.targetType)}</span>
          <span className="muted cell-ltr" title={row.targetId}>
            {short(row.targetId)}
          </span>
        </span>
      ),
    },
    {
      key: "request",
      header: "معرّف الطلب",
      render: (row) =>
        row.requestId ? (
          <code className="audit-code" dir="ltr" title={row.requestId}>
            {short(row.requestId)}
          </code>
        ) : (
          <span className="muted">—</span>
        ),
    },
    {
      key: "details",
      header: "",
      width: "1%",
      required: true,
      render: (row) => (
        <button
          type="button"
          className="button-ghost"
          data-testid={`audit-expand-${row.id}`}
          onClick={() => setOpen(row)}
        >
          التفاصيل
        </button>
      ),
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الإعدادات والنظام"
        title="سجل التدقيق"
        description="كل تغيير إداري، ومن نفّذه، ومتى."
        actions={
          <ExportButton
            name="audit"
            params={filters}
            label="تصدير النتائج إلى إكسل"
            testId="export-audit"
          />
        }
      />

      <div className="live-toolbar">
        <SearchBox
          value={filters.resource}
          placeholder="ابحث بمعرّف العنصر"
          testId="audit-resource"
          onChange={(resource) => setFilters({ ...filters, resource })}
        />
        <select
          className="toolbar-select"
          aria-label="الإجراء"
          value={filters.action}
          data-testid="filter-action"
          onChange={(event) => setFilters({ ...filters, action: event.target.value })}
        >
          <option value="">كل الإجراءات</option>
          {filters.action && !ACTION_OPTIONS.some((o) => o.value === filters.action) ? (
            <option value={filters.action}>{actionLabel(filters.action)}</option>
          ) : null}
          {ACTION_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <FilterChips
          label="الفترة"
          value={since}
          options={SINCE}
          testId="audit-since"
          onChange={(next) =>
            setFilters({
              ...filters,
              from: next ? addDays(today, -(Number(next) - 1)) : "",
              to: "",
            })
          }
        />
      </div>

      {filters.requestId || filters.actor ? (
        <p className="notice">
          {filters.requestId
            ? `تعرض العمليات التي نفّذها الطلب ${short(filters.requestId)} فقط.`
            : "تعرض عمليات مشغّل واحد فقط."}{" "}
          <button
            type="button"
            className="button-link"
            onClick={() => setFilters({ ...filters, requestId: "", actor: "" })}
          >
            عرض الكل
          </button>
        </p>
      ) : null}

      {audit.loading ? <LoadingState /> : null}
      {audit.error ? <ErrorState error={audit.error} onRetry={audit.reload} /> : null}

      {audit.data ? (
        <DataTable
          id="audit"
          caption="سجل التدقيق"
          columns={columns}
          rows={audit.data.items}
          rowKey={(row) => row.id}
          empty={<EmptyState title="لا نتائج مطابقة" illustration="noResults" />}
        />
      ) : null}
      {audit.pagination ? <Pagination {...audit.pagination} /> : null}

      <FormDialog
        open={open !== null}
        wide
        icon="history"
        title={open ? actionLabel(open.action) : ""}
        description={open ? `${targetLabel(open.targetType)} · ${formatDateTime(open.createdAt)}` : undefined}
        onClose={() => setOpen(null)}
        testId="audit-details"
        footer={
          <button type="button" className="button-ghost" onClick={() => setOpen(null)}>
            إغلاق
          </button>
        }
      >
        {open ? (
          <div className="stack">
            <dl className="audit-facts">
              <div>
                <dt>رمز الإجراء</dt>
                <dd dir="ltr">{open.action}</dd>
              </div>
              <div>
                <dt>معرّف العنصر</dt>
                <dd dir="ltr">{open.targetId}</dd>
              </div>
              <div>
                <dt>معرّف الطلب</dt>
                <dd dir="ltr">{open.requestId || "—"}</dd>
              </div>
              <div>
                <dt>المنفّذ</dt>
                <dd dir="ltr">{open.actorId ?? "النظام"}</dd>
              </div>
            </dl>
            <DiffViewer
              before={(open.metadata.before as Record<string, unknown>) ?? {}}
              after={(open.metadata.after as Record<string, unknown>) ?? open.metadata}
            />
            <p className="muted">
              اللقطات مُنقّحة قبل التخزين. القيمة <code>[REDACTED]</code> تعني أن الحقل حسّاس ولم
              يُحفظ أصلاً.
            </p>
          </div>
        ) : null}
      </FormDialog>
    </div>
  );
}
