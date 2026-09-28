"use client";

import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  ConfirmDialog,
  ErrorState,
  FormSection,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";

type Category = Readonly<{
  id: string;
  groupId: string;
  code: string;
  slug: string;
  nameAr: string;
  nameEn: string;
  iconKey: string;
  specialization: string;
  active: boolean;
  sortOrder: number;
}>;

type Group = Readonly<{ id: string; nameAr: string }>;
type Province = Readonly<{ id: string; nameAr: string; active: boolean }>;

const CAPABILITIES = [
  ["supportsHours", "أوقات الدوام"],
  ["supportsPhotos", "الصور"],
  ["supportsRatings", "التقييمات"],
  ["supportsDuty", "المناوبة"],
  ["supportsSpecialtyFilter", "فلتر الاختصاص"],
  ["supportsServiceFilter", "فلتر الخدمات"],
  ["supportsTemporaryClosure", "الإغلاق المؤقت"],
  ["supportsOwnerOnboarding", "تسجيل المالك"],
] as const;

const SPECIALIZATIONS = [
  ["GENERIC", "عام"],
  ["PHARMACY", "صيدلية"],
  ["MEDICAL_CLINIC", "عيادة"],
  ["NURSING_CENTER", "مركز تمريض"],
] as const;

/**
 * Cycle J, in one screen: create a category, set its capabilities, set its province
 * switches.
 *
 * Two levels are kept visibly apart, because conflating them is the easy mistake here.
 * `active` says whether a category exists as an option at all; a province switch says
 * whether it is visible or open to owners *in one province*. A category can be active
 * everywhere and shown nowhere, which is exactly the launch state.
 *
 * The capability form offers duty for any category. It is not disabled for non-pharmacies
 * on purpose: the backend owns that invariant and refuses, and a UI that silently hid the
 * control would leave an operator wondering why the option they were told about is missing.
 */
export default function TaxonomyCategoriesPage() {
  const categories = useResource<{ items: Category[] }>("categories");
  const groups = useResource<{ items: Group[] }>("categoryGroups");
  const provinces = useResource<{ items: Province[] }>("provinces");
  const mutation = useMutation();
  const canManage = useCan("admin.taxonomy.manage");

  const [selected, setSelected] = useState<Category | null>(null);
  const [capabilities, setCapabilities] = useState<Record<string, boolean>>({});
  const [switchDraft, setSwitchDraft] = useState({
    provinceId: "",
    publicEnabled: false,
    ownerRegistrationEnabled: false,
  });
  const [creating, setCreating] = useState(false);
  const [draft, setDraft] = useState({
    groupId: "",
    code: "",
    slug: "",
    nameAr: "",
    specialization: "GENERIC",
  });
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);

  async function create(): Promise<void> {
    const ok = await mutation.run("categoryCreate", {
      groupId: draft.groupId,
      code: draft.code.trim(),
      slug: draft.slug.trim() || draft.code.trim(),
      nameAr: draft.nameAr.trim(),
      specialization: draft.specialization,
    });
    if (!ok) return;
    setCreating(false);
    setDraft({ groupId: "", code: "", slug: "", nameAr: "", specialization: "GENERIC" });
    setToast("تم إنشاء التصنيف. لن يظهر للعامة حتى تُفعّل مفاتيح المحافظات.");
    categories.reload();
  }

  async function saveCapabilities(): Promise<void> {
    if (!selected) return;
    const ok = await mutation.run("categoryCapabilities", { id: selected.id, ...capabilities });
    if (!ok) return;
    setToast("تم حفظ القدرات.");
  }

  async function saveSwitch(): Promise<void> {
    if (!selected || !switchDraft.provinceId) return;
    const ok = await mutation.run("categoryProvince", { id: selected.id, ...switchDraft });
    if (!ok) return;
    setToast("تم تحديث مفتاح المحافظة.");
  }

  async function toggleActive(category: Category): Promise<void> {
    const ok = await mutation.run("categoryUpdate", {
      id: category.id,
      active: !category.active,
    });
    if (!ok) return;
    setToast(category.active ? "تم تعطيل التصنيف." : "تم تفعيل التصنيف.");
    categories.reload();
  }

  return (
    <div className="stack">
      <PageHeader
        title="التصنيفات"
        description="التصنيفات وقدراتها ومفاتيح ظهورها في كل محافظة."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-category"
              onClick={() => {
                mutation.reset();
                setCreating(true);
              }}
            >
              تصنيف جديد
            </button>
          ) : null
        }
      />

      <p className="notice">
        «مفعّل» يعني أن التصنيف موجود كخيار. الظهور للعامة يعتمد على مفتاح المحافظة، لذلك قد
        يكون التصنيف مفعّلاً وغير ظاهر في أي محافظة.
      </p>

      {categories.loading ? <LoadingState /> : null}
      {categories.error ? (
        <ErrorState error={categories.error} onRetry={categories.reload} />
      ) : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}

      {categories.data ? (
        <div className="table-wrap">
          <table className="data-table" data-testid="data-table">
            <caption className="sr-only">التصنيفات</caption>
            <thead>
              <tr>
                <th scope="col">الاسم</th>
                <th scope="col">الرمز</th>
                <th scope="col">الاختصاص</th>
                <th scope="col">الحالة</th>
                <th scope="col" />
              </tr>
            </thead>
            <tbody>
              {categories.data.items.map((category) => (
                <tr key={category.id}>
                  <td>{category.nameAr}</td>
                  <td className="cell-ltr">
                    <code>{category.code}</code>
                  </td>
                  <td>
                    {SPECIALIZATIONS.find(([value]) => value === category.specialization)?.[1] ??
                      category.specialization}
                  </td>
                  <td>
                    <StatusBadge tone={category.active ? "positive" : "neutral"}>
                      {category.active ? "مفعّل" : "معطّل"}
                    </StatusBadge>
                  </td>
                  <td>
                    {canManage ? (
                      <div className="button-row">
                        <button
                          type="button"
                          className="button-ghost"
                          data-testid={`configure-${category.code}`}
                          onClick={() => {
                            mutation.reset();
                            setSelected(category);
                            setCapabilities({});
                            setSwitchDraft({
                              provinceId: "",
                              publicEnabled: false,
                              ownerRegistrationEnabled: false,
                            });
                          }}
                        >
                          تهيئة
                        </button>
                        <button
                          type="button"
                          className="button-ghost"
                          data-testid={`toggle-${category.code}`}
                          onClick={() => toggleActive(category)}
                        >
                          {category.active ? "تعطيل" : "تفعيل"}
                        </button>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {selected ? (
        <>
          <FormSection
            title={`قدرات: ${selected.nameAr}`}
            description="القدرات غير المذكورة تبقى كما هي. المناوبة مسموحة للصيدليات فقط، ويرفضها الخادم لغيرها."
            footer={
              <>
                <button
                  type="button"
                  className="button-primary"
                  disabled={mutation.pending}
                  data-testid="save-capabilities"
                  onClick={saveCapabilities}
                >
                  حفظ القدرات
                </button>
                <button type="button" className="button-ghost" onClick={() => setSelected(null)}>
                  إغلاق
                </button>
              </>
            }
          >
            {CAPABILITIES.map(([key, label]) => (
              <label key={key} className="switch-row">
                <span>{label}</span>
                <input
                  type="checkbox"
                  data-testid={`cap-${key}`}
                  checked={capabilities[key] ?? false}
                  onChange={(event) =>
                    setCapabilities({ ...capabilities, [key]: event.target.checked })
                  }
                />
              </label>
            ))}
          </FormSection>

          <FormSection
            title={`مفاتيح المحافظات: ${selected.nameAr}`}
            description="يتحكم هذا في الظهور العام وفتح تسجيل المالكين داخل محافظة واحدة."
            footer={
              <button
                type="button"
                className="button-primary"
                disabled={mutation.pending || !switchDraft.provinceId}
                data-testid="save-switch"
                onClick={saveSwitch}
              >
                حفظ المفتاح
              </button>
            }
          >
            <label className="field">
              <span>المحافظة</span>
              <select
                value={switchDraft.provinceId}
                data-testid="switch-province"
                onChange={(event) =>
                  setSwitchDraft({ ...switchDraft, provinceId: event.target.value })
                }
              >
                <option value="">اختر محافظة</option>
                {(provinces.data?.items ?? []).map((province) => (
                  <option key={province.id} value={province.id}>
                    {province.nameAr}
                  </option>
                ))}
              </select>
            </label>
            <label className="switch-row">
              <span>ظاهر للعامة</span>
              <input
                type="checkbox"
                data-testid="switch-public"
                checked={switchDraft.publicEnabled}
                onChange={(event) =>
                  setSwitchDraft({ ...switchDraft, publicEnabled: event.target.checked })
                }
              />
            </label>
            <label className="switch-row">
              <span>تسجيل المالكين مفتوح</span>
              <input
                type="checkbox"
                data-testid="switch-onboarding"
                checked={switchDraft.ownerRegistrationEnabled}
                onChange={(event) =>
                  setSwitchDraft({
                    ...switchDraft,
                    ownerRegistrationEnabled: event.target.checked,
                  })
                }
              />
            </label>
          </FormSection>
        </>
      ) : null}

      <ConfirmDialog
        open={creating}
        title="تصنيف جديد"
        body="الرمز والمُعرّف النصي يُحدَّدان مرة واحدة ولا يمكن تغييرهما بعد الإنشاء."
        confirmLabel="إنشاء"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={create}
        onCancel={() => setCreating(false)}
      >
        <label className="field">
          <span>المجموعة</span>
          <select
            value={draft.groupId}
            data-testid="category-group"
            onChange={(event) => setDraft({ ...draft, groupId: event.target.value })}
          >
            <option value="">اختر مجموعة</option>
            {(groups.data?.items ?? []).map((group) => (
              <option key={group.id} value={group.id}>
                {group.nameAr}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>الرمز</span>
          <input
            dir="ltr"
            value={draft.code}
            data-testid="category-code"
            aria-invalid={Boolean(errors.code)}
            onChange={(event) => setDraft({ ...draft, code: event.target.value })}
          />
          {errors.code ? <span className="field-error">{errors.code}</span> : null}
        </label>
        <label className="field">
          <span>الاسم بالعربية</span>
          <input
            value={draft.nameAr}
            data-testid="category-name-ar"
            onChange={(event) => setDraft({ ...draft, nameAr: event.target.value })}
          />
        </label>
        <label className="field">
          <span>الاختصاص</span>
          <select
            value={draft.specialization}
            data-testid="category-specialization"
            onChange={(event) => setDraft({ ...draft, specialization: event.target.value })}
          >
            {SPECIALIZATIONS.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
