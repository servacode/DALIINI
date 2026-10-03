"use client";

import { useState } from "react";

import { useMutation } from "../lib/client/use-mutation";
import { useResource } from "../lib/client/use-resource";
import { fieldErrorsFor, messageFor } from "../lib/errors/messages";
import { FormSection } from "./ui";

type Named = Readonly<{ id: string; nameAr: string; active?: boolean }>;
type Tag = Readonly<{ id: number; nameAr: string; active: boolean }>;

/** The facility as the console reads it back (`adminFacilityRetrieve`). */
export type EditableFacility = Readonly<{
  id: string;
  nameAr: string;
  nameEn: string | null;
  status: string;
  categoryId: string;
  provinceId: string;
  cityId: string | null;
  descriptionAr: string | null;
  descriptionEn: string | null;
  phone: string | null;
  whatsapp: string | null;
  addressAr: string | null;
  addressEn: string | null;
  location: { latitude: number; longitude: number } | null;
  specialtyIds: readonly number[];
  serviceTagIds: readonly number[];
}>;

type Draft = {
  categoryId: string;
  provinceId: string;
  cityId: string;
  nameAr: string;
  nameEn: string;
  descriptionAr: string;
  phone: string;
  whatsapp: string;
  addressAr: string;
  latitude: string;
  longitude: string;
  specialtyIds: number[];
  serviceTagIds: number[];
  status: "ACTIVE" | "DRAFT";
};

function draftOf(facility: EditableFacility | null): Draft {
  return {
    categoryId: facility?.categoryId ?? "",
    provinceId: facility?.provinceId ?? "",
    cityId: facility?.cityId ?? "",
    nameAr: facility?.nameAr ?? "",
    nameEn: facility?.nameEn ?? "",
    descriptionAr: facility?.descriptionAr ?? "",
    phone: facility?.phone ?? "",
    whatsapp: facility?.whatsapp ?? "",
    addressAr: facility?.addressAr ?? "",
    latitude: facility?.location ? String(facility.location.latitude) : "",
    longitude: facility?.location ? String(facility.location.longitude) : "",
    specialtyIds: [...(facility?.specialtyIds ?? [])],
    serviceTagIds: [...(facility?.serviceTagIds ?? [])],
    status: "ACTIVE",
  };
}

/** The request body: every field on create, only what changed on edit. */
function bodyOf(draft: Draft, original: Draft | null): Record<string, unknown> | string {
  const lat = draft.latitude.trim();
  const lng = draft.longitude.trim();
  if (Boolean(lat) !== Boolean(lng)) return "أدخل خط العرض وخط الطول معاً، أو اتركهما فارغين.";
  const location = lat ? { latitude: Number(lat), longitude: Number(lng) } : null;
  if (location && (Number.isNaN(location.latitude) || Number.isNaN(location.longitude))) {
    return "الإحداثيات أرقام عشرية، مثل 35.9506 و 39.0094.";
  }
  const full: Record<string, unknown> = {
    categoryId: draft.categoryId,
    provinceId: draft.provinceId,
    cityId: draft.cityId || null,
    nameAr: draft.nameAr.trim(),
    nameEn: draft.nameEn.trim(),
    descriptionAr: draft.descriptionAr.trim(),
    phone: draft.phone.trim(),
    whatsapp: draft.whatsapp.trim(),
    addressAr: draft.addressAr.trim(),
    location,
    specialtyIds: draft.specialtyIds,
    serviceTagIds: draft.serviceTagIds,
  };
  if (!original) return { ...full, status: draft.status };
  const before = bodyOf(original, null) as Record<string, unknown>;
  return Object.fromEntries(
    Object.entries(full).filter(([key, value]) => JSON.stringify(value) !== JSON.stringify(before[key])),
  );
}

/**
 * Add a facility, or correct one. The same rules as an owner's edit hold server-side; what
 * differs is that an operator's correction does not send a live facility back for review.
 */
export function FacilityForm({
  facility,
  onSaved,
}: {
  facility: EditableFacility | null;
  onSaved: (id: string) => void;
}) {
  const original = facility ? draftOf(facility) : null;
  const [draft, setDraft] = useState<Draft>(() => draftOf(facility));
  const [local, setLocal] = useState<string | null>(null);
  const mutation = useMutation();
  const errors = fieldErrorsFor(mutation.error);

  const provinces = useResource<{ items: Named[] }>("provinces");
  const categories = useResource<{ items: Named[] }>("categories");
  const cities = useResource<{ items: Named[] }>(
    "provinceCities",
    { id: draft.provinceId },
    { enabled: Boolean(draft.provinceId) },
  );
  const specialties = useResource<{ items: Tag[] }>(
    "categorySpecialties",
    { id: draft.categoryId },
    { enabled: Boolean(draft.categoryId) },
  );
  const services = useResource<{ items: Tag[] }>(
    "categoryServiceTags",
    { id: draft.categoryId },
    { enabled: Boolean(draft.categoryId) },
  );

  function set<K extends keyof Draft>(key: K, value: Draft[K]): void {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  function toggle(key: "specialtyIds" | "serviceTagIds", id: number): void {
    setDraft((current) => ({
      ...current,
      [key]: current[key].includes(id)
        ? current[key].filter((value) => value !== id)
        : [...current[key], id],
    }));
  }

  async function save(): Promise<void> {
    setLocal(null);
    const body = bodyOf(draft, original);
    if (typeof body === "string") {
      setLocal(body);
      return;
    }
    if (facility && Object.keys(body).length === 0) {
      setLocal("لم يتغير شيء.");
      return;
    }
    const answer = await mutation.runFor<{ id: string }>(
      facility ? "facilityUpdate" : "facilityCreate",
      facility ? { id: facility.id, ...body } : body,
    );
    if (answer) onSaved(answer.id);
  }

  const field = (
    key: keyof Draft,
    label: string,
    options: { ltr?: boolean; multiline?: boolean; hint?: string; testId?: string } = {},
  ) => (
    <label className="field">
      <span>{label}</span>
      {options.multiline ? (
        <textarea
          rows={3}
          value={String(draft[key])}
          aria-invalid={Boolean(errors[key])}
          data-testid={options.testId}
          onChange={(event) => set(key, event.target.value as never)}
        />
      ) : (
        <input
          dir={options.ltr ? "ltr" : undefined}
          value={String(draft[key])}
          aria-invalid={Boolean(errors[key])}
          data-testid={options.testId}
          onChange={(event) => set(key, event.target.value as never)}
        />
      )}
      {options.hint ? <span className="field-hint">{options.hint}</span> : null}
      {errors[key] ? <span className="field-error">{errors[key]}</span> : null}
    </label>
  );

  const activeTags = (items: readonly Tag[] | undefined, chosen: readonly number[]) =>
    (items ?? []).filter((item) => item.active || chosen.includes(item.id));

  const submitLabel = facility ? "حفظ التعديلات" : "إضافة المنشأة";

  return (
    <div className="stack" data-testid="facility-form">
      <FormSection title="التصنيف والمكان">
        <label className="field">
          <span>التصنيف</span>
          <select
            value={draft.categoryId}
            data-testid="facility-category"
            aria-invalid={Boolean(errors.categoryId)}
            onChange={(event) => {
              const categoryId = event.target.value;
              // Specialties and services belong to a category; another one starts them empty.
              setDraft((current) => ({ ...current, categoryId, specialtyIds: [], serviceTagIds: [] }));
            }}
          >
            <option value="">اختر التصنيف</option>
            {(categories.data?.items ?? []).map((item) => (
              <option key={item.id} value={item.id}>
                {item.nameAr}
              </option>
            ))}
          </select>
          {errors.categoryId ? <span className="field-error">{errors.categoryId}</span> : null}
        </label>
        <label className="field">
          <span>المحافظة</span>
          <select
            value={draft.provinceId}
            data-testid="facility-province"
            aria-invalid={Boolean(errors.provinceId)}
            onChange={(event) =>
              setDraft((current) => ({ ...current, provinceId: event.target.value, cityId: "" }))
            }
          >
            <option value="">اختر المحافظة</option>
            {(provinces.data?.items ?? []).map((item) => (
              <option key={item.id} value={item.id}>
                {item.nameAr}
              </option>
            ))}
          </select>
          {errors.provinceId ? <span className="field-error">{errors.provinceId}</span> : null}
        </label>
        <label className="field">
          <span>المدينة</span>
          <select
            value={draft.cityId}
            disabled={!draft.provinceId}
            data-testid="facility-city"
            onChange={(event) => set("cityId", event.target.value)}
          >
            <option value="">بلا مدينة</option>
            {(cities.data?.items ?? [])
              .filter((item) => item.active !== false || item.id === draft.cityId)
              .map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nameAr}
                </option>
              ))}
          </select>
          {errors.cityId ? <span className="field-error">{errors.cityId}</span> : null}
        </label>
        {facility ? null : (
          <label className="field">
            <span>الظهور</span>
            <select
              value={draft.status}
              data-testid="facility-status"
              onChange={(event) => set("status", event.target.value as Draft["status"])}
            >
              <option value="ACTIVE">تظهر فوراً في الدليل</option>
              <option value="DRAFT">مسودة مخفية</option>
            </select>
          </label>
        )}
      </FormSection>

      <FormSection title="الاسم والوصف">
        {field("nameAr", "الاسم بالعربية", { testId: "facility-name-ar" })}
        {field("nameEn", "الاسم بالإنجليزية", { ltr: true })}
        {field("descriptionAr", "الوصف", { multiline: true })}
      </FormSection>

      <FormSection title="التواصل والعنوان">
        {field("phone", "الهاتف", { ltr: true, testId: "facility-phone" })}
        {field("whatsapp", "واتساب", { ltr: true, hint: "رقم جوال سوري، مثل 0991234567." })}
        {field("addressAr", "العنوان", { testId: "facility-address" })}
      </FormSection>

      <FormSection
        title="الموقع على الخريطة"
        description="من أي خريطة: اضغط مطولاً على المكان وانسخ الإحداثيتين. اتركهما فارغين لإزالة الموقع."
      >
        {field("latitude", "خط العرض", { ltr: true, hint: "مثل 35.9506" })}
        {field("longitude", "خط الطول", { ltr: true, hint: "مثل 39.0094" })}
        {errors.location ? <span className="field-error">{errors.location}</span> : null}
      </FormSection>

      {activeTags(specialties.data?.items, draft.specialtyIds).length > 0 ||
      activeTags(services.data?.items, draft.serviceTagIds).length > 0 ? (
        <FormSection title="الاختصاصات والخدمات">
          {(
            [
              ["specialtyIds", "الاختصاصات", specialties.data?.items],
              ["serviceTagIds", "الخدمات", services.data?.items],
            ] as const
          ).map(([key, label, items]) =>
            activeTags(items, draft[key]).length > 0 ? (
              <fieldset key={key} className="choice-group">
                <legend>{label}</legend>
                {activeTags(items, draft[key]).map((item) => (
                  <label key={item.id} className="switch-inline">
                    <input
                      type="checkbox"
                      checked={draft[key].includes(item.id)}
                      onChange={() => toggle(key, item.id)}
                    />
                    <span>{item.nameAr}</span>
                  </label>
                ))}
                {errors[key] ? <span className="field-error">{errors[key]}</span> : null}
              </fieldset>
            ) : null,
          )}
        </FormSection>
      ) : null}

      {local || (mutation.error && Object.keys(errors).length === 0) ? (
        <p className="field-error" role="alert" data-testid="facility-form-error">
          {local ?? messageFor(mutation.error)}
        </p>
      ) : null}
      <div className="button-row">
        <button
          type="button"
          className="button-primary"
          disabled={mutation.pending || !draft.categoryId || !draft.provinceId || !draft.nameAr.trim()}
          data-testid="facility-save"
          onClick={save}
        >
          {mutation.pending ? "جارٍ الحفظ…" : submitLabel}
        </button>
      </div>
    </div>
  );
}
