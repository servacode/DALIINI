"use client";

import Link from "next/link";
import { useResource } from "../../../lib/client/use-resource";
import {
  AuditTimeline,
  ErrorState,
  LoadingState,
  PageHeader,
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
            <Link href="/reviews" className="kpi" data-testid="kpi-pending">
              <strong>{dashboard.data.pendingReviews}</strong>
              <span>طلبات بانتظار المراجعة</span>
            </Link>
            <div className="kpi">
              <strong>{dashboard.data.reverification}</strong>
              <span>منشآت تحتاج إعادة تحقق</span>
            </div>
            <div className="kpi">
              <strong>{dashboard.data.activeUsers}</strong>
              <span>حسابات فعّالة</span>
            </div>
          </div>

          <section className="panel stack">
            <h2>المنشآت حسب الحالة</h2>
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
          </section>

          <section className="panel stack">
            <h2>آخر الإجراءات</h2>
            <AuditTimeline entries={dashboard.data.recentActions} />
          </section>
        </>
      ) : null}
    </div>
  );
}
