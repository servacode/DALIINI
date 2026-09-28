"use client";

import { useResource } from "../../../lib/client/use-resource";
import {
  AuditTimeline,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  StatCard,
  StatusBadge,
  type Tone,
} from "../../../components/ui";

type Dashboard = Readonly<{
  pendingReviews: number;
  reverification: number;
  activeUsers: number;
  facilitiesByStatus: readonly { status: string; count: number }[];
  recentActions: readonly {
    action: string;
    targetType: string;
    targetId: string;
    createdAt: string;
  }[];
}>;

const STATUS_LABELS: Record<string, string> = {
  DRAFT: "مسودة",
  SUBMITTED: "قيد المراجعة",
  ACTIVE: "فعّالة",
  SUSPENDED: "موقوفة",
  CLOSED: "مغلقة",
  REVERIFICATION_REQUIRED: "تحتاج إعادة تحقق",
};

const STATUS_TONES: Record<string, Tone> = {
  ACTIVE: "positive",
  SUSPENDED: "warning",
  CLOSED: "danger",
  REVERIFICATION_REQUIRED: "warning",
  SUBMITTED: "info",
};

export default function DashboardPage() {
  const dashboard = useResource<Dashboard>("dashboard");

  return (
    <div className="stack">
      <PageHeader
        title="لوحة المتابعة"
        description="الحالة التشغيلية الحالية وآخر الإجراءات المسجّلة."
      />
      {dashboard.loading ? <LoadingState /> : null}
      {dashboard.error ? <ErrorState error={dashboard.error} onRetry={dashboard.reload} /> : null}
      {dashboard.data ? (
        <>
          <div className="kpi-grid">
            <StatCard
              href="/reviews"
              testId="kpi-pending"
              label="طلبات بانتظار المراجعة"
              value={dashboard.data.pendingReviews}
              icon="inbox"
              tone="info"
              hint="فتح قائمة المراجعات"
            />
            <StatCard
              label="منشآت تحتاج إعادة تحقق"
              value={dashboard.data.reverification}
              icon="shield"
              tone="warning"
            />
            <StatCard
              label="حسابات فعّالة"
              value={dashboard.data.activeUsers}
              icon="userCheck"
            />
          </div>

          <div className="grid-main-aside">
            <Panel title="آخر الإجراءات" description="أحدث العمليات المسجّلة في سجل التدقيق.">
              <AuditTimeline entries={dashboard.data.recentActions} />
            </Panel>

            <Panel title="المنشآت حسب الحالة">
              {dashboard.data.facilitiesByStatus.length === 0 ? (
                <span className="muted">لا منشآت بعد.</span>
              ) : (
                <div>
                  {dashboard.data.facilitiesByStatus.map((row) => (
                    <div key={row.status} className="switch-row">
                      <StatusBadge tone={STATUS_TONES[row.status] ?? "neutral"}>
                        {STATUS_LABELS[row.status] ?? row.status}
                      </StatusBadge>
                      <strong className="tabular">{row.count}</strong>
                    </div>
                  ))}
                </div>
              )}
            </Panel>
          </div>
        </>
      ) : null}
    </div>
  );
}
