"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { firstAllowedPage, useCan, useIdentity } from "../../../components/admin-shell";
import { BarChart, LineChart } from "../../../components/charts";
import { AlertsPanel, TaskCenter } from "../../../components/smart";
import { addDays, damascusDay } from "../../../lib/client/calendar";
import { useResource } from "../../../lib/client/use-resource";
import { counted } from "../../../components/ui/extra";
import {
  AuditTimeline,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  StatCard,
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

/** A status's tone as a colour a bar can be filled with. */
const TONE_COLORS: Record<Tone, string> = {
  positive: "var(--ad-success)",
  warning: "var(--ad-warning)",
  danger: "var(--ad-danger)",
  info: "var(--ad-info)",
  brand: "var(--ad-brand)",
  accent: "var(--ad-accent)",
  neutral: "var(--ad-stroke-strong)",
};

type Day = Readonly<{ date: string; searches: number; facilityViews: number; newUsers: number }>;

export default function DashboardPage() {
  const { permissions } = useIdentity();
  const canReadDashboard = permissions.includes("admin.dashboard.read");
  const router = useRouter();
  useEffect(() => {
    if (canReadDashboard) return;
    const elsewhere = firstAllowedPage(permissions);
    if (elsewhere && elsewhere !== "/dashboard") router.replace(elsewhere);
  }, [canReadDashboard, permissions, router]);
  const dashboard = useResource<Dashboard>(
    "dashboard",
    {},
    { refreshMs: 60_000, enabled: canReadDashboard },
  );
  // Two weeks of use, for whoever may read the analytics; the rest of the page does not need it.
  const canReadAnalytics = useCan("admin.analytics.read");
  const [fortnight] = useState(() => {
    const today = damascusDay(new Date());
    return { from: addDays(today, -13), to: today };
  });
  const series = useResource<{ days: Day[] }>("analyticsSeries", fortnight, {
    enabled: canReadAnalytics,
  });
  const days = series.data?.days ?? [];

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
              hint={`${newAccounts(dashboard.data.newUsers7d)} خلال 7 أيام`}
            />
          </div>

          <div className="grid-main-aside">
            <TaskCenter />

            <Panel title="المنشآت حسب الحالة" description="كل شريط يفتح قائمته.">
              {dashboard.data.facilitiesByStatus.length === 0 ? (
                <span className="muted">لا منشآت بعد.</span>
              ) : (
                <BarChart
                  caption="المنشآت حسب الحالة"
                  bars={dashboard.data.facilitiesByStatus.map((row) => ({
                    key: row.status,
                    label: STATUS_LABELS[row.status] ?? row.status,
                    value: row.count,
                    color: TONE_COLORS[STATUS_TONES[row.status] ?? "neutral"],
                    href: `/facilities?status=${encodeURIComponent(row.status)}`,
                  }))}
                />
              )}
            </Panel>
          </div>

          {days.length > 0 ? (
            <Panel
              title="آخر أسبوعين"
              description="البحث والمشاهدات والحسابات الجديدة، يوماً بيوم."
              actions={
                <Link className="button-ghost" href="/analytics">
                  التحليلات
                </Link>
              }
            >
              <LineChart
                caption="آخر أسبوعين يوماً بيوم"
                height={200}
                days={days.map((day) => day.date)}
                series={[
                  { key: "searches", label: "بحث", color: "var(--ad-brand)", values: days.map((d) => d.searches) },
                  { key: "views", label: "مشاهدات", color: "var(--ad-info)", values: days.map((d) => d.facilityViews) },
                  { key: "users", label: "حسابات جديدة", color: "var(--ad-accent)", values: days.map((d) => d.newUsers) },
                ]}
              />
            </Panel>
          ) : null}

          <Panel title="آخر الإجراءات" description="أحدث العمليات المسجّلة في سجل التدقيق.">
            <AuditTimeline entries={dashboard.data.recentActions} />
          </Panel>
        </>
      ) : null}
    </div>
  );
}

/** «حساب جديد», «حسابان جديدان», «4 حسابات جديدة», «12 حساباً جديداً»; none is «لا حسابات جديدة». */
function newAccounts(n: number): string {
  if (n === 0) return "لا حسابات جديدة";
  return counted(n, "حساب جديد", "حسابان جديدان", "حسابات جديدة", "حساباً جديداً");
}
