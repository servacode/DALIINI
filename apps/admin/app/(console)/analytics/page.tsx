"use client";

import {
  type Column,
  DataTable,
  ErrorState,
  LoadingState,
  PageHeader,
  StatCard,
  Panel,
} from "../../../components/ui";
import { useResource } from "../../../lib/client/use-resource";

type Analytics = Readonly<{
  activeFacilities: number;
  pendingReviews: number;
  ratingAverage: number | null;
  events: readonly { name: string; count: number }[];
}>;

/**
 * Only the metrics the contract already exposes.
 *
 * Nothing is derived, combined or invented here, and nothing personal is shown: no
 * coordinates, no phone numbers, no account identifiers. The event counts are aggregate.
 */
export default function AnalyticsPage() {
  const analytics = useResource<Analytics>("analytics");

  const columns: readonly Column<{ name: string; count: number }>[] = [
    { key: "name", header: "الحدث", ltr: true, render: (row) => <code>{row.name}</code> },
    { key: "count", header: "العدد", ltr: true, render: (row) => row.count },
  ];

  return (
    <div className="stack">
      <PageHeader title="التحليلات" description="مؤشرات تشغيلية مجمّعة." />
      {analytics.loading ? <LoadingState /> : null}
      {analytics.error ? (
        <ErrorState error={analytics.error} onRetry={analytics.reload} />
      ) : null}
      {analytics.data ? (
        <>
          <div className="kpi-grid">
            <StatCard
              label="منشآت فعّالة"
              value={analytics.data.activeFacilities}
              icon="building"
            />
            <StatCard
              label="طلبات بانتظار المراجعة"
              value={analytics.data.pendingReviews}
              icon="inbox"
              tone="info"
            />
            <StatCard
              label="متوسط التقييم"
              value={
                analytics.data.ratingAverage === null
                  ? "—"
                  : analytics.data.ratingAverage.toFixed(2)
              }
              icon="chart"
              tone="warning"
            />
          </div>
          <Panel title="أحداث المنتج" flush>
            <DataTable
              caption="أحداث المنتج"
              columns={columns}
              rows={analytics.data.events}
              rowKey={(row) => row.name}
            />
          </Panel>
        </>
      ) : null}
    </div>
  );
}
