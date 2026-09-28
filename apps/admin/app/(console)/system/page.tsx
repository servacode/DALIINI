"use client";

import Link from "next/link";

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
      <PageHeader
        title="حالة النظام"
        description="جاهزية الاعتماديات التشغيلية."
        actions={
          <Link className="button-ghost" href="/design">
            نظام التصميم
          </Link>
        }
      />
      {status.loading ? <LoadingState /> : null}
      {status.error ? <ErrorState error={status.error} onRetry={status.reload} /> : null}
      {status.data ? (
        <div className="grid-2">
          <Panel title="الاعتماديات">
            {dependencies.map(([label, value]) => (
              <div key={label} className="switch-row">
                <span>{label}</span>
                <StatusBadge tone={TONES[value] ?? "neutral"}>
                  {LABELS[value] ?? value}
                </StatusBadge>
              </div>
            ))}
          </Panel>
          <Panel title="الإصدار">
            <KeyValueList
              items={[
                { label: "إصدار الواجهة البرمجية", value: status.data.apiVersion, ltr: true },
                { label: "البيئة", value: status.data.environment, ltr: true },
                {
                  label: "بصمة العقد",
                  value: <code>{status.data.schemaHash.slice(0, 16)}…</code>,
                  ltr: true,
                },
                { label: "وقت الفحص", value: formatDateTime(status.data.checkedAt), ltr: true },
              ]}
            />
          </Panel>
        </div>
      ) : null}
    </div>
  );
}
