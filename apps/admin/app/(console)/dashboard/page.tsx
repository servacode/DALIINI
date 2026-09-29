"use client";

import { AlertsPanel, TaskCenter } from "../../../components/smart";
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
  termsFor,
  labelsFor,
} from "../../../components/ui";

type Dashboard = Readonly<{
  pendingReviews: number;
  reverification: number;
  activeUsers: number;
  dutyActiveNow: number;
  newUsers7d: number;
  openReports: number;
  systemWarnings: readonly string[];
  facilitiesByStatus: readonly { status: string; count: number }[];
  recentActions: readonly {
    action: string;
    targetType: string;
    targetId: string;
    createdAt: string;
  }[];
}>;

const STATUS_LABELS = labelsFor("facilityStatus");

const STATUS_TONES = Object.fromEntries(
  Object.entries(termsFor("facilityStatus")).map(([key, meta]) => [key, meta.tone]),
) as Record<string, Tone>;

export default function DashboardPage() {
  const dashboard = useResource<Dashboard>("dashboard", {}, { refreshMs: 60_000 });

  return (
    <div className="stack">
      <PageHeader
        title="لوحة المتابعة"
        description="الحالة التشغيلية الحالية وآخر الإجراءات المسجّلة."
      />
      <AlertsPanel />
      {dashboard.loading ? <LoadingState /> : null}
      {dashboard.error ? <ErrorState error={dashboard.error} onRetry={dashboard.reload} /> : null}
      {dashboard.data ? (
        <>
          {dashboard.data.systemWarnings.length > 0 ? (
            <div className="notice" role="status" data-testid="system-warnings">
              <span>
                <strong>تنبيهات الإعداد: </strong>
                {dashboard.data.systemWarnings.join(" · ")}
              </span>
            </div>
          ) : null}
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
              href="/reports"
              label="بلاغات مفتوحة"
              value={dashboard.data.openReports}
              icon="flag"
              tone="warning"
              hint="فتح البلاغات"
            />
            <StatCard
              label="صيدليات مناوبة الآن"
              value={dashboard.data.dutyActiveNow}
              icon="clock"
            />
            <StatCard
              label="حسابات فعّالة"
              value={dashboard.data.activeUsers}
              icon="userCheck"
              hint={`${dashboard.data.newUsers7d} حساباً جديداً خلال ٧ أيام`}
            />
          </div>

          <div className="grid-main-aside">
            <TaskCenter />

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

          <Panel title="آخر الإجراءات" description="أحدث العمليات المسجّلة في سجل التدقيق.">
            <AuditTimeline entries={dashboard.data.recentActions} />
          </Panel>
        </>
      ) : null}
    </div>
  );
}
