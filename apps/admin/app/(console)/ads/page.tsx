"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
  formatDateTime,
} from "../../../components/ui";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../lib/errors/messages";

type Advertisement = Readonly<{
  id: string;
  titleAr: string;
  targetScope: string;
  enabled: boolean;
  startsAt: string | null;
  endsAt: string | null;
  sortOrder: number;
  slideDurationMs: number;
}>;

const SCOPES = [
  ["GLOBAL", "كل المحافظات"],
  ["PROVINCE", "محافظة محددة"],
  ["CATEGORY", "تصنيف محدد"],
] as const;

/** `datetime-local` needs `YYYY-MM-DDTHH:mm`; ISO with a zone is what the contract wants. */
function toLocalInput(value: string | null): string {
  if (!value) return "";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${parsed.getFullYear()}-${pad(parsed.getMonth() + 1)}-${pad(parsed.getDate())}T${pad(parsed.getHours())}:${pad(parsed.getMinutes())}`;
}

function toIso(value: string): string | null {
  if (!value) return null;
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed.toISOString();
}

type Draft = {
  id?: string;
  imageKey: string;
  titleAr: string;
  targetScope: string;
  provinceId: string;
  categoryId: string;
  startsAt: string;
  endsAt: string;
  enabled: boolean;
  sortOrder: string;
  slideDurationMs: string;
};

const BLANK: Draft = {
  imageKey: "",
  titleAr: "",
  targetScope: "GLOBAL",
  provinceId: "",
  categoryId: "",
  startsAt: "",
  endsAt: "",
  enabled: false,
  sortOrder: "0",
  slideDurationMs: "5000",
};

/**
 * Advertisement lifecycle: create, edit, schedule, activate, remove.
 *
 * Schedule and targeting are validated by the backend as one set — an end before its start,
 * a global advertisement carrying a province, a slide duration outside its bounds — so the
 * messages shown here are the backend's, not a second copy of the same rules.
 */
export default function AdsPage() {
  const ads = useResource<{ items: Advertisement[] }>("ads");
  const provinces = useResource<{ items: { id: string; nameAr: string }[] }>("provinces");
  const categories = useResource<{ items: { id: string; nameAr: string }[] }>("categories");
  const mutation = useMutation();
  const canManage = useCan("admin.ads.manage");

  const [draft, setDraft] = useState<Draft | null>(null);
  const [removing, setRemoving] = useState<Advertisement | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);

  async function save(): Promise<void> {
    if (!draft) return;
    const payload: Record<string, unknown> = {
      imageKey: draft.imageKey.trim(),
      titleAr: draft.titleAr.trim(),
      targetScope: draft.targetScope,
      startsAt: toIso(draft.startsAt),
      endsAt: toIso(draft.endsAt),
      enabled: draft.enabled,
      sortOrder: Number(draft.sortOrder) || 0,
      slideDurationMs: Number(draft.slideDurationMs) || 5000,
      provinceId: draft.targetScope === "PROVINCE" ? draft.provinceId || null : null,
      categoryId: draft.targetScope === "CATEGORY" ? draft.categoryId || null : null,
    };
    if (draft.id) payload.id = draft.id;
    const ok = await mutation.run(draft.id ? "adUpdate" : "adCreate", payload);
    if (!ok) return;
    setDraft(null);
    setToast(draft.id ? "تم تحديث الإعلان." : "تمت إضافة الإعلان.");
    ads.reload();
  }

  async function remove(): Promise<void> {
    if (!removing) return;
    const ok = await mutation.run("adDelete", { id: removing.id });
    if (!ok) return;
    setRemoving(null);
    setToast("تم حذف الإعلان.");
    ads.reload();
  }

  async function toggle(ad: Advertisement): Promise<void> {
    const ok = await mutation.run("adUpdate", { id: ad.id, enabled: !ad.enabled });
    if (!ok) return;
    setToast(ad.enabled ? "تم إيقاف الإعلان." : "تم تفعيل الإعلان.");
    ads.reload();
  }

  return (
    <div className="stack">
      <PageHeader
        title="الإعلانات"
        description="إعلانات الطرف الأول: المحتوى والاستهداف والجدولة."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-ad"
              onClick={() => {
                mutation.reset();
                setDraft({ ...BLANK });
              }}
            >
              إعلان جديد
            </button>
          ) : null
        }
      />

      {ads.loading ? <LoadingState /> : null}
      {ads.error ? <ErrorState error={ads.error} onRetry={ads.reload} /> : null}
      {mutation.error && !draft ? <ErrorState error={mutation.error} /> : null}

      {ads.data ? (
        ads.data.items.length === 0 ? (
          <EmptyState title="لا إعلانات بعد" />
        ) : (
          <div className="table-wrap">
            <table className="data-table" data-testid="data-table">
              <caption className="sr-only">الإعلانات</caption>
              <thead>
                <tr>
                  <th scope="col">العنوان</th>
                  <th scope="col">الاستهداف</th>
                  <th scope="col">من</th>
                  <th scope="col">إلى</th>
                  <th scope="col">الحالة</th>
                  <th scope="col" />
                </tr>
              </thead>
              <tbody>
                {ads.data.items.map((ad) => (
                  <tr key={ad.id}>
                    <td>{ad.titleAr || "—"}</td>
                    <td>{SCOPES.find(([value]) => value === ad.targetScope)?.[1]}</td>
                    <td className="cell-ltr">{formatDateTime(ad.startsAt)}</td>
                    <td className="cell-ltr">{formatDateTime(ad.endsAt)}</td>
                    <td>
                      <StatusBadge tone={ad.enabled ? "positive" : "neutral"}>
                        {ad.enabled ? "فعّال" : "متوقف"}
                      </StatusBadge>
                    </td>
                    <td>
                      {canManage ? (
                        <div className="button-row">
                          <button
                            type="button"
                            className="button-ghost"
                            data-testid={`edit-ad-${ad.id}`}
                            onClick={() => {
                              mutation.reset();
                              setDraft({
                                ...BLANK,
                                id: ad.id,
                                titleAr: ad.titleAr,
                                targetScope: ad.targetScope,
                                startsAt: toLocalInput(ad.startsAt),
                                endsAt: toLocalInput(ad.endsAt),
                                enabled: ad.enabled,
                                sortOrder: String(ad.sortOrder),
                                slideDurationMs: String(ad.slideDurationMs),
                              });
                            }}
                          >
                            تعديل
                          </button>
                          <button
                            type="button"
                            className="button-ghost"
                            data-testid={`toggle-ad-${ad.id}`}
                            onClick={() => toggle(ad)}
                          >
                            {ad.enabled ? "إيقاف" : "تفعيل"}
                          </button>
                          <button
                            type="button"
                            className="button-ghost"
                            data-tone="danger"
                            data-testid={`delete-ad-${ad.id}`}
                            onClick={() => {
                              mutation.reset();
                              setRemoving(ad);
                            }}
                          >
                            حذف
                          </button>
                        </div>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : null}

      <ConfirmDialog
        open={draft !== null}
        title={draft?.id ? "تعديل الإعلان" : "إعلان جديد"}
        confirmLabel={draft?.id ? "حفظ" : "إضافة"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={save}
        onCancel={() => setDraft(null)}
      >
        {draft ? (
          <>
            <label className="field">
              <span>مفتاح الصورة</span>
              <input
                dir="ltr"
                value={draft.imageKey}
                data-testid="ad-image"
                aria-invalid={Boolean(errors.imageKey)}
                onChange={(event) => setDraft({ ...draft, imageKey: event.target.value })}
              />
              {errors.imageKey ? <span className="field-error">{errors.imageKey}</span> : null}
            </label>
            <label className="field">
              <span>العنوان</span>
              <input
                value={draft.titleAr}
                data-testid="ad-title"
                onChange={(event) => setDraft({ ...draft, titleAr: event.target.value })}
              />
            </label>
            <label className="field">
              <span>نطاق الاستهداف</span>
              <select
                value={draft.targetScope}
                data-testid="ad-scope"
                onChange={(event) => setDraft({ ...draft, targetScope: event.target.value })}
              >
                {SCOPES.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
            {draft.targetScope === "PROVINCE" ? (
              <label className="field">
                <span>المحافظة</span>
                <select
                  value={draft.provinceId}
                  data-testid="ad-province"
                  onChange={(event) => setDraft({ ...draft, provinceId: event.target.value })}
                >
                  <option value="">اختر محافظة</option>
                  {(provinces.data?.items ?? []).map((province) => (
                    <option key={province.id} value={province.id}>
                      {province.nameAr}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            {draft.targetScope === "CATEGORY" ? (
              <label className="field">
                <span>التصنيف</span>
                <select
                  value={draft.categoryId}
                  data-testid="ad-category"
                  onChange={(event) => setDraft({ ...draft, categoryId: event.target.value })}
                >
                  <option value="">اختر تصنيفاً</option>
                  {(categories.data?.items ?? []).map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.nameAr}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            <label className="field">
              <span>يبدأ في</span>
              <input
                type="datetime-local"
                dir="ltr"
                value={draft.startsAt}
                data-testid="ad-starts"
                onChange={(event) => setDraft({ ...draft, startsAt: event.target.value })}
              />
            </label>
            <label className="field">
              <span>ينتهي في</span>
              <input
                type="datetime-local"
                dir="ltr"
                value={draft.endsAt}
                data-testid="ad-ends"
                aria-invalid={Boolean(errors.ends_at || errors.endsAt)}
                onChange={(event) => setDraft({ ...draft, endsAt: event.target.value })}
              />
              {errors.ends_at || errors.endsAt ? (
                <span className="field-error">{errors.ends_at ?? errors.endsAt}</span>
              ) : null}
            </label>
            <label className="switch-row">
              <span>مفعّل</span>
              <input
                type="checkbox"
                data-testid="ad-enabled"
                checked={draft.enabled}
                onChange={(event) => setDraft({ ...draft, enabled: event.target.checked })}
              />
            </label>
          </>
        ) : null}
      </ConfirmDialog>

      <ConfirmDialog
        open={removing !== null}
        title="حذف الإعلان"
        body="سيُحذف الإعلان نهائياً ويُسجَّل الحذف في سجل التدقيق."
        confirmLabel="تأكيد الحذف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={remove}
        onCancel={() => setRemoving(null)}
      />

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
