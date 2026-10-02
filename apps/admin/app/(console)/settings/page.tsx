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

type AppRelease = Readonly<{
  platform: string;
  minimumVersionCode: number;
  latestVersionCode: number;
  storeUrl: string;
  noticeAr: string;
  updatedAt: string | null;
}>;

/**
 * What a mobile build must be to keep working.
 *
 * The one control in this console that can stop every phone in the field, so it says what it
 * will do before it does it, and it will not let the minimum pass the latest — nobody can
 * install a build that does not exist. Zero means nothing is enforced, which is what an
 * untouched platform reads as, and that is said rather than left to be guessed from a 0.
 */
function AppReleasePanel({ canManage }: { canManage: boolean }): React.JSX.Element {
  const release = useResource<AppRelease>("appRelease");
  const mutation = useMutation();
  const [draft, setDraft] = useState<Partial<AppRelease>>({});
  const [saved, setSaved] = useState<string | null>(null);

  const value = { ...release.data, ...draft } as AppRelease;
  const invalid =
    Number(value.minimumVersionCode ?? 0) > Number(value.latestVersionCode ?? 0);

  async function save(): Promise<void> {
    if (invalid) return;
    const ok = await mutation.run("appReleaseUpdate", {
      minimumVersionCode: Number(value.minimumVersionCode ?? 0),
      latestVersionCode: Number(value.latestVersionCode ?? 0),
      storeUrl: value.storeUrl ?? "",
      noticeAr: value.noticeAr ?? "",
    });
    if (!ok) return;
    setDraft({});
    setSaved("تم حفظ إصدار التطبيق.");
    release.reload();
  }

  return (
    <Panel title="إصدار التطبيق">
      {release.loading ? <LoadingState /> : null}
      {release.error ? <ErrorState error={release.error} onRetry={release.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {release.data ? (
        <div className="stack">
          <p className="field-hint">
            نسخة أقدم من «الحدّ الأدنى» تتوقّف وتعرض الرسالة أدناه. الصفر يعني ألّا يُمنع أحد.
          </p>
          <label className="field">
            <span>الحدّ الأدنى المقبول</span>
            <input
              type="number"
              min={0}
              inputMode="numeric"
              data-testid="release-minimum"
              disabled={!canManage}
              value={String(value.minimumVersionCode ?? 0)}
              onChange={(event) =>
                setDraft((d) => ({ ...d, minimumVersionCode: Number(event.target.value) }))
              }
            />
          </label>
          <label className="field">
            <span>أحدث نسخة منشورة</span>
            <input
              type="number"
              min={0}
              inputMode="numeric"
              data-testid="release-latest"
              disabled={!canManage}
              value={String(value.latestVersionCode ?? 0)}
              onChange={(event) =>
                setDraft((d) => ({ ...d, latestVersionCode: Number(event.target.value) }))
              }
            />
          </label>
          {invalid ? (
            <p className="field-error" data-testid="release-invalid">
              الحدّ الأدنى لا يمكن أن يتجاوز الأحدث: لا أحد يستطيع تثبيت نسخة غير موجودة.
            </p>
          ) : null}
          <label className="field">
            <span>رابط التحديث</span>
            <input
              type="url"
              data-testid="release-store"
              disabled={!canManage}
              placeholder="يُترك فارغًا فلا يظهر زرّ"
              value={value.storeUrl ?? ""}
              onChange={(event) => setDraft((d) => ({ ...d, storeUrl: event.target.value }))}
            />
          </label>
          <label className="field">
            <span>الرسالة المعروضة</span>
            <textarea
              rows={2}
              data-testid="release-notice"
              disabled={!canManage}
              placeholder="يُترك فارغًا فيستعمل التطبيق صياغته"
              value={value.noticeAr ?? ""}
              onChange={(event) => setDraft((d) => ({ ...d, noticeAr: event.target.value }))}
            />
          </label>
          {canManage ? (
            <div className="button-row">
              <button
                type="button"
                className="button-primary"
                data-testid="release-save"
                disabled={mutation.pending || invalid}
                onClick={save}
              >
                حفظ
              </button>
            </div>
          ) : null}
          {release.data.updatedAt ? (
            <p className="field-hint">آخر تغيير: {formatDateTime(release.data.updatedAt)}</p>
          ) : null}
        </div>
      ) : null}
      <Toast message={saved} onDismiss={() => setSaved(null)} />
    </Panel>
  );
}

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

      {/* Part of this screen, not a screen of its own: when the operator may not read the
          settings at all, the page says so once and this says nothing. Two identical
          permission notices stacked is a worse answer than one. */}
      {settings.error ? null : <AppReleasePanel canManage={canManage} />}

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
