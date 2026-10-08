"use client";

import { useState } from "react";

import {
  type Period,
  PeriodPicker,
  lastDays,
  periodLabel,
} from "../../../../components/period-picker";
import {
  type Column,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  StatCard,
} from "../../../../components/ui";
import { damascusDay } from "../../../../lib/client/calendar";
import { useResource } from "../../../../lib/client/use-resource";
import { LOCALE } from "../../../../lib/locale";

type Member = Readonly<{
  userId: string;
  name: string;
  decisions: number;
  approvals: number;
  rejections: number;
  medianDecisionHours: number | null;
  reportDecisions: number;
}>;

type Performance = Readonly<{ from: string; to: string; items: readonly Member[] }>;

const NUMBER = new Intl.NumberFormat(LOCALE);
const DECIMAL = new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 1 });
const PERCENT = new Intl.NumberFormat(LOCALE, { style: "percent", maximumFractionDigits: 0 });

/** Hours when under two days, days after that: «٥٫٢ س», «٣٫١ يوم». */
function duration(hours: number | null): string {
  if (hours === null) return "—";
  return hours < 48 ? `${DECIMAL.format(hours)} س` : `${DECIMAL.format(hours / 24)} يوم`;
}

/**
 * Who decided what in the period: applications approved and rejected, how long each
 * reviewer's decisions took from submission (the median, so one stuck file does not tell
 * the whole story), and the problem reports they closed.
 *
 * It describes the workload, not a ranking of people; it is sorted by volume only so the
 * busiest queue is read first.
 */
export default function StaffPerformancePage() {
  const [today] = useState(() => damascusDay(new Date()));
  const [period, setPeriod] = useState<Period>(() => lastDays(30, damascusDay(new Date())));
  const performance = useResource<Performance>("staffPerformance", period);

  const items = [...(performance.data?.items ?? [])].sort(
    (a, b) => b.decisions + b.reportDecisions - (a.decisions + a.reportDecisions),
  );
  const totals = items.reduce(
    (sum, item) => ({
      decisions: sum.decisions + item.decisions,
      approvals: sum.approvals + item.approvals,
      reports: sum.reports + item.reportDecisions,
    }),
    { decisions: 0, approvals: 0, reports: 0 },
  );

  const columns: readonly Column<Member>[] = [
    {
      key: "name",
      header: "المراجع",
      sortValue: (row) => row.name,
      render: (row) => <strong>{row.name}</strong>,
    },
    {
      key: "decisions",
      header: "قرارات الطلبات",
      sortValue: (row) => row.decisions,
      sortFirst: "desc",
      render: (row) => <span className="tabular">{NUMBER.format(row.decisions)}</span>,
    },
    {
      key: "approvals",
      header: "قبول",
      sortValue: (row) => row.approvals,
      sortFirst: "desc",
      render: (row) => <span className="tabular">{NUMBER.format(row.approvals)}</span>,
    },
    {
      key: "rejections",
      header: "رفض",
      sortValue: (row) => row.rejections,
      sortFirst: "desc",
      render: (row) => <span className="tabular">{NUMBER.format(row.rejections)}</span>,
    },
    {
      key: "rate",
      header: "نسبة القبول",
      sortValue: (row) => (row.decisions > 0 ? row.approvals / row.decisions : null),
      sortFirst: "desc",
      render: (row) =>
        row.decisions > 0 ? (
          <span className="tabular">{PERCENT.format(row.approvals / row.decisions)}</span>
        ) : (
          <span className="muted">—</span>
        ),
    },
    {
      key: "median",
      header: "الوسيط حتى القرار",
      sortValue: (row) => row.medianDecisionHours,
      render: (row) => <span className="tabular">{duration(row.medianDecisionHours)}</span>,
    },
    {
      key: "reports",
      header: "بلاغات أُغلقت",
      sortValue: (row) => row.reportDecisions,
      sortFirst: "desc",
      render: (row) => <span className="tabular">{NUMBER.format(row.reportDecisions)}</span>,
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="أداء الفريق"
        description="قرارات كل مراجع في الفترة المختارة، والمدة من إرسال الطلب حتى القرار."
      />
      <PeriodPicker value={period} today={today} onChange={setPeriod} />

      {performance.loading ? <LoadingState /> : null}
      {performance.error ? (
        <ErrorState error={performance.error} onRetry={performance.reload} />
      ) : null}
      {performance.data ? (
        <>
          <div className="kpi-grid">
            <StatCard label="قرارات الطلبات" value={NUMBER.format(totals.decisions)} icon="inbox" />
            <StatCard
              label="نسبة القبول"
              value={totals.decisions > 0 ? PERCENT.format(totals.approvals / totals.decisions) : "—"}
              icon="checkCircle"
              tone="info"
            />
            <StatCard label="بلاغات أُغلقت" value={NUMBER.format(totals.reports)} icon="flag" tone="warning" />
            <StatCard label="مراجعون عملوا في الفترة" value={NUMBER.format(items.length)} icon="users" />
          </div>
          <Panel
            title={`الفترة ${periodLabel({ from: period.from, to: period.to })}`}
            description="الأكثر قرارات أولاً."
            flush
            testId="staff-table"
          >
            <DataTable
              caption="أداء الفريق"
              columns={columns}
              rows={items}
              rowKey={(row) => row.userId}
              empty={
                <EmptyState
                  title="لا قرارات في هذه الفترة"
                  hint="اختر فترة أطول، أو راجع قائمة الطلبات."
                />
              }
            />
          </Panel>
        </>
      ) : null}
    </div>
  );
}
