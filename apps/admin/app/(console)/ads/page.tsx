"use client";

import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { Icons } from "../../../components/icons";
import { type Period, PeriodPicker, lastDays, periodLabel } from "../../../components/period-picker";
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
import { SlidePreview } from "../../../components/ui/extra";
import { damascusDay } from "../../../lib/client/calendar";
import { uploadAdImage } from "../../../lib/client/files";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { fieldErrorsFor, isSessionExpired, messageFor } from "../../../lib/errors/messages";

type Advertisement = Readonly<{
  id: string;
  titleAr: string;
  targetScope: string;
  provinceId: string | null;
  categoryId: string | null;
  imageUrl: string | null;
  enabled: boolean;
  startsAt: string | null;
  endsAt: string | null;
  sortOrder: number;
  slideDurationMs: number;
}>;

/** How often each advertisement was seen and pressed in the apps, over a period. */
type AdStats = Readonly<{
  fromDate: string;
  toDate: string;
  items: readonly Readonly<{
    adId: string;
    impressions: number;
    clicks: number;
    clickRate: number | null;
  }>[];
}>;

const NUMBER = new Intl.NumberFormat("ar-SY");
const PERCENT = new Intl.NumberFormat("ar-SY", { style: "percent", maximumFractionDigits: 1 });

/** A dash while the numbers load, or for an advertisement created after they were read. */
function count(value: number | undefined): string {
  return value === undefined ? "—" : NUMBER.format(value);
}

/**
 * The period the numbers cover, as the backend read it. The generated client turns its date
 * fields into `Date`s, so they arrive as UTC midnight (`2026-09-04T00:00:00.000Z`); the day is
 * the first ten characters either way.
 */
function covered(stats: AdStats): Period {
  return { from: stats.fromDate.slice(0, 10), to: stats.toDate.slice(0, 10) };
}

/** No views means no rate, not a rate of zero. */
function rate(value: number | null | undefined): string {
  return value == null ? "—" : PERCENT.format(value);
}

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
 * The slide image being edited. `key` is what the save sends (kept in a hidden field);
 * `url` is where the uploaded image is served from; `local` is the picked file itself, shown
 * at once and kept as the fallback if the public media origin cannot be reached.
 */
type SlideImage = Readonly<{
  key: string;
  url: string | null;
  local: string | null;
  uploading: boolean;
  error: string | null;
}>;

const NO_IMAGE: SlideImage = { key: "", url: null, local: null, uploading: false, error: null };

const ACCEPTED = ["image/jpeg", "image/png", "image/webp"];
const MAX_BYTES = 2 * 1024 * 1024;
const MIN_SIDE = 100;
const MAX_SIDE = 4096;

const IMAGE_MESSAGES = {
  format: "الصورة يجب أن تكون بصيغة JPEG أو PNG أو WebP.",
  tooLarge: "حجم الصورة أكبر من ٢ ميغابايت. صغّرها ثم أعد المحاولة.",
  unreadable: "تعذّرت قراءة الملف كصورة. اختر صورة أخرى.",
  dimensions: "طول كل ضلع في الصورة يجب أن يكون بين ١٠٠ و٤٠٩٦ بكسل.",
  required: "اختر صورة الإعلان.",
  uploading: "انتظر حتى يكتمل رفع الصورة.",
};

/** The picked image's pixel size, read by the browser before anything is uploaded. */
function imageSize(url: string): Promise<{ width: number; height: number } | null> {
  return new Promise((resolve) => {
    const probe = new Image();
    probe.onload = () => resolve({ width: probe.naturalWidth, height: probe.naturalHeight });
    probe.onerror = () => resolve(null);
    probe.src = url;
  });
}

/**
 * Advertisement lifecycle: create, edit, schedule, activate, remove.
 *
 * Schedule and targeting are validated by the backend as one set — an end before its start,
 * a global advertisement carrying a province, a slide duration outside its bounds — so the
 * messages shown here are the backend's, not a second copy of the same rules.
 */
export default function AdsPage() {
  const ads = useResource<{ items: Advertisement[] }>("ads");
  const [today] = useState(() => damascusDay(new Date()));
  const [period, setPeriod] = useState<Period>(() => lastDays(30, damascusDay(new Date())));
  const stats = useResource<AdStats>("adStats", period);
  const statOf = new Map((stats.data?.items ?? []).map((item) => [item.adId, item]));
  const provinces = useResource<{ items: { id: string; nameAr: string }[] }>("provinces");
  const categories = useResource<{ items: { id: string; nameAr: string }[] }>("categories");
  const mutation = useMutation();
  const canManage = useCan("admin.ads.manage");
  const router = useRouter();

  const [draft, setDraft] = useState<Draft | null>(null);
  const [image, setImage] = useState<SlideImage>(NO_IMAGE);
  // The object URL of the picked file, so it can be released when replaced, when the
  // editor closes, or when the screen goes away.
  const localUrl = useRef<string | null>(null);
  useEffect(
    () => () => {
      if (localUrl.current) URL.revokeObjectURL(localUrl.current);
    },
    [],
  );
  const [removing, setRemoving] = useState<Advertisement | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);

  function releaseLocal(): void {
    if (localUrl.current) URL.revokeObjectURL(localUrl.current);
    localUrl.current = null;
  }

  /** `currentImage` is the slide's image as published, shown until a new one is picked. */
  function openEditor(next: Draft, currentImage: string | null = null): void {
    mutation.reset();
    releaseLocal();
    setImage(currentImage ? { ...NO_IMAGE, url: currentImage } : NO_IMAGE);
    setDraft(next);
  }

  function closeEditor(): void {
    releaseLocal();
    setImage(NO_IMAGE);
    setDraft(null);
  }

  /**
   * Check the file here first (type, size, pixel size), show it at once, then upload it.
   * A second pick while the first is still uploading wins: the first answer is ignored.
   */
  async function pick(file: File): Promise<void> {
    if (!ACCEPTED.includes(file.type)) {
      setImage((current) => ({ ...current, error: IMAGE_MESSAGES.format }));
      return;
    }
    if (file.size > MAX_BYTES) {
      setImage((current) => ({ ...current, error: IMAGE_MESSAGES.tooLarge }));
      return;
    }
    const local = URL.createObjectURL(file);
    const size = await imageSize(local);
    if (
      !size ||
      size.width < MIN_SIDE ||
      size.height < MIN_SIDE ||
      size.width > MAX_SIDE ||
      size.height > MAX_SIDE
    ) {
      URL.revokeObjectURL(local);
      setImage((current) => ({
        ...current,
        error: size ? IMAGE_MESSAGES.dimensions : IMAGE_MESSAGES.unreadable,
      }));
      return;
    }
    releaseLocal();
    localUrl.current = local;
    setImage({ key: "", url: null, local, uploading: true, error: null });

    const result = await uploadAdImage(file);
    if (!result.ok && isSessionExpired(result.error)) {
      router.replace("/login");
      return;
    }
    // A newer pick, or closing the editor, has replaced this one: its answer no longer applies.
    if (localUrl.current !== local) return;
    if (result.ok) {
      setImage({ key: result.data.imageKey, url: result.data.url, local, uploading: false, error: null });
      return;
    }
    // A refused image leaves the preview, so nothing on screen looks accepted.
    releaseLocal();
    setImage({ ...NO_IMAGE, error: fieldErrorsFor(result.error).file ?? messageFor(result.error) });
  }

  async function save(): Promise<void> {
    if (!draft) return;
    if (image.uploading) {
      setImage({ ...image, error: IMAGE_MESSAGES.uploading });
      return;
    }
    if (!draft.id && !image.key) {
      // Keep the reason a picked image was refused, if there is one; it is the useful message.
      setImage({ ...image, error: image.error ?? IMAGE_MESSAGES.required });
      return;
    }
    const payload: Record<string, unknown> = {
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
    // An edit without a new picture keeps the current one: the key is simply not sent.
    if (image.key) payload.imageKey = image.key;
    const ok = await mutation.run(draft.id ? "adUpdate" : "adCreate", payload);
    if (!ok) return;
    closeEditor();
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
              onClick={() => openEditor({ ...BLANK })}
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
          <>
            <PeriodPicker value={period} today={today} onChange={setPeriod} />
            {stats.error ? <ErrorState error={stats.error} onRetry={stats.reload} /> : null}
            <div className="table-wrap">
              <table className="data-table" data-testid="data-table">
                <caption className="sr-only">
                  الإعلانات
                  {stats.data
                    ? `، وأرقامها للفترة ${periodLabel(covered(stats.data))}`
                    : null}
                </caption>
                <thead>
                  <tr>
                    <th scope="col">العنوان</th>
                    <th scope="col">الاستهداف</th>
                    <th scope="col">من</th>
                    <th scope="col">إلى</th>
                    <th scope="col">الحالة</th>
                    <th scope="col">المشاهدات</th>
                    <th scope="col">النقرات</th>
                    <th scope="col">نسبة النقر</th>
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
                      <td className="cell-ltr" data-testid={`ad-impressions-${ad.id}`}>
                        {count(statOf.get(ad.id)?.impressions)}
                      </td>
                      <td className="cell-ltr">{count(statOf.get(ad.id)?.clicks)}</td>
                      <td className="cell-ltr">{rate(statOf.get(ad.id)?.clickRate)}</td>
                      <td>
                        {canManage ? (
                          <div className="button-row">
                            <button
                              type="button"
                              className="button-ghost"
                              data-testid={`edit-ad-${ad.id}`}
                              onClick={() =>
                                openEditor(
                                  {
                                    ...BLANK,
                                    id: ad.id,
                                    titleAr: ad.titleAr,
                                    targetScope: ad.targetScope,
                                    provinceId: ad.provinceId ?? "",
                                    categoryId: ad.categoryId ?? "",
                                    startsAt: toLocalInput(ad.startsAt),
                                    endsAt: toLocalInput(ad.endsAt),
                                    enabled: ad.enabled,
                                    sortOrder: String(ad.sortOrder),
                                    slideDurationMs: String(ad.slideDurationMs),
                                  },
                                  ad.imageUrl,
                                )
                              }
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
          </>
        )
      ) : null}

      <ConfirmDialog
        open={draft !== null}
        title={draft?.id ? "تعديل الإعلان" : "إعلان جديد"}
        confirmLabel={draft?.id ? "حفظ" : "إضافة"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={save}
        onCancel={closeEditor}
      >
        {draft ? (
          <>
            <div className="field ad-image-field">
              <span className="field-label-row">
                معاينة
                <span className="field-hint">كما يظهر في شريط الإعلانات في التطبيق</span>
              </span>
              <SlidePreview
                src={image.url || image.local}
                fallback={image.local}
                title={draft.titleAr}
                busy={image.uploading}
                emptyLabel={
                  draft.id
                    ? "تبقى الصورة الحالية ما لم تختر صورة جديدة"
                    : "اختر صورة لتظهر هنا كما يراها المستخدم"
                }
              />
              <label className="file-picker" data-busy={image.uploading || undefined}>
                <input
                  type="file"
                  accept={ACCEPTED.join(",")}
                  data-testid="ad-image-file"
                  aria-describedby="ad-image-hint"
                  aria-invalid={Boolean(image.error || errors.imageKey)}
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    // Cleared so choosing the same file again after an error still fires.
                    event.target.value = "";
                    if (file) void pick(file);
                  }}
                />
                <Icons.upload />
                <span>
                  {image.local
                    ? "اختيار صورة أخرى"
                    : draft.id
                      ? "استبدال الصورة"
                      : "اختيار صورة"}
                </span>
              </label>
              <span className="field-hint" id="ad-image-hint">
                صورة أفقية بنسبة ١٦:٩ (مثلاً ١٦٠٠×٩٠٠)، بصيغة JPEG أو PNG أو WebP، حتى ٢ ميغابايت.
              </span>
              {image.error ? (
                <span className="field-error" role="alert" data-testid="ad-image-error">
                  {image.error}
                </span>
              ) : errors.imageKey ? (
                <span className="field-error">{IMAGE_MESSAGES.required}</span>
              ) : null}
              {image.key ? (
                <span className="field-hint ad-image-ready">
                  <Icons.checkCircle />
                  رُفعت الصورة، وتُحفظ مع الإعلان.
                </span>
              ) : null}
              <input type="hidden" name="imageKey" value={image.key} data-testid="ad-image" />
            </div>
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
            <label className="field">
              <span>الترتيب</span>
              <input
                type="number"
                dir="ltr"
                min={0}
                value={draft.sortOrder}
                data-testid="ad-sort"
                aria-invalid={Boolean(errors.sortOrder)}
                onChange={(event) => setDraft({ ...draft, sortOrder: event.target.value })}
              />
              <span className="field-hint">الأصغر يظهر أولاً في الشريط.</span>
              {errors.sortOrder ? <span className="field-error">{errors.sortOrder}</span> : null}
            </label>
            <label className="field">
              <span>مدة العرض (ثوانٍ)</span>
              <input
                type="number"
                dir="ltr"
                min={1}
                step={0.5}
                value={String(Number(draft.slideDurationMs) / 1000 || "")}
                data-testid="ad-duration"
                aria-invalid={Boolean(errors.slideDurationMs)}
                onChange={(event) =>
                  setDraft({
                    ...draft,
                    slideDurationMs: String(Math.round(Number(event.target.value) * 1000)),
                  })
                }
              />
              {errors.slideDurationMs ? (
                <span className="field-error">{errors.slideDurationMs}</span>
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
