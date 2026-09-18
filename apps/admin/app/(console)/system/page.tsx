"use client";

import {
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  type Tone,
  formatDateTime,
} from "../../../components/ui";
import { useResource } from "../../../lib/client/use-resource";

type SystemStatus = Readonly<{
  apiVersion: string;
  environment: string;
  database: string;
  redis: string;
  celery: string;
  storage: string;
  schemaHash: string;
  checkedAt: string;
}>;

const TONES: Record<string, Tone> = {
  ok: "positive",
  configured: "positive",
  unavailable: "danger",
  unconfigured: "warning",
};

const LABELS: Record<string, string> = {
  ok: "متاح",
  configured: "مُهيّأ",
  unavailable: "غير متاح",
  unconfigured: "غير مُهيّأ",
};

/**
 * Dependency presence, and nothing more.
 *
 * The endpoint reports whether each dependency is configured and reachable. It returns no
 * connection string, host, credential or token, so there is nothing here to redact — the
 * safety is in what the backend declines to send, not in what this screen hides.
 */
export default function SystemPage() {
  const status = useResource<SystemStatus>("systemStatus");

  const dependencies: readonly (readonly [string, string])[] = status.data
    ? [
        ["قاعدة البيانات", status.data.database],
        ["Redis", status.data.redis],
        ["Celery", status.data.celery],
        ["التخزين", status.data.storage],
      ]
    : [];

  return (
    <div className="stack">
      <PageHeader title="حالة النظام" description="جاهزية الاعتماديات التشغيلية." />
      {status.loading ? <LoadingState /> : null}
      {status.error ? <ErrorState error={status.error} onRetry={status.reload} /> : null}
      {status.data ? (
        <>
          <section className="panel stack">
            <h2>الاعتماديات</h2>
            {dependencies.map(([label, value]) => (
              <div key={label} className="switch-row">
                <span>{label}</span>
                <StatusBadge tone={TONES[value] ?? "neutral"}>
                  {LABELS[value] ?? value}
                </StatusBadge>
              </div>
            ))}
          </section>
          <section className="panel stack">
            <h2>الإصدار</h2>
            <div className="switch-row">
              <span>إصدار الواجهة البرمجية</span>
              <span className="cell-ltr">{status.data.apiVersion}</span>
            </div>
            <div className="switch-row">
              <span>البيئة</span>
              <span className="cell-ltr">{status.data.environment}</span>
            </div>
            <div className="switch-row">
              <span>بصمة العقد</span>
              <code className="cell-ltr">{status.data.schemaHash.slice(0, 16)}…</code>
            </div>
            <div className="switch-row">
              <span>وقت الفحص</span>
              <span className="cell-ltr">{formatDateTime(status.data.checkedAt)}</span>
            </div>
          </section>
        </>
      ) : null}
    </div>
  );
}
