"use client";

import { useResource } from "../../../lib/client/use-resource";
import {
  AuditTimeline,
  Card,
  ErrorState,
  LoadingState,
  PageHeader,
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
    <div className="operation-stack">
      <PageHeader
        eyebrow="العمليات"
        title="لوحة المتابعة"
        description="الحالة التشغيلية الحالية وآخر الإجراءات المسجّلة."
      />
      {dashboard.loading ? <LoadingState /> : null}
      {dashboard.error ? <ErrorState error={dashboard.error} onRetry={dashboard.reload} /> : null}
      {dashboard.data ? (
        <>
          <div className="kpi-grid">
            <StatCard
              label="طلبات بانتظار المراجعة"
              value={dashboard.data.pendingReviews}
              href="/reviews"
              testId="kpi-pending"
            />
            <StatCard label="منشآت تحتاج إعادة تحقق" value={dashboard.data.reverification} />
            <StatCard label="حسابات فعّالة" value={dashboard.data.activeUsers} />
          </div>

          <Card
            title="المنشآت حسب الحالة"
            description="كل منشأة في الدليل، حسب المرحلة التي هي فيها الآن."
          >
            <div className="button-row">
              {dashboard.data.facilitiesByStatus.length === 0 ? (
                <span className="muted">لا منشآت بعد.</span>
              ) : (
                dashboard.data.facilitiesByStatus.map((row) => (
                  <StatusBadge key={row.status} tone={STATUS_TONES[row.status] ?? "neutral"}>
                    {STATUS_LABELS[row.status] ?? row.status} · {row.count}
                  </StatusBadge>
                ))
              )}
            </div>
          </Card>

          <Card title="آخر الإجراءات" description="ما سجّله التدقيق مؤخّرًا، بأحدثها أولًا.">
            <AuditTimeline entries={dashboard.data.recentActions} />
          </Card>
        </>
      ) : null}
    </div>
  );
}
