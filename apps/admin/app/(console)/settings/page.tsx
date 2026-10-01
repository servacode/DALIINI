"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  Toast,
  formatDateTime,
} from "../../../components/ui";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";

type Setting = Readonly<{
  key: string;
  valueType: string;
  value: unknown;
  updatedAt: string;
}>;

/**
 * Human names for the settings the platform itself reads. Any other key still renders with
 * its raw name; this only makes the ones operators act on under pressure readable.
 */
const KNOWN: Record<string, { label: string; hint?: string }> = {
  "maintenance.enabled": {
    label: "وضع الصيانة",
    hint: "عند التفعيل يتوقف التطبيق العام ويعرض شاشة الصيانة. لوحة الإدارة تبقى متاحة.",
  },
  "maintenance.messageAr": { label: "رسالة الصيانة", hint: "تظهر للمستخدمين في شاشة الصيانة." },
  "maintenance.retryAfterSeconds": {
    label: "إعادة المحاولة بعد (ثوانٍ)",
    hint: "المدة التي تنتظرها التطبيقات قبل إعادة المحاولة تلقائياً.",
  },
};

/**
 * Typed platform settings.
 *
 * The control matches the declared type: a boolean gets a switch, a number gets a numeric
 * input, everything else gets text. Rendering every value as a string would work and would
 * be wrong — it invites an operator to type `true` into a boolean and produce a setting
 * that is neither.
 *
 * Each save is audited by the backend, so the trail records who changed which setting.
 */
export default function SettingsPage() {
  const settings = useResource<{ items: Setting[] }>("settings");
  const mutation = useMutation();
  const canManage = useCan("admin.settings.manage");

  const [drafts, setDrafts] = useState<Record<string, string | boolean | number>>({});
  const [toast, setToast] = useState<string | null>(null);

  function current(setting: Setting): string | boolean | number {
    if (setting.key in drafts) return drafts[setting.key]!;
    if (setting.valueType === "boolean") return Boolean(setting.value);
    if (setting.valueType === "number") return Number(setting.value ?? 0);
    return typeof setting.value === "string"
      ? setting.value
      : JSON.stringify(setting.value ?? "");
  }

  async function save(setting: Setting): Promise<void> {
    const ok = await mutation.run("settingWrite", {
      key: setting.key,
      type: setting.valueType,
      value: current(setting),
    });
    if (!ok) return;
    setToast(`تم حفظ الإعداد ${setting.key}.`);
    settings.reload();
  }

  return (
    <div className="stack">
      <PageHeader title="الإعدادات" description="إعدادات المنصة، كل تغيير مُسجَّل." />
      {settings.loading ? <LoadingState /> : null}
      {settings.error ? <ErrorState error={settings.error} onRetry={settings.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}

      {settings.data ? (
        settings.data.items.length === 0 ? (
          <EmptyState title="لا إعدادات مُهيّأة" />
        ) : (
          <Panel>
            {settings.data.items.map((setting) => (
              <div key={setting.key} className="switch-row">
                <span>
                  {KNOWN[setting.key] ? (
                    <>
                      <strong>{KNOWN[setting.key]!.label}</strong>
                      {KNOWN[setting.key]!.hint ? (
                        <span className="field-hint setting-hint">
                          {KNOWN[setting.key]!.hint}
                        </span>
                      ) : null}
                    </>
                  ) : null}
                  <code className="cell-ltr">{setting.key}</code>{" "}
                  <StatusBadge tone="neutral">{setting.valueType}</StatusBadge>
                  <br />
                  <span className="muted">
                    آخر تحديث:{" "}
                    <span className="cell-ltr">{formatDateTime(setting.updatedAt)}</span>
                  </span>
                </span>
                <span className="button-row">
                  {setting.valueType === "boolean" ? (
                    <input
                      type="checkbox"
                      disabled={!canManage}
                      data-testid={`setting-${setting.key}`}
                      checked={Boolean(current(setting))}
                      onChange={(event) =>
                        setDrafts({ ...drafts, [setting.key]: event.target.checked })
                      }
                    />
                  ) : setting.valueType === "number" ? (
                    <input
                      type="number"
                      dir="ltr"
                      disabled={!canManage}
                      data-testid={`setting-${setting.key}`}
                      value={String(current(setting))}
                      onChange={(event) =>
                        setDrafts({ ...drafts, [setting.key]: Number(event.target.value) })
                      }
                    />
                  ) : (
                    <input
                      type="text"
                      disabled={!canManage}
                      data-testid={`setting-${setting.key}`}
                      value={String(current(setting))}
                      onChange={(event) =>
                        setDrafts({ ...drafts, [setting.key]: event.target.value })
                      }
                    />
                  )}
                  {canManage ? (
                    <button
                      type="button"
                      className="button-ghost"
                      disabled={mutation.pending}
                      data-testid={`save-${setting.key}`}
                      onClick={() => save(setting)}
                    >
                      حفظ
                    </button>
                  ) : null}
                </span>
              </div>
            ))}
          </Panel>
        )
      ) : null}

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
