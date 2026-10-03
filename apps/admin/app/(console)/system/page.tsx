"use client";

import { type IconName, Icon } from "../../../components/icons";
import {
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  type Tone,
  formatDateTime,
} from "../../../components/ui";
import { useResource } from "../../../lib/client/use-resource";

type Status = "ok" | "warning" | "failed" | "off";

type HealthCheck = Readonly<{
  key: string;
  status: Status;
  summary: string;
  latencyMs: number | null;
  lastOkAt: string | null;
  lastFailureAt: string | null;
  metrics: readonly Readonly<{ key: string; value: number }>[];
}>;

type SystemStatus = Readonly<{
  apiVersion: string;
  environment: string;
  overall: Exclude<Status, "off">;
  checks: readonly HealthCheck[];
  schemaHash: string;
  checkedAt: string;
}>;

/** What each check is called, and what it is for, in the operator's words. */
const CHECKS: Record<string, { label: string; detail: string; icon: IconName }> = {
  database: { label: "قاعدة البيانات", detail: "كل بيانات المنصة.", icon: "server" },
  redis: { label: "Redis", detail: "الطابور والتخزين المؤقت والتحديثات الفورية.", icon: "layers" },
  worker: { label: "عامل المهام", detail: "يرسل الإشعارات وينفّذ المهام الخلفية.", icon: "tool" },
  scheduler: { label: "المجدول", detail: "التذكيرات والتنظيف الليلي.", icon: "calendar" },
  storage: { label: "مساحة الملفات", detail: "الصور ووثائق التحقق.", icon: "image" },
  otp: { label: "رموز التحقق", detail: "التسجيل واستعادة الحساب.", icon: "whatsapp" },
  push: { label: "الإشعارات", detail: "تنبيهات الهواتف.", icon: "bell" },
  backup: { label: "النسخ الاحتياطي", detail: "نسخة يومية من قاعدة البيانات.", icon: "download" },
  errors: { label: "تتبع الأخطاء", detail: "يُعلمنا بالأعطال قبل أن يبلغ عنها الناس.", icon: "alert" },
  maintenance: { label: "وضع الصيانة", detail: "يوقف الواجهات العامة عند الحاجة.", icon: "settings" },
};

const METRICS: Record<string, string> = {
  workers: "العمّال المستجيبون",
  pendingMigrations: "ترحيلات لم تُطبَّق",
  sent24h: "رموز أُرسلت خلال ٢٤ ساعة",
  verified24h: "رموز استُخدمت خلال ٢٤ ساعة",
  failures: "إخفاقات متتالية",
  activeDevices: "أجهزة تستقبل الإشعارات",
  deliveries24h: "إشعارات وصلت خلال ٢٤ ساعة",
};

const STATUS: Record<Status, { label: string; tone: Tone }> = {
  ok: { label: "يعمل", tone: "positive" },
  warning: { label: "يحتاج انتباهاً", tone: "warning" },
  failed: { label: "متوقف", tone: "danger" },
  off: { label: "غير مستخدم هنا", tone: "neutral" },
};

const OVERALL: Record<SystemStatus["overall"], string> = {
  ok: "كل الخدمات تعمل.",
  warning: "المنصة تعمل، وفيها ما يستحق النظر.",
  failed: "خدمة على الأقل متوقفة. ابدأ بالبطاقات الحمراء.",
};

const NUMBER = new Intl.NumberFormat("ar-SY");

/**
 * Every dependency, asked directly when the page opens and every minute after (DECISION-073).
 *
 * Each card is one service: whether it works, a sentence saying what that means, how long it
 * took to answer, and the numbers behind the answer. The ones that do not answer requests (the
 * scheduler, the code channel, push, the nightly backup) are read from what they last recorded.
 * Nothing here is a host, an address or a credential: the backend does not send them.
 */
export default function SystemPage() {
  const status = useResource<SystemStatus>("systemStatus", {}, { refreshMs: 60_000 });
  const data = status.data;

  return (
    <div className="stack">
      <PageHeader
        title="حالة النظام"
        description="كل خدمة تحتاجها المنصة، تُسأل مباشرة الآن ويُعاد سؤالها كل دقيقة."
        actions={
          <button
            type="button"
            className="button-ghost"
            onClick={status.reload}
            disabled={status.loading}
            data-testid="system-recheck"
          >
            <Icon name="refresh" />
            إعادة الفحص
          </button>
        }
      />
      {status.loading && !data ? <LoadingState label="جارٍ سؤال الخدمات…" /> : null}
      {status.error ? <ErrorState error={status.error} onRetry={status.reload} /> : null}
      {data ? (
        <>
          <div className="health-overall" data-status={data.overall} role="status" data-testid="system-overall">
            <Icon name={data.overall === "ok" ? "checkCircle" : "alert"} width={22} height={22} />
            <span>
              <strong>{OVERALL[data.overall]}</strong>
              <span className="muted"> آخر فحص {formatDateTime(data.checkedAt)}</span>
            </span>
          </div>

          <section aria-labelledby="dependencies-title" className="stack">
            <h2 id="dependencies-title" className="section-title">
              الاعتماديات
            </h2>
            <ul className="health-grid">
              {data.checks.map((check) => (
                <HealthCard key={check.key} check={check} />
              ))}
            </ul>
          </section>

          <Panel title="الإصدار">
            <KeyValueList
              items={[
                { label: "إصدار الواجهة البرمجية", value: data.apiVersion, ltr: true },
                { label: "البيئة", value: data.environment, ltr: true },
                {
                  label: "بصمة العقد",
                  value: <code>{data.schemaHash.slice(0, 16)}…</code>,
                  ltr: true,
                },
                { label: "وقت الفحص", value: formatDateTime(data.checkedAt), ltr: true },
              ]}
            />
          </Panel>
        </>
      ) : null}
    </div>
  );
}

function HealthCard({ check }: { check: HealthCheck }) {
  const meta = CHECKS[check.key] ?? { label: check.key, detail: "", icon: "info" as IconName };
  const state = STATUS[check.status];
  const facts: { label: string; value: string }[] = [];
  if (check.latencyMs !== null) {
    facts.push({ label: "زمن الاستجابة", value: `${NUMBER.format(check.latencyMs)} م.ث` });
  }
  for (const metric of check.metrics) {
    facts.push({ label: METRICS[metric.key] ?? metric.key, value: NUMBER.format(metric.value) });
  }
  if (check.lastOkAt) facts.push({ label: "آخر نجاح", value: formatDateTime(check.lastOkAt) });
  if (check.lastFailureAt) {
    facts.push({ label: "آخر إخفاق", value: formatDateTime(check.lastFailureAt) });
  }

  return (
    <li className="health-card" data-status={check.status} data-testid={`health-${check.key}`}>
      <header>
        <span className="health-icon">
          <Icon name={meta.icon} width={20} height={20} />
        </span>
        <span className="health-name">
          <strong>{meta.label}</strong>
          <span className="muted">{meta.detail}</span>
        </span>
        <StatusBadge tone={state.tone}>{state.label}</StatusBadge>
      </header>
      <p className="health-summary">{check.summary}</p>
      {facts.length > 0 ? (
        <dl className="health-facts">
          {facts.map((fact) => (
            <div key={fact.label}>
              <dt>{fact.label}</dt>
              <dd className="tabular">{fact.value}</dd>
            </div>
          ))}
        </dl>
      ) : null}
    </li>
  );
}
