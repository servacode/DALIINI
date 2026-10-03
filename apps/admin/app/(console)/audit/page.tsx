"use client";

import { useState } from "react";

import { ExportButton } from "../../../components/export-button";
import {
  DiffViewer,
  EmptyState,
  ErrorState,
  FilterBar,
  LoadingState,
  PageHeader,
  Pagination,
  Panel,
  formatDateTime,
} from "../../../components/ui";
import { useCursorPage } from "../../../lib/client/use-cursor-page";

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

/**
 * The audit trail, with its filters and the before/after of each change.
 *
 * Snapshots are redacted at write time by `audit/services.py`, so a value reading
 * `[REDACTED]` is the redaction working. Nothing is un-redacted here, and nothing is
 * reconstructed from elsewhere.
 *
 * `requestId` is shown because it is the join between this row and a server log line — and
 * because it is what an operator quotes when reporting something that went wrong.
 */
export default function AuditPage() {
  const [filters, setFilters] = useState<Record<string, string>>({
    action: "",
    resource: "",
    requestId: "",
  });
  const audit = useCursorPage<AuditRow>("audit", filters);
  const [expanded, setExpanded] = useState<string | null>(null);

  return (
    <div className="stack">
      <PageHeader
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
      <FilterBar
        fields={[
          { name: "action", label: "الإجراء", placeholder: "facility.suspended" },
          { name: "resource", label: "العنصر", placeholder: "النوع أو المعرّف" },
          { name: "actor", label: "المنفّذ", placeholder: "معرّف المستخدم" },
          { name: "requestId", label: "معرّف الطلب", placeholder: "UUID" },
          { name: "from", label: "من تاريخ", type: "date" },
          { name: "to", label: "إلى تاريخ", type: "date" },
        ]}
        values={filters}
        onApply={setFilters}
      />

      {audit.loading ? <LoadingState /> : null}
      {audit.error ? <ErrorState error={audit.error} onRetry={audit.reload} /> : null}

      {audit.data ? (
        audit.data.items.length === 0 ? (
          <EmptyState title="لا نتائج مطابقة" />
        ) : (
          <div className="table-wrap">
            <table className="data-table" data-testid="data-table">
              <caption className="sr-only">سجل التدقيق</caption>
              <thead>
                <tr>
                  <th scope="col">الوقت</th>
                  <th scope="col">الإجراء</th>
                  <th scope="col">العنصر</th>
                  <th scope="col">معرّف الطلب</th>
                  <th scope="col" />
                </tr>
              </thead>
              <tbody>
                {audit.data.items.map((row) => (
                  <tr key={row.id} data-selected={expanded === row.id || undefined}>
                    <td className="cell-ltr">{formatDateTime(row.createdAt)}</td>
                    <td>
                      <code className="cell-ltr">{row.action}</code>
                    </td>
                    <td>
                      {row.targetType}
                      <br />
                      <span className="muted cell-ltr">{row.targetId}</span>
                    </td>
                    <td className="cell-ltr">
                      <code>{row.requestId || "—"}</code>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="button-ghost"
                        data-testid={`audit-expand-${row.id}`}
                        onClick={() => setExpanded(expanded === row.id ? null : row.id)}
                      >
                        {expanded === row.id ? "إخفاء" : "التفاصيل"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}
      {audit.pagination ? <Pagination {...audit.pagination} /> : null}

      {expanded && audit.data ? (
        <Panel title="تفاصيل التغيير">
          {(() => {
            const row = audit.data.items.find((item) => item.id === expanded);
            if (!row) return null;
            return (
              <>
                <DiffViewer
                  before={(row.metadata.before as Record<string, unknown>) ?? {}}
                  after={(row.metadata.after as Record<string, unknown>) ?? row.metadata}
                />
                <p className="muted">
                  اللقطات مُنقّحة قبل التخزين. القيمة <code>[REDACTED]</code> تعني أن الحقل
                  حسّاس ولم يُحفظ أصلاً.
                </p>
              </>
            );
          })()}
        </Panel>
      ) : null}
    </div>
  );
}
