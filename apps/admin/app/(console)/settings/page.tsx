"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
  Toast,
  formatDateTime,
} from "../../../components/ui";
import { relativeTime } from "../../../components/ui/extra";
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
const KNOWN: Record<string, { label: string; hint?: string; group: Group; unit?: string }> = {
  "maintenance.enabled": {
    label: "وضع الصيانة",
    hint: "عند التفعيل يتوقف التطبيق العام ويعرض شاشة الصيانة. لوحة الإدارة تبقى متاحة.",
    group: "maintenance",
  },
  "maintenance.messageAr": {
    label: "رسالة الصيانة",
    hint: "تظهر للمستخدمين في شاشة الصيانة.",
    group: "maintenance",
  },
  "maintenance.retryAfterSeconds": {
    label: "إعادة المحاولة بعد",
    hint: "المدة التي تنتظرها التطبيقات قبل أن تعيد المحاولة تلقائياً.",
    group: "maintenance",
    unit: "ثانية",
  },
  "review.slaHours": {
    label: "مهلة مراجعة الطلب",
    hint: "الطلب الذي ينتظر أكثر منها يظهر متأخراً في الرئيسية وفي مركز المهام.",
    group: "operations",
    unit: "ساعة",
  },
  "support.whatsapp": {
    label: "رقم واتساب الدعم",
    hint: "يظهر في التطبيق والموقع زرَّ «راسلنا على واتساب». مثل 0933123456، أو فارغاً لإخفائه.",
    group: "support",
  },
  "support.email": {
    label: "بريد الدعم",
    hint: "يظهر في صفحتي الدعم والتواصل بالموقع. اختياري.",
    group: "support",
  },
  "readiness.minActiveFacilities": {
    label: "أقل عدد منشآت لفتح محافظة",
    hint: "من شروط جاهزية المحافظة للإطلاق: عدد المنشآت الفعّالة فيها.",
    group: "operations",
    unit: "منشأة",
  },
};

type Group = "support" | "maintenance" | "operations" | "other";

const GROUPS: readonly { key: Group; title: string; description: string }[] = [
  {
    key: "support",
    title: "الدعم والتواصل",
    description: "كيف يصل الناس إلى فريق المنصة.",
  },
  {
    key: "maintenance",
    title: "الصيانة",
    description: "إيقاف التطبيق العام مؤقتاً، وما يراه الناس أثناءه.",
  },
  {
    key: "operations",
    title: "المراجعة والإطلاق",
    description: "مُهل العمل اليومي وشروط فتح المحافظات.",
  },
  { key: "other", title: "إعدادات أخرى", description: "إعدادات لم تُسمَّ بعد في اللوحة." },
];

/** The declared type, whatever case the backend writes it in. */
function kindOf(setting: Setting): "boolean" | "number" | "text" {
  const type = setting.valueType.toLowerCase();
  if (type === "boolean" || type === "bool") return "boolean";
  if (type === "integer" || type === "number" || type === "int" || type === "float") return "number";
  return "text";
}

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
    <Panel
      title="إصدار التطبيق"
      description="نسخة أقدم من «الحدّ الأدنى» تتوقّف وتعرض الرسالة أدناه. الصفر يعني ألّا يُمنع أحد."
    >
      {release.loading ? <LoadingState /> : null}
      {release.error ? <ErrorState error={release.error} onRetry={release.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {release.data ? (
        <div className="stack">
          <div className="form-grid-2">
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
          </div>
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

  function stored(setting: Setting): string | boolean | number {
    const kind = kindOf(setting);
    if (kind === "boolean") return setting.value === true || setting.value === "true";
    if (kind === "number") return Number(setting.value ?? 0);
    return typeof setting.value === "string" ? setting.value : JSON.stringify(setting.value ?? "");
  }

  function current(setting: Setting): string | boolean | number {
    return setting.key in drafts ? drafts[setting.key]! : stored(setting);
  }

  async function save(setting: Setting): Promise<void> {
    const ok = await mutation.run("settingWrite", {
      key: setting.key,
      type: setting.valueType,
      value: current(setting),
    });
    if (!ok) return;
    setDrafts(Object.fromEntries(Object.entries(drafts).filter(([key]) => key !== setting.key)));
    setToast(`حُفظ «${KNOWN[setting.key]?.label ?? setting.key}».`);
    settings.reload();
  }

  const items = settings.data?.items ?? [];

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الإعدادات والنظام"
        title="الإعدادات"
        description="إعدادات المنصة. كل تغيير يُحفظ في سجل العمليات باسم من غيّره."
      />
      {settings.loading ? <LoadingState /> : null}
      {settings.error ? <ErrorState error={settings.error} onRetry={settings.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}

      {settings.data && items.length === 0 ? <EmptyState title="لا إعدادات مُهيّأة" /> : null}

      {GROUPS.map((group) => {
        const inGroup = items.filter((setting) => (KNOWN[setting.key]?.group ?? "other") === group.key);
        if (inGroup.length === 0) return null;
        return (
          <section key={group.key} className="card-section" aria-labelledby={`settings-${group.key}`}>
            <header className="card-section-head">
              <h2 id={`settings-${group.key}`}>{group.title}</h2>
              <span>{group.description}</span>
            </header>
            <ul className="setting-grid">
              {inGroup.map((setting) => {
                const known = KNOWN[setting.key];
                const kind = kindOf(setting);
                const value = current(setting);
                const dirty = setting.key in drafts && drafts[setting.key] !== stored(setting);
                const on = kind === "boolean" && value === true;
                return (
                  <li
                    key={setting.key}
                    className="setting-card"
                    data-on={on || undefined}
                    data-alert={(setting.key === "maintenance.enabled" && on) || undefined}
                  >
                    <div className="setting-text">
                      <strong>{known?.label ?? setting.key}</strong>
                      {known?.hint ? <span>{known.hint}</span> : null}
                    </div>
                    <div className="setting-control">
                      {kind === "boolean" ? (
                        <label className="switch-inline">
                          <input
                            type="checkbox"
                            className="switch"
                            disabled={!canManage}
                            data-testid={`setting-${setting.key}`}
                            checked={Boolean(value)}
                            onChange={(event) =>
                              setDrafts({ ...drafts, [setting.key]: event.target.checked })
                            }
                          />
                          <span>{value ? "مفعّل" : "متوقف"}</span>
                        </label>
                      ) : kind === "number" ? (
                        <span className="setting-number">
                          <input
                            type="number"
                            dir="ltr"
                            min={0}
                            disabled={!canManage}
                            data-testid={`setting-${setting.key}`}
                            value={String(value)}
                            onChange={(event) =>
                              setDrafts({ ...drafts, [setting.key]: Number(event.target.value) })
                            }
                          />
                          {known?.unit ? <span>{known.unit}</span> : null}
                        </span>
                      ) : (
                        <input
                          type="text"
                          className="setting-text-input"
                          disabled={!canManage}
                          data-testid={`setting-${setting.key}`}
                          value={String(value)}
                          onChange={(event) =>
                            setDrafts({ ...drafts, [setting.key]: event.target.value })
                          }
                        />
                      )}
                    </div>
                    <div className="setting-foot">
                      <span className="muted">{`آخر تغيير ${relativeTime(setting.updatedAt)}`}</span>
                      {canManage ? (
                        <button
                          type="button"
                          className={dirty ? "button-primary" : "button-ghost"}
                          disabled={mutation.pending || !dirty}
                          data-testid={`save-${setting.key}`}
                          onClick={() => save(setting)}
                        >
                          {dirty ? "حفظ التغيير" : "محفوظ"}
                        </button>
                      ) : null}
                    </div>
                  </li>
                );
              })}
            </ul>
          </section>
        );
      })}

      {/* Part of this screen, not a screen of its own: when the operator may not read the
          settings at all, the page says so once and this says nothing. Two identical
          permission notices stacked is a worse answer than one. */}
      {settings.error ? null : <AppReleasePanel canManage={canManage} />}

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
