"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { Icon } from "../../../components/icons";
import { EmptyState, ErrorState, LoadingState, PageHeader, Toast } from "../../../components/ui";
import { FilterChips, FormDialog, ItemCard } from "../../../components/ui/extra";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../lib/errors/messages";
import { LOCALE } from "../../../lib/locale";

type Requirement = Readonly<{
  id: number;
  categoryId: string;
  labelAr: string;
  labelEn: string;
  required: boolean;
  active: boolean;
  minFiles: number;
  maxFiles: number;
  sortOrder: number;
}>;

type Category = Readonly<{ id: string; nameAr: string }>;

const SHOW = [
  { value: "", label: "الكل" },
  { value: "on", label: "المفعّلة" },
  { value: "off", label: "المعطّلة" },
] as const;
type Show = (typeof SHOW)[number]["value"];

const NUMBER = new Intl.NumberFormat(LOCALE);

/**
 * Verification policy, as something an operator configures.
 *
 * This screen is the tool. It does not state, imply or pre-fill what a pharmacy must
 * supply: that decision belongs to whoever runs the platform and is still open as
 * LAUNCH_POLICY_PENDING. The launch seed deliberately ships no requirement at all.
 *
 * Retiring a requirement is `active = false`, never a delete — evidence already submitted
 * points at it, and removing the row would orphan that evidence.
 */
export default function VerificationPage() {
  const requirements = useResource<{ items: Requirement[] }>("verificationRequirements");
  const categories = useResource<{ items: Category[] }>("categories");
  const mutation = useMutation();
  const canManage = useCan("admin.verification.manage");

  const [creating, setCreating] = useState(false);
  const [show, setShow] = useState<Show>("");
  const [draft, setDraft] = useState({
    categoryId: "",
    labelAr: "",
    required: true,
    minFiles: "1",
    maxFiles: "1",
  });
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);

  const categoryName = (id: string) =>
    categories.data?.items.find((category) => category.id === id)?.nameAr ?? id;

  async function create(): Promise<void> {
    const ok = await mutation.run("verificationCreate", {
      categoryId: draft.categoryId,
      labelAr: draft.labelAr.trim(),
      required: draft.required,
      minFiles: Number(draft.minFiles) || 0,
      maxFiles: Number(draft.maxFiles) || 1,
    });
    if (!ok) return;
    setCreating(false);
    setDraft({ categoryId: "", labelAr: "", required: true, minFiles: "1", maxFiles: "1" });
    setToast("تمت إضافة المتطلب.");
    requirements.reload();
  }

  async function toggle(requirement: Requirement): Promise<void> {
    const ok = await mutation.run("verificationUpdate", {
      id: requirement.id,
      active: !requirement.active,
    });
    if (!ok) return;
    setToast(requirement.active ? "تم تعطيل المتطلب." : "تم تفعيل المتطلب.");
    requirements.reload();
  }

  async function toggleRequired(requirement: Requirement): Promise<void> {
    const ok = await mutation.run("verificationUpdate", {
      id: requirement.id,
      required: !requirement.required,
    });
    if (!ok) return;
    setToast("تم تحديث إلزامية المتطلب.");
    requirements.reload();
  }

  const items = requirements.data?.items ?? [];
  const shown = items.filter((requirement) =>
    show === "on" ? requirement.active : show === "off" ? !requirement.active : true,
  );

  return (
    <div className="stack">
      <PageHeader
        eyebrow="الدليل"
        title="متطلبات التحقق"
        description="سياسة الأدلة المطلوبة لكل تصنيف: ما يرفعه المالك ليُقبل طلبه."
        actions={
          canManage ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-requirement"
              onClick={() => {
                mutation.reset();
                setCreating(true);
              }}
            >
              <Icon name="plus" />
              متطلب جديد
            </button>
          ) : null
        }
      />

      <p className="notice">
        سياسة التحقق تُدار من هنا ولم تُحسم بعد لأي تصنيف. لا تُنشئ متطلباً إلزامياً قبل اعتماده
        تشغيلياً، لأنه يمنع إرسال الطلبات والموافقة عليها.
      </p>

      {items.length > 0 ? (
        <div className="live-toolbar">
          <FilterChips label="عرض" value={show} options={SHOW} onChange={setShow} />
        </div>
      ) : null}

      {requirements.loading ? <LoadingState /> : null}
      {requirements.error ? (
        <ErrorState error={requirements.error} onRetry={requirements.reload} />
      ) : null}
      {mutation.error && !creating ? <ErrorState error={mutation.error} /> : null}

      {requirements.data ? (
        items.length === 0 ? (
          <EmptyState
            title="لا متطلبات مُهيّأة بعد"
            hint="هذه هي حالة الإطلاق المقصودة. أضف متطلباً عندما تُعتمد السياسة."
          />
        ) : shown.length === 0 ? (
          <EmptyState title="لا متطلبات في هذا العرض." illustration="noResults" />
        ) : (
          <ul className="profile-grid" data-testid="requirements">
            {shown.map((requirement, index) => (
              <ItemCard
                key={requirement.id}
                index={index}
                icon="verified"
                tone={!requirement.active ? "neutral" : requirement.required ? "gold" : "info"}
                muted={!requirement.active}
                title={requirement.labelAr}
                subtitle={categoryName(requirement.categoryId)}
                state={
                  !requirement.active
                    ? { label: "معطّل" }
                    : requirement.required
                      ? { label: "إلزامي", tone: "warning" }
                      : { label: "اختياري", tone: "info" }
                }
                facts={[
                  { label: "أقل عدد ملفات", value: NUMBER.format(requirement.minFiles) },
                  { label: "أعلى عدد ملفات", value: NUMBER.format(requirement.maxFiles) },
                  { label: "الترتيب", value: NUMBER.format(requirement.sortOrder) },
                ]}
                testId={`requirement-${requirement.id}`}
                actions={
                  canManage ? (
                    <>
                      <button
                        type="button"
                        className="profile-act-quiet"
                        disabled={mutation.pending}
                        data-testid={`toggle-required-${requirement.id}`}
                        onClick={() => toggleRequired(requirement)}
                      >
                        {requirement.required ? "اجعله اختيارياً" : "اجعله إلزامياً"}
                      </button>
                      <button
                        type="button"
                        className={requirement.active ? "profile-act-danger" : "profile-act-main"}
                        disabled={mutation.pending}
                        data-testid={`toggle-requirement-${requirement.id}`}
                        onClick={() => toggle(requirement)}
                      >
                        {requirement.active ? "تعطيل" : "تفعيل"}
                      </button>
                    </>
                  ) : null
                }
              >
                <p className="item-card-quiet">
                  {requirement.required
                    ? "لا يُرسَل الطلب ولا يُقبل دون هذا الدليل."
                    : "يُطلب من المالك ولا يمنع إرسال الطلب."}
                </p>
              </ItemCard>
            ))}
          </ul>
        )
      ) : null}

      <FormDialog
        open={creating}
        icon="verified"
        title="متطلب تحقق جديد"
        description="يُطبَّق على كل طلب جديد في التصنيف المحدد، ولا يُنقل لتصنيف آخر لاحقاً."
        onClose={() => setCreating(false)}
        locked={mutation.pending}
        testId="requirement-dialog"
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
              data-testid="save-requirement"
              disabled={mutation.pending || !draft.categoryId || !draft.labelAr.trim()}
              onClick={create}
            >
              {mutation.pending ? "جارٍ الإضافة…" : "إضافة المتطلب"}
            </button>
          </div>
        }
      >
        <div className="stack">
          {mutation.error && Object.keys(errors).length === 0 ? (
            <ErrorState error={mutation.error} />
          ) : null}
          <label className="field">
            <span>التصنيف</span>
            <select
              value={draft.categoryId}
              data-testid="requirement-category"
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
          <label className="field">
            <span>وصف المتطلب</span>
            <input
              value={draft.labelAr}
              placeholder="مثال: ترخيص مزاولة المهنة"
              data-testid="requirement-label"
              aria-invalid={Boolean(errors.labelAr)}
              onChange={(event) => setDraft({ ...draft, labelAr: event.target.value })}
            />
            {errors.labelAr ? <span className="field-error">{errors.labelAr}</span> : null}
          </label>
          <label className="switch-row">
            <span>إلزامي</span>
            <input
              type="checkbox"
              data-testid="requirement-required"
              checked={draft.required}
              onChange={(event) => setDraft({ ...draft, required: event.target.checked })}
            />
          </label>
          <div className="form-grid-2">
            <label className="field">
              <span>أقل عدد ملفات</span>
              <input
                type="number"
                dir="ltr"
                min={0}
                value={draft.minFiles}
                data-testid="requirement-min"
                onChange={(event) => setDraft({ ...draft, minFiles: event.target.value })}
              />
            </label>
            <label className="field">
              <span>أعلى عدد ملفات</span>
              <input
                type="number"
                dir="ltr"
                min={1}
                value={draft.maxFiles}
                data-testid="requirement-max"
                aria-invalid={Boolean(errors.maxFiles)}
                onChange={(event) => setDraft({ ...draft, maxFiles: event.target.value })}
              />
              {errors.maxFiles ? <span className="field-error">{errors.maxFiles}</span> : null}
            </label>
          </div>
        </div>
      </FormDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
