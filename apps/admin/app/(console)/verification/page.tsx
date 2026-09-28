"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  EmptyState,
  ConfirmDialog,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
} from "../../../components/ui";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../lib/errors/messages";

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

  return (
    <div className="operation-stack">
      <PageHeader
        eyebrow="العمليات"
        title="متطلبات التحقق"
        description="سياسة الأدلة المطلوبة لكل تصنيف."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-requirement"
              onClick={() => {
                mutation.reset();
                setCreating(true);
              }}
            >
              متطلب جديد
            </button>
          ) : null
        }
      />

      <p className="notice">
        سياسة التحقق تُدار من هنا ولم تُحسم بعد لأي تصنيف. لا تُنشئ متطلباً إلزامياً قبل اعتماده
        تشغيلياً، لأنه يمنع إرسال الطلبات والموافقة عليها.
      </p>

      {requirements.loading ? <LoadingState /> : null}
      {requirements.error ? (
        <ErrorState error={requirements.error} onRetry={requirements.reload} />
      ) : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}

      {requirements.data ? (
        requirements.data.items.length === 0 ? (
          <EmptyState
            title="لا متطلبات مُهيّأة بعد"
            hint="هذه هي حالة الإطلاق المقصودة. أضف متطلباً عندما تُعتمد السياسة."
          />
        ) : (
          <div className="table-wrap">
            <table className="data-table" data-testid="data-table">
              <caption className="sr-only">متطلبات التحقق</caption>
              <thead>
                <tr>
                  <th scope="col">المتطلب</th>
                  <th scope="col">التصنيف</th>
                  <th scope="col">إلزامي</th>
                  <th scope="col">عدد الملفات</th>
                  <th scope="col">الحالة</th>
                  <th scope="col" />
                </tr>
              </thead>
              <tbody>
                {requirements.data.items.map((requirement) => (
                  <tr key={requirement.id}>
                    <td>{requirement.labelAr}</td>
                    <td>{categoryName(requirement.categoryId)}</td>
                    <td>
                      <StatusBadge tone={requirement.required ? "warning" : "neutral"}>
                        {requirement.required ? "إلزامي" : "اختياري"}
                      </StatusBadge>
                    </td>
                    <td className="cell-ltr">
                      {requirement.minFiles}–{requirement.maxFiles}
                    </td>
                    <td>
                      <StatusBadge tone={requirement.active ? "positive" : "neutral"}>
                        {requirement.active ? "مفعّل" : "معطّل"}
                      </StatusBadge>
                    </td>
                    <td>
                      {canManage ? (
                        <div className="button-row">
                          <button
                            type="button"
                            className="button-ghost"
                            data-testid={`toggle-required-${requirement.id}`}
                            onClick={() => toggleRequired(requirement)}
                          >
                            {requirement.required ? "اجعله اختيارياً" : "اجعله إلزامياً"}
                          </button>
                          <button
                            type="button"
                            className="button-ghost"
                            data-testid={`toggle-requirement-${requirement.id}`}
                            onClick={() => toggle(requirement)}
                          >
                            {requirement.active ? "تعطيل" : "تفعيل"}
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
        open={creating}
        title="متطلب تحقق جديد"
        body="سيُطبَّق على كل طلب جديد في التصنيف المحدد. لا يمكن نقله لتصنيف آخر لاحقاً."
        confirmLabel="إضافة"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={create}
        onCancel={() => setCreating(false)}
      >
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
        <label className="field">
          <span>أقل عدد ملفات</span>
          <input
            type="number"
            dir="ltr"
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
            value={draft.maxFiles}
            data-testid="requirement-max"
            aria-invalid={Boolean(errors.maxFiles)}
            onChange={(event) => setDraft({ ...draft, maxFiles: event.target.value })}
          />
          {errors.maxFiles ? <span className="field-error">{errors.maxFiles}</span> : null}
        </label>
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
