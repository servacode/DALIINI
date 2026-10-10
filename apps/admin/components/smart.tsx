"use client";

import Link from "next/link";

import { hrefFor, type EntityLink } from "../lib/client/links";
import { useResource } from "../lib/client/use-resource";
import { Icon } from "./icons";
import { EmptyState, ErrorState, LoadingState, Panel, StatusBadge, labelsFor } from "./ui";

/*
 * The console's "what needs me now" surfaces: alerts the platform raises by itself, and the
 * task centre that orders work by age against the review SLA. Both refresh in place every
 * minute while the tab is visible, like the review queue.
 */

type Alert = Readonly<{
  kind: string;
  severity: "info" | "warning" | "critical";
  titleAr: string;
  detailAr: string;
  count: number;
  link: EntityLink | null;
}>;

const SEVERITY_LABEL: Record<Alert["severity"], string> = {
  critical: "عاجل",
  warning: "تنبيه",
  info: "للعلم",
};

/**
 * The alerts, newest state first. On the dashboard nothing is shown when there are none; on
 * their own page ([standalone]) that silence would read as a page that failed to load, so the
 * page says there are none.
 */
export function AlertsPanel({ standalone = false }: { standalone?: boolean }) {
  const alerts = useResource<{ items: Alert[] }>("alerts", {}, { refreshMs: 60_000 });
  if (alerts.loading) return standalone ? <LoadingState /> : null;
  if (alerts.error) return <ErrorState error={alerts.error} onRetry={alerts.reload} />;
  const items = alerts.data?.items ?? [];
  if (items.length === 0) {
    return standalone ? (
      <EmptyState title="لا تنبيهات الآن" hint="تظهر هنا تلقائياً كل مشكلة تحتاج انتباهك." />
    ) : null;
  }
  return (
    <section className="alert-list" aria-label="تنبيهات تحتاج انتباهك" data-testid="alerts">
      {items.map((alert, index) => {
        const href = hrefFor(alert.link);
        const body = (
          <>
            <span className="alert-icon" aria-hidden="true">
              <Icon name={alert.severity === "info" ? "info" : "alert"} />
            </span>
            <span className="alert-text">
              <strong>{alert.titleAr}</strong>
              <span>{alert.detailAr}</span>
            </span>
            <span className="alert-meta">
              <span className="alert-severity">{SEVERITY_LABEL[alert.severity]}</span>
              {href ? <Icon name="chevron" /> : null}
            </span>
          </>
        );
        const key = `${alert.kind}-${index}`;
        return href ? (
          <Link key={key} href={href} className="alert-item" data-severity={alert.severity}>
            {body}
          </Link>
        ) : (
          <div key={key} className="alert-item" data-severity={alert.severity}>
            {body}
          </div>
        );
      })}
    </section>
  );
}

type AgeItem = Readonly<{ ageHours: number; overdue: boolean }>;
type Bucket<T> = Readonly<{ count: number; overdueCount: number; oldest: readonly T[] }>;
type Tasks = Readonly<{
  slaHours: number;
  applications: Readonly<{
    initial: Bucket<AgeItem & { id: string; facilityNameAr: string; provinceNameAr: string }>;
    reverification: Bucket<AgeItem & { id: string; facilityNameAr: string; provinceNameAr: string }>;
    change?: Bucket<AgeItem & { id: string; facilityNameAr: string; provinceNameAr: string }>;
    claim?: Bucket<AgeItem & { id: string; facilityNameAr: string; provinceNameAr: string }>;
  }>;
  reports: Bucket<
    AgeItem & { facilityId: string; facilityNameAr: string; openCount: number; reasons: readonly string[] }
  > & { facilityCount: number };
  reverificationRequired: Bucket<AgeItem & { facilityId: string; facilityNameAr: string; provinceNameAr: string }>;
}>;

function age(hours: number): string {
  if (hours < 1) return "أقل من ساعة";
  if (hours < 48) return `منذ ${Math.round(hours)} ساعة`;
  return `منذ ${Math.round(hours / 24)} يوماً`;
}

function Row({
  href,
  title,
  subtitle,
  item,
}: {
  href: string;
  title: string;
  subtitle?: string;
  item: AgeItem;
}) {
  return (
    <li>
      <Link href={href} className="task-row">
        <span className="task-title">
          <strong>{title}</strong>
          {subtitle ? <span className="muted">{subtitle}</span> : null}
        </span>
        <span className="task-age" data-overdue={item.overdue || undefined}>
          <Icon name="clock" />
          {age(item.ageHours)}
        </span>
      </Link>
    </li>
  );
}

const REASONS = labelsFor("reportReason");

/** Work waiting for a decision, oldest first, with anything past the SLA marked. */
export function TaskCenter() {
  const tasks = useResource<Tasks>("tasks", {}, { refreshMs: 60_000 });
  if (tasks.loading) return <LoadingState />;
  if (tasks.error) return <ErrorState error={tasks.error} onRetry={tasks.reload} />;
  if (!tasks.data) return null;
  const t = tasks.data;
  const groups = [
    {
      key: "initial",
      title: "طلبات تسجيل جديدة",
      href: "/reviews?kind=INITIAL",
      bucket: t.applications.initial,
      rows: t.applications.initial.oldest.map((item) => (
        <Row key={item.id} href={`/reviews/${item.id}`} title={item.facilityNameAr} subtitle={item.provinceNameAr} item={item} />
      )),
    },
    {
      key: "reverification",
      title: "طلبات إعادة تحقق",
      href: "/reviews?kind=REVERIFICATION",
      bucket: t.applications.reverification,
      rows: t.applications.reverification.oldest.map((item) => (
        <Row key={item.id} href={`/reviews/${item.id}`} title={item.facilityNameAr} subtitle={item.provinceNameAr} item={item} />
      )),
    },
    ...(t.applications.change
      ? [
          {
            key: "change",
            title: "تعديلات على منشآت ظاهرة",
            href: "/reviews?kind=CHANGE",
            bucket: t.applications.change,
            rows: t.applications.change.oldest.map((item) => (
              <Row key={item.id} href={`/reviews/${item.id}`} title={item.facilityNameAr} subtitle={item.provinceNameAr} item={item} />
            )),
          },
        ]
      : []),
    ...(t.applications.claim
      ? [
          {
            key: "claim",
            title: "مطالبات بملكية منشآت",
            href: "/reviews?kind=CLAIM",
            bucket: t.applications.claim,
            rows: t.applications.claim.oldest.map((item) => (
              <Row key={item.id} href={`/reviews/${item.id}`} title={item.facilityNameAr} subtitle={item.provinceNameAr} item={item} />
            )),
          },
        ]
      : []),
    {
      key: "reports",
      title: "بلاغات مفتوحة",
      href: "/reports",
      bucket: t.reports,
      rows: t.reports.oldest.map((item) => (
        <Row
          key={item.facilityId}
          href={`/facilities/${item.facilityId}`}
          title={item.facilityNameAr}
          subtitle={`${item.openCount} بلاغ · ${item.reasons.map((r) => REASONS[r] ?? r).join("، ")}`}
          item={item}
        />
      )),
    },
    {
      key: "reverificationRequired",
      title: "منشآت تحتاج إعادة تحقق",
      href: "/facilities?status=REVERIFICATION_REQUIRED",
      bucket: t.reverificationRequired,
      rows: t.reverificationRequired.oldest.map((item) => (
        <Row key={item.facilityId} href={`/facilities/${item.facilityId}`} title={item.facilityNameAr} subtitle={item.provinceNameAr} item={item} />
      )),
    },
  ];
  const total = groups.reduce((sum, group) => sum + group.bucket.count, 0);

  return (
    <Panel
      title="مركز المهام"
      description={`كل ما ينتظر قراراً، الأقدم أولاً. المهلة المستهدفة ${t.slaHours} ساعة.`}
      testId="task-center"
    >
      {total === 0 ? (
        <EmptyState title="لا مهام معلّقة" hint="كل الطلبات والبلاغات محسومة." illustration="success" />
      ) : (
        <div className="task-groups">
          {groups
            .filter((group) => group.bucket.count > 0)
            .map((group) => (
              <section key={group.key} className="task-group">
                <header>
                  <Link href={group.href}>{group.title}</Link>
                  <span className="button-row">
                    <StatusBadge tone="neutral">{group.bucket.count}</StatusBadge>
                    {group.bucket.overdueCount > 0 ? (
                      <StatusBadge tone="danger">{`${group.bucket.overdueCount} متأخر`}</StatusBadge>
                    ) : null}
                  </span>
                </header>
                <ul>{group.rows}</ul>
              </section>
            ))}
        </div>
      )}
    </Panel>
  );
}
