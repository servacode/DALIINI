"use client";

import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { type IconName, Icon, Icons } from "../../../../components/icons";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Toast,
} from "../../../../components/ui";
import {
  FilterChips,
  FormDialog,
  ItemCard,
  ItemChips,
  SearchBox,
} from "../../../../components/ui/extra";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

type Switch = Readonly<{
  provinceId: string;
  provinceNameAr: string;
  publicEnabled: boolean;
  ownerRegistrationEnabled: boolean;
}>;

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
  capabilities: Readonly<Record<string, boolean>>;
  switches: readonly Switch[];
  facilityCount: number;
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

const SHOW = [
  { value: "", label: "الكل" },
  { value: "public", label: "ظاهرة للعامة" },
  { value: "hidden", label: "غير ظاهرة" },
  { value: "off", label: "معطّلة" },
] as const;
type Show = (typeof SHOW)[number]["value"];

const NUMBER = new Intl.NumberFormat(LOCALE);
const EMPTY_SWITCH = { provinceId: "", publicEnabled: false, ownerRegistrationEnabled: false };

function iconFor(category: Category): IconName {
  if (category.iconKey && category.iconKey in Icons) return category.iconKey as IconName;
  if (category.specialization === "PHARMACY") return "pharmacy";
  if (category.specialization === "MEDICAL_CLINIC") return "clinic";
  return "tag";
}

function specializationLabel(value: string): string {
  return SPECIALIZATIONS.find(([key]) => key === value)?.[1] ?? value;
}

/**
 * Cycle J, in one screen: create a category, set its capabilities, set its province
 * switches — each category a card, its settings in one window (DECISION-113).
 *
 * Two levels are kept visibly apart, because conflating them is the easy mistake here.
 * `active` says whether a category exists as an option at all; a province switch says
 * whether it is visible or open to owners *in one province*. A category can be active
 * everywhere and shown nowhere, which is exactly the launch state — and its card says so in
 * gold rather than green.
 *
 * The window opens on the category's flags and switches as they are now. It used to open
 * blank, so saving it unseen could switch off what was on.
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

  const [q, setQ] = useState("");
  const [show, setShow] = useState<Show>("");
  const [selected, setSelected] = useState<Category | null>(null);
  const [capabilities, setCapabilities] = useState<Record<string, boolean>>({});
  const [switchDraft, setSwitchDraft] = useState(EMPTY_SWITCH);
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

  const all = categories.data?.items ?? [];
  const needle = q.trim();
  const shown = all.filter((category) => {
    if (needle && !category.nameAr.includes(needle) && !category.code.includes(needle)) {
      return false;
    }
    const visible = category.switches.some((s) => s.publicEnabled);
    if (show === "public") return category.active && visible;
    if (show === "hidden") return category.active && !visible;
    if (show === "off") return !category.active;
    return true;
  });

  function configure(category: Category): void {
    mutation.reset();
    setSelected(category);
    setCapabilities({ ...category.capabilities });
    setSwitchDraft(EMPTY_SWITCH);
  }

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
    categories.reload();
  }

  async function saveSwitch(): Promise<void> {
    if (!selected || !switchDraft.provinceId) return;
    const ok = await mutation.run("categoryProvince", { id: selected.id, ...switchDraft });
    if (!ok) return;
    setToast("تم تحديث مفتاح المحافظة.");
    categories.reload();
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

  // The switches as they are now, with the window's unsaved edits shown in their place, so the
  // list under the form never disagrees with what was just saved.
  const current =
    selected === null ? [] : (all.find((c) => c.id === selected.id)?.switches ?? selected.switches);

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الدليل"
        title="التصنيفات"
        description="التصنيفات وقدراتها ومفاتيح ظهورها في كل محافظة."
        actions={
          canManage ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-category"
              onClick={() => {
                mutation.reset();
                setCreating(true);
              }}
            >
              <Icon name="plus" />
              تصنيف جديد
            </button>
          ) : null
        }
      />

      <div className="live-toolbar">
        <SearchBox value={q} onChange={setQ} placeholder="ابحث باسم تصنيف" delay={0} />
        <FilterChips label="عرض" value={show} options={SHOW} onChange={setShow} />
      </div>

      <p className="notice">
        «مفعّل» يعني أن التصنيف موجود كخيار. الظهور للعامة يعتمد على مفتاح المحافظة، لذلك قد
        يكون التصنيف مفعّلاً وغير ظاهر في أي محافظة.
      </p>

      {categories.loading ? <LoadingState /> : null}
      {categories.error ? (
        <ErrorState error={categories.error} onRetry={categories.reload} />
      ) : null}
      {mutation.error && !selected && !creating ? <ErrorState error={mutation.error} /> : null}

      {categories.data && shown.length === 0 ? (
        <EmptyState title="لا تصنيف يطابق هذا العرض." illustration="noResults" />
      ) : null}

      {shown.length > 0 ? (
        <ul className="profile-grid" data-testid="categories">
          {shown.map((category, index) => {
            const visible = category.switches.filter((s) => s.publicEnabled).length;
            const owners = category.switches.filter((s) => s.ownerRegistrationEnabled).length;
            const enabled = CAPABILITIES.filter(([key]) => category.capabilities[key]).map(
              ([, label]) => label,
            );
            return (
              <ItemCard
                key={category.id}
                index={index}
                icon={iconFor(category)}
                tone={!category.active ? "neutral" : visible > 0 ? "brand" : "gold"}
                muted={!category.active}
                title={category.nameAr}
                subtitle={specializationLabel(category.specialization)}
                state={
                  !category.active
                    ? { label: "معطّل" }
                    : visible > 0
                      ? { label: "ظاهر للعامة", tone: "positive" }
                      : { label: "غير ظاهر بعد", tone: "warning" }
                }
                facts={[
                  { label: "المنشآت", value: NUMBER.format(category.facilityCount) },
                  { label: "ظاهر في", value: `${NUMBER.format(visible)} محافظة` },
                  { label: "تسجيل المالكين", value: `${NUMBER.format(owners)} محافظة` },
                ]}
                testId={`category-${category.code}`}
                actions={
                  canManage ? (
                    <>
                      <button
                        type="button"
                        className="profile-act-main"
                        data-testid={`configure-${category.code}`}
                        onClick={() => configure(category)}
                      >
                        <Icon name="settings" width={16} height={16} />
                        الإعدادات
                      </button>
                      <button
                        type="button"
                        className={category.active ? "profile-act-danger" : "profile-act-quiet"}
                        data-testid={`toggle-${category.code}`}
                        disabled={mutation.pending}
                        onClick={() => toggleActive(category)}
                      >
                        {category.active ? "تعطيل" : "تفعيل"}
                      </button>
                    </>
                  ) : null
                }
              >
                <span className="item-card-quiet">القدرات</span>
                <ItemChips items={enabled} max={4} empty="لا قدرات مفعّلة." />
              </ItemCard>
            );
          })}
        </ul>
      ) : null}

      <FormDialog
        open={selected !== null}
        wide
        icon={selected ? iconFor(selected) : "tag"}
        title={selected ? `إعدادات «${selected.nameAr}»` : ""}
        description="ما يدعمه التصنيف، وأين يظهر للعامة ويُفتح للمالكين."
        onClose={() => setSelected(null)}
        locked={mutation.pending}
        testId="category-settings"
        footer={
          <button type="button" className="button-ghost" onClick={() => setSelected(null)}>
            إغلاق
          </button>
        }
      >
        {selected ? (
          <div className="stack">
            {mutation.error ? <ErrorState error={mutation.error} /> : null}
            <div className="settings-columns">
              <section className="settings-block">
                <header>
                  <strong>القدرات</strong>
                  <span>المناوبة للصيدليات فقط، ويرفضها الخادم لغيرها.</span>
                </header>
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
                <div className="button-row">
                  <button
                    type="button"
                    className="button-primary"
                    disabled={mutation.pending}
                    data-testid="save-capabilities"
                    onClick={saveCapabilities}
                  >
                    حفظ القدرات
                  </button>
                </div>
              </section>

              <section className="settings-block">
                <header>
                  <strong>مفاتيح المحافظات</strong>
                  <span>الظهور للعامة وفتح تسجيل المالكين، في كل محافظة على حدة.</span>
                </header>
                <label className="field">
                  <span>المحافظة</span>
                  <select
                    value={switchDraft.provinceId}
                    data-testid="switch-province"
                    onChange={(event) => {
                      const id = event.target.value;
                      const now = current.find((s) => s.provinceId === id);
                      setSwitchDraft({
                        provinceId: id,
                        publicEnabled: now?.publicEnabled ?? false,
                        ownerRegistrationEnabled: now?.ownerRegistrationEnabled ?? false,
                      });
                    }}
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
                    disabled={!switchDraft.provinceId}
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
                    disabled={!switchDraft.provinceId}
                    checked={switchDraft.ownerRegistrationEnabled}
                    onChange={(event) =>
                      setSwitchDraft({
                        ...switchDraft,
                        ownerRegistrationEnabled: event.target.checked,
                      })
                    }
                  />
                </label>
                <div className="button-row">
                  <button
                    type="button"
                    className="button-primary"
                    disabled={mutation.pending || !switchDraft.provinceId}
                    data-testid="save-switch"
                    onClick={saveSwitch}
                  >
                    حفظ المفتاح
                  </button>
                </div>

                <ul className="plain-list switch-summary">
                  {current.length === 0 ? (
                    <li className="item-card-quiet">لم يُفتح في أي محافظة بعد.</li>
                  ) : (
                    current.map((s) => (
                      <li key={s.provinceId} className="plain-row">
                        <span>{s.provinceNameAr}</span>
                        <span className="switch-summary-pills">
                          <span data-on={s.publicEnabled}>
                            {s.publicEnabled ? "ظاهر" : "مخفي"}
                          </span>
                          <span data-on={s.ownerRegistrationEnabled}>
                            {s.ownerRegistrationEnabled ? "التسجيل مفتوح" : "التسجيل مغلق"}
                          </span>
                        </span>
                      </li>
                    ))
                  )}
                </ul>
              </section>
            </div>
          </div>
        ) : null}
      </FormDialog>

      <FormDialog
        open={creating}
        icon="tag"
        title="تصنيف جديد"
        description="الرمز يُحدَّد مرة واحدة ولا يمكن تغييره بعد الإنشاء."
        onClose={() => setCreating(false)}
        locked={mutation.pending}
        testId="category-new"
        footer={
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              disabled={mutation.pending}
              onClick={() => setCreating(false)}
            >
              إلغاء
            </button>
            <button
              type="button"
              className="button-primary"
              data-testid="save-category"
              disabled={
                mutation.pending || !draft.groupId || !draft.code.trim() || !draft.nameAr.trim()
              }
              onClick={create}
            >
              {mutation.pending ? "جارٍ الإنشاء…" : "إنشاء التصنيف"}
            </button>
          </div>
        }
      >
        <div className="stack">
          {mutation.error && Object.keys(errors).length === 0 ? (
            <ErrorState error={mutation.error} />
          ) : null}
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
            <span>الاسم بالعربية</span>
            <input
              value={draft.nameAr}
              placeholder="مثال: بصريات"
              data-testid="category-name-ar"
              onChange={(event) => setDraft({ ...draft, nameAr: event.target.value })}
            />
          </label>
          <label className="field">
            <span>الرمز</span>
            <input
              dir="ltr"
              value={draft.code}
              placeholder="optics"
              data-testid="category-code"
              aria-invalid={Boolean(errors.code)}
              onChange={(event) => setDraft({ ...draft, code: event.target.value })}
            />
            <span className="field-hint">حروف إنكليزية صغيرة وشرطات، يُستعمل في الروابط.</span>
            {errors.code ? <span className="field-error">{errors.code}</span> : null}
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
        </div>
      </FormDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
