"use client";

import Link from "next/link";
import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  FilterBar,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
  formatDateTime,
  termsFor,
  labelsFor,
  Pagination,
} from "../../../components/ui";
import { useMutation } from "../../../lib/client/use-mutation";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useUrlFilters } from "../../../lib/client/use-url-filters";

type Report = Readonly<{
  id: string;
  facilityId: string;
  facilityNameAr: string;
  reporterId: string | null;
  reason: string;
  note: string;
  status: string;
  createdAt: string;
  resolvedById: string | null;
  resolvedAt: string | null;
}>;

export const REASONS = labelsFor("reportReason");

const STATUS = termsFor("reportStatus");

/**
 * Problems the public reported about a listing.
 *
 * This is the loop that keeps the directory honest between verifications: a reader who
 * finds a pharmacy closed, or on duty when it is not, says so, and an operator either
 * fixes the listing (resolve) or records why the report does not hold (dismiss). Both are
 * audited with the note, and neither edits the facility by itself — the fix happens on the
 * facility screen, where the lifecycle rules apply.
 */
export default function ReportsPage() {
  const [filters, setFilters] = useUrlFilters({ status: "OPEN", facility: "" });
  const reports = useCursorPage<Report>("reports", filters);
  const mutation = useMutation();
  const canManage = useCan("admin.reports.manage");

  const [acting, setActing] = useState<{ report: Report; kind: "resolve" | "dismiss" } | null>(
    null,
  );
  /* A decision about everything ticked, rather than about one row. */
  const [bulk, setBulk] = useState<"resolve" | "dismiss" | null>(null);
  const [selected, setSelected] = useState<readonly string[]>([]);
  const [note, setNote] = useState("");
  const [toast, setToast] = useState<string | null>(null);

  const rows = reports.data?.items ?? [];
  /* Only open reports can be decided, so only they can be ticked. */
  const selectable = rows.filter((row) => row.status === "OPEN");
  const chosen = selected.filter((id) => selectable.some((row) => row.id === id));

  function toggle(id: string): void {
    setSelected((current) =>
      current.includes(id) ? current.filter((value) => value !== id) : [...current, id],
    );
  }

  async function submit(): Promise<void> {
    if (!acting) return;
    const resolving = acting.kind === "resolve";
    const ok = await mutation.run(resolving ? "reportResolve" : "reportDismiss", {
      id: acting.report.id,
      note: note.trim(),
    });
    if (!ok) return;
    setActing(null);
    setNote("");
    setToast(resolving ? "تم تعليم البلاغ كمُعالَج." : "تم رفض البلاغ.");
    reports.reload();
  }

  async function submitBulk(): Promise<void> {
    if (!bulk || chosen.length === 0) return;
    // Some may have been decided by somebody else between the list and the button. The
    // backend says so per id rather than failing the batch, and the toast says so too —
    // «حُسم ١٢» when two of fourteen were already closed is the truth, not a rounding.
    const answer = await mutation.runFor<{ decided?: number }>("reportsBulkDecide", {
      ids: chosen,
      action: bulk,
      note: note.trim(),
    });
    if (!answer) return;
    const decided = answer.decided ?? chosen.length;
    const skipped = chosen.length - decided;
    setBulk(null);
    setNote("");
    setSelected([]);
    setToast(
      skipped > 0
        ? `حُسم ${decided} بلاغًا، و${skipped} لم يعد مفتوحًا.`
        : `حُسم ${decided} بلاغًا.`,
    );
    reports.reload();
  }

  const allChosen = selectable.length > 0 && chosen.length === selectable.length;

  const columns: readonly Column<Report>[] = [
    ...(canManage && selectable.length > 0
      ? [
          {
            key: "select",
            width: "1%",
            header: (
              <input
                type="checkbox"
                aria-label="اختيار كل البلاغات المفتوحة"
                data-testid="select-all"
                checked={allChosen}
                onChange={() => setSelected(allChosen ? [] : selectable.map((row) => row.id))}
              />
            ),
            render: (row: Report) =>
              row.status === "OPEN" ? (
                <input
                  type="checkbox"
                  aria-label={`اختيار بلاغ ${row.facilityNameAr}`}
                  data-testid={`select-${row.id}`}
                  checked={selected.includes(row.id)}
                  onChange={() => toggle(row.id)}
                />
              ) : null,
          } as Column<Report>,
        ]
      : []),
    {
      key: "facility",
      header: "المنشأة",
      render: (row) => <Link href={`/facilities/${row.facilityId}`}>{row.facilityNameAr}</Link>,
    },
    { key: "reason", header: "السبب", render: (row) => REASONS[row.reason] ?? row.reason },
    {
      key: "note",
      header: "الملاحظة",
      render: (row) => (row.note ? row.note : <span className="muted">—</span>),
    },
    {
      key: "status",
      header: "الحالة",
      render: (row) => (
        <StatusBadge tone={STATUS[row.status]?.tone ?? "neutral"}>
          {STATUS[row.status]?.label ?? row.status}
        </StatusBadge>
      ),
    },
    {
      key: "createdAt",
      header: "تاريخ البلاغ",
      ltr: true,
      render: (row) => formatDateTime(row.createdAt),
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) =>
        canManage && row.status === "OPEN" ? (
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              data-testid={`resolve-${row.id}`}
              onClick={() => {
                mutation.reset();
                setActing({ report: row, kind: "resolve" });
              }}
            >
              معالجة
            </button>
            <button
              type="button"
              className="button-ghost"
              data-tone="danger"
              data-testid={`dismiss-${row.id}`}
              onClick={() => {
                mutation.reset();
                setActing({ report: row, kind: "dismiss" });
              }}
            >
              رفض
            </button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="البلاغات"
        description="ما أبلغ عنه المستخدمون من أخطاء في بيانات المنشآت."
      />
      <FilterBar
        fields={[
          {
            name: "status",
            label: "الحالة",
            type: "select",
            options: Object.entries(STATUS).map(([value, meta]) => ({
              value,
              label: meta.label,
            })),
          },
        ]}
        values={filters}
        onApply={setFilters}
      />
      {chosen.length > 0 ? (
        <div className="bulk-bar" data-testid="bulk-bar">
          <span>
            <strong>{chosen.length}</strong> بلاغًا مختارًا
          </span>
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              data-testid="bulk-resolve"
              onClick={() => {
                mutation.reset();
                setBulk("resolve");
              }}
            >
              معالجة المختار
            </button>
            <button
              type="button"
              className="button-ghost"
              data-tone="danger"
              data-testid="bulk-dismiss"
              onClick={() => {
                mutation.reset();
                setBulk("dismiss");
              }}
            >
              رفض المختار
            </button>
            <button type="button" className="button-ghost" onClick={() => setSelected([])}>
              إلغاء الاختيار
            </button>
          </div>
        </div>
      ) : null}
      {reports.loading ? <LoadingState /> : null}
      {reports.error ? <ErrorState error={reports.error} onRetry={reports.reload} /> : null}
      {reports.data ? (
        <DataTable
          caption="البلاغات"
          columns={columns}
          rows={reports.data.items}
          rowKey={(row) => row.id}
          empty={
            <EmptyState
              title="لا بلاغات"
              hint={filters.status === "OPEN" ? "لا يوجد ما ينتظر المعالجة." : undefined}
            />
          }
        />
      ) : null}
      {reports.pagination ? <Pagination {...reports.pagination} /> : null}

      <ConfirmDialog
        open={acting !== null}
        title={acting?.kind === "resolve" ? "معالجة البلاغ" : "رفض البلاغ"}
        body={
          acting?.kind === "resolve"
            ? "علّم البلاغ كمُعالَج بعد تصحيح بيانات المنشأة من صفحتها."
            : "يُغلق البلاغ دون تغيير. اكتب سبب الرفض ليبقى في سجل التدقيق."
        }
        confirmLabel={acting?.kind === "resolve" ? "تأكيد المعالجة" : "تأكيد الرفض"}
        destructive={acting?.kind === "dismiss"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setActing(null)}
      >
        <label className="field">
          <span>ملاحظة</span>
          <textarea
            rows={3}
            value={note}
            data-testid="report-note"
            onChange={(event) => setNote(event.target.value)}
          />
        </label>
      </ConfirmDialog>

      <ConfirmDialog
        open={bulk !== null}
        title={bulk === "resolve" ? "معالجة المختار" : "رفض المختار"}
        body={
          bulk === "resolve"
            ? `سيُعلَّم ${chosen.length} بلاغًا كمُعالَج. الملاحظة نفسها تُسجَّل على كلٍّ منها.`
            : `سيُغلق ${chosen.length} بلاغًا دون تغيير. اكتب السبب ليبقى في سجل التدقيق.`
        }
        confirmLabel={bulk === "resolve" ? "تأكيد المعالجة" : "تأكيد الرفض"}
        destructive={bulk === "dismiss"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submitBulk}
        onCancel={() => setBulk(null)}
      >
        <label className="field">
          <span>ملاحظة</span>
          <textarea
            rows={3}
            value={note}
            data-testid="bulk-note"
            onChange={(event) => setNote(event.target.value)}
          />
        </label>
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
