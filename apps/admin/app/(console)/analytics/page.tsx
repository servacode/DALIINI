"use client";

import { useState } from "react";

import { useIdentity } from "../../../components/admin-shell";
import { LineChart } from "../../../components/charts";
import { ExportButton } from "../../../components/export-button";
import { type Period, PeriodPicker, lastDays, periodLabel } from "../../../components/period-picker";
import {
  type Column,
  DataTable,
  ErrorState,
  LoadingState,
  PageHeader,
  StatCard,
  Panel,
} from "../../../components/ui";
import { Trend } from "../../../components/ui/extra";
import { addDays, damascusDay } from "../../../lib/client/calendar";
import { useResource } from "../../../lib/client/use-resource";
import { LOCALE } from "../../../lib/locale";

type PeriodKpis = Readonly<{
  from: string;
  to: string;
  approvalMedianHours: number | null;
  searches: number;
  zeroResultSearches: number;
  facilityViews: number;
  directionsRequests: number;
}>;

type Analytics = PeriodKpis &
  Readonly<{
    previous: PeriodKpis;
    activeFacilities: number;
    pendingReviews: number;
    ratingAverage: number | null;
    events: readonly { name: string; count: number }[];
  }>;

type Day = Readonly<{
  date: string;
  searches: number;
  zeroResultSearches: number;
  facilityViews: number;
  directionsRequests: number;
  newUsers: number;
  approvals: number;
  reports: number;
}>;

const NUMBER = new Intl.NumberFormat(LOCALE);
const DECIMAL = new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 1 });

/** A period as the backend echoes it: `to` is the exclusive next midnight. */
function echoed(kpis: PeriodKpis): Period {
  return {
    from: damascusDay(kpis.from),
    to: addDays(damascusDay(kpis.to), -1),
  };
}

const EXPORTS = [
  {
    name: "facilities",
    title: "المنشآت",
    detail: "كل المنشآت بحالتها وتصنيفها ومحافظتها ودرجة اكتمال بياناتها.",
    permission: "admin.facilities.read",
  },
  {
    name: "reports",
    title: "البلاغات",
    detail: "كل بلاغات المستخدمين بأسبابها وحالتها.",
    permission: "admin.reports.read",
  },
  {
    name: "audit",
    title: "سجل العمليات",
    detail: "كل تغيير إداري، ومن نفّذه، ومتى.",
    permission: "admin.audit.read",
  },
] as const;

/**
 * Only the metrics the contract already exposes, for a period the operator chooses.
 *
 * The period-bound numbers are compared with the same length of time just before, as the
 * backend computes it; the arrows say which way each moved and whether that is good. The
 * current totals (active facilities, the review queue, the rating) have no period and so no
 * comparison. Nothing personal is shown: the counts are aggregate.
 */
export default function AnalyticsPage() {
  const [today] = useState(() => damascusDay(new Date()));
  const [period, setPeriod] = useState<Period>(() => lastDays(30, damascusDay(new Date())));
  const analytics = useResource<Analytics>("analyticsPeriod", period);
  const series = useResource<{ days: Day[] }>("analyticsSeries", period);
  const days = series.data?.days ?? [];
  const line = (key: Exclude<keyof Day, "date">) => days.map((day) => day[key]);
  const { permissions } = useIdentity();
  const data = analytics.data;

  const columns: readonly Column<{ name: string; count: number }>[] = [
    {
      key: "name",
      header: "الحدث",
      ltr: true,
      sortValue: (row) => row.name,
      render: (row) => <code>{row.name}</code>,
    },
    {
      key: "count",
      header: "العدد",
      ltr: true,
      sortValue: (row) => row.count,
      sortFirst: "desc",
      render: (row) => NUMBER.format(row.count),
    },
  ];

  const hours = (value: number | null) => (value === null ? "—" : `${DECIMAL.format(value)} س`);
  const exports = EXPORTS.filter((item) => permissions.includes(item.permission));

  return (
    <div className="stack">
      <PageHeader title="التحليلات" description="مؤشرات تشغيلية مجمّعة، لفترة تختارها." />
      <PeriodPicker value={period} today={today} onChange={setPeriod} />

      {analytics.loading ? <LoadingState /> : null}
      {analytics.error ? <ErrorState error={analytics.error} onRetry={analytics.reload} /> : null}
      {data ? (
        <>
          <Panel
            title={`الفترة ${periodLabel(echoed(data))}`}
            description={`مقارنةً بالفترة السابقة المماثلة ${periodLabel(echoed(data.previous))}.`}
            testId="period-kpis"
          >
            <div className="kpi-grid">
              <StatCard
                label="عمليات البحث"
                value={NUMBER.format(data.searches)}
                icon="search"
                trend={<Trend current={data.searches} previous={data.previous.searches} />}
                hint={`${NUMBER.format(data.previous.searches)} في الفترة السابقة`}
              />
              <StatCard
                label="بحث بلا نتائج"
                value={NUMBER.format(data.zeroResultSearches)}
                icon="inbox"
                tone="warning"
                trend={
                  <Trend
                    current={data.zeroResultSearches}
                    previous={data.previous.zeroResultSearches}
                    lowerIsBetter
                  />
                }
                hint={
                  data.searches > 0
                    ? `${NUMBER.format(Math.round((data.zeroResultSearches / data.searches) * 100))}٪ من عمليات البحث`
                    : undefined
                }
              />
              <StatCard
                label="مشاهدات المنشآت"
                value={NUMBER.format(data.facilityViews)}
                icon="eye"
                trend={<Trend current={data.facilityViews} previous={data.previous.facilityViews} />}
                hint={`${NUMBER.format(data.previous.facilityViews)} في الفترة السابقة`}
              />
              <StatCard
                label="طلبات الاتجاهات"
                value={NUMBER.format(data.directionsRequests)}
                icon="directions"
                trend={
                  <Trend
                    current={data.directionsRequests}
                    previous={data.previous.directionsRequests}
                  />
                }
                hint={`${NUMBER.format(data.previous.directionsRequests)} في الفترة السابقة`}
              />
              <StatCard
                label="وسيط زمن الموافقة"
                value={hours(data.approvalMedianHours)}
                icon="clock"
                trend={
                  <Trend
                    current={data.approvalMedianHours}
                    previous={data.previous.approvalMedianHours}
                    lowerIsBetter
                  />
                }
                hint="من إرسال الطلب إلى قبوله"
              />
            </div>
          </Panel>

          {days.length > 0 ? (
            <div className="grid-2">
              <Panel title="الاستخدام يوماً بيوم" description="ما فعله الزوار في كل يوم من الفترة.">
                <LineChart
                  caption="الاستخدام اليومي"
                  days={days.map((day) => day.date)}
                  series={[
                    { key: "searches", label: "بحث", color: "var(--ad-brand)", values: line("searches") },
                    { key: "views", label: "مشاهدات", color: "var(--ad-info)", values: line("facilityViews") },
                    { key: "directions", label: "اتجاهات", color: "var(--ad-accent)", values: line("directionsRequests") },
                    { key: "empty", label: "بلا نتائج", color: "var(--ad-danger)", values: line("zeroResultSearches") },
                  ]}
                />
              </Panel>
              <Panel title="الحركة يوماً بيوم" description="حسابات جديدة، وطلبات قُبلت، وبلاغات وصلت.">
                <LineChart
                  caption="الحركة اليومية"
                  days={days.map((day) => day.date)}
                  series={[
                    { key: "users", label: "حسابات جديدة", color: "var(--ad-brand)", values: line("newUsers") },
                    { key: "approvals", label: "قبول", color: "var(--ad-success)", values: line("approvals") },
                    { key: "reports", label: "بلاغات", color: "var(--ad-warning)", values: line("reports") },
                  ]}
                />
              </Panel>
            </div>
          ) : null}

          <Panel title="الآن" description="أرقام حالية لا ترتبط بالفترة.">
            <div className="kpi-grid">
              <StatCard
                label="منشآت فعّالة"
                value={NUMBER.format(data.activeFacilities)}
                icon="building"
              />
              <StatCard
                label="طلبات بانتظار المراجعة"
                value={NUMBER.format(data.pendingReviews)}
                icon="inbox"
                tone="info"
                href="/reviews"
                hint="فتح قائمة المراجعات"
              />
              <StatCard
                label="متوسط التقييم"
                value={data.ratingAverage === null ? "—" : DECIMAL.format(data.ratingAverage)}
                icon="star"
                tone="warning"
              />
            </div>
          </Panel>

          <Panel title="أحداث المنتج" description="العدد الإجمالي لكل حدث منذ البداية." flush>
            <DataTable
              caption="أحداث المنتج"
              columns={columns}
              rows={data.events}
              rowKey={(row) => row.name}
            />
          </Panel>
        </>
      ) : null}

      {exports.length > 0 ? (
        <Panel
          title="تصدير البيانات"
          description="ملفات CSV تُفتح في إكسل والعربية سليمة، بكل الصفوف حتى ٥٠٬٠٠٠ صف ودون فلترة."
          testId="exports"
        >
          <ul className="export-list">
            {exports.map((item) => (
              <li key={item.name} className="export-item">
                <div>
                  <strong>{item.title}</strong>
                  <span className="muted">{item.detail}</span>
                </div>
                <ExportButton name={item.name} />
              </li>
            ))}
          </ul>
        </Panel>
      ) : null}
    </div>
  );
}
