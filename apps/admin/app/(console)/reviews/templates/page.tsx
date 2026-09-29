"use client";

import { useCallback, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import type { RejectionTemplate } from "../../../../components/rejection-template-picker";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  TermBadge,
  Toast,
} from "../../../../components/ui";
import { CharCount } from "../../../../components/ui/extra";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";

type Draft = Readonly<{
  id?: string;
  titleAr: string;
  bodyAr: string;
  sortOrder: string;
  active: boolean;
}>;

const TITLE_MAX = 120;
const BODY_MAX = 1000;
const NUMBER = new Intl.NumberFormat("ar-SY");

/**
 * Ready-made rejection reasons.
 *
 * A reviewer picks one in the reject dialog and edits it before sending, so the common
 * cases (an unreadable licence, a location that does not match) read the same whichever
 * reviewer wrote them. A template is only a starting point: editing or deleting one never
 * changes a rejection already sent. Inactive templates stay here but are not offered.
 */
export default function RejectionTemplatesPage() {
  const templates = useResource<{ items: RejectionTemplate[] }>("rejectionTemplates");
  const canManage = useCan("admin.reviews.decide");
  const mutation = useMutation();

  const [draft, setDraft] = useState<Draft | null>(null);
  const [removing, setRemoving] = useState<RejectionTemplate | null>(null);
  const [problem, setProblem] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const dismissToast = useCallback(() => setToast(null), []);

  const items = [...(templates.data?.items ?? [])].sort(
    (a, b) => a.sortOrder - b.sortOrder || a.titleAr.localeCompare(b.titleAr, "ar"),
  );
  const server = fieldErrorsFor(mutation.error);
  const titleLength = draft ? Array.from(draft.titleAr.trim()).length : 0;
  const bodyLength = draft ? Array.from(draft.bodyAr.trim()).length : 0;

  async function save(): Promise<void> {
    if (!draft) return;
    if (titleLength === 0 || bodyLength === 0) {
      setProblem("اكتب عنوان القالب ونص السبب.");
      return;
    }
    if (titleLength > TITLE_MAX || bodyLength > BODY_MAX) {
      setProblem(
        `العنوان ${NUMBER.format(TITLE_MAX)} حرفاً على الأكثر، والنص ${NUMBER.format(BODY_MAX)} حرف على الأكثر.`,
      );
      return;
    }
    setProblem(null);
    const payload = {
      titleAr: draft.titleAr.trim(),
      bodyAr: draft.bodyAr.trim(),
      sortOrder: Math.max(0, Math.round(Number(draft.sortOrder) || 0)),
      active: draft.active,
    };
    const ok = await mutation.run(
      draft.id ? "rejectionTemplateUpdate" : "rejectionTemplateCreate",
      draft.id ? { id: draft.id, ...payload } : payload,
    );
    if (!ok) return;
    setToast(draft.id ? "حُفظ القالب." : "أُضيف القالب.");
    setDraft(null);
    templates.reload();
  }

  async function toggle(template: RejectionTemplate): Promise<void> {
    const ok = await mutation.run("rejectionTemplateUpdate", {
      id: template.id,
      active: !template.active,
    });
    if (!ok) return;
    setToast(template.active ? "عُطّل القالب، ولن يُعرض على المراجعين." : "فُعّل القالب.");
    templates.reload();
  }

  async function remove(): Promise<void> {
    if (!removing) return;
    const ok = await mutation.run("rejectionTemplateDelete", { id: removing.id });
    if (!ok) return;
    setRemoving(null);
    setToast("حُذف القالب.");
    templates.reload();
  }

  const columns: readonly Column<RejectionTemplate>[] = [
    {
      key: "order",
      header: "الترتيب",
      ltr: true,
      width: "1%",
      render: (row) => NUMBER.format(row.sortOrder),
    },
    {
      key: "template",
      header: "القالب",
      render: (row) => (
        <span className="cell-stack">
          <strong>{row.titleAr}</strong>
          <span className="muted text-clamp-2">{row.bodyAr}</span>
        </span>
      ),
    },
    {
      key: "active",
      header: "الحالة",
      render: (row) => <TermBadge group="switch" value={row.active ? "ON" : "OFF"} />,
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) =>
        canManage ? (
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              data-testid={`edit-template-${row.id}`}
              onClick={() => {
                mutation.reset();
                setProblem(null);
                setDraft({
                  id: row.id,
                  titleAr: row.titleAr,
                  bodyAr: row.bodyAr,
                  sortOrder: String(row.sortOrder),
                  active: row.active,
                });
              }}
            >
              تعديل
            </button>
            <button
              type="button"
              className="button-ghost"
              disabled={mutation.pending}
              data-testid={`toggle-template-${row.id}`}
              onClick={() => toggle(row)}
            >
              {row.active ? "تعطيل" : "تفعيل"}
            </button>
            <button
              type="button"
              className="button-ghost"
              data-tone="danger"
              data-testid={`delete-template-${row.id}`}
              onClick={() => {
                mutation.reset();
                setRemoving(row);
              }}
            >
              حذف
            </button>
          </div>
        ) : null,
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="قوالب أسباب الرفض"
        description="نصوص جاهزة يختار منها المراجع عند رفض طلب، ثم يعدّلها إن لزم."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-template"
              onClick={() => {
                mutation.reset();
                setProblem(null);
                setDraft({
                  titleAr: "",
                  bodyAr: "",
                  sortOrder: String((items.at(-1)?.sortOrder ?? 0) + 10),
                  active: true,
                });
              }}
            >
              قالب جديد
            </button>
          ) : null
        }
      />

      {templates.loading ? <LoadingState /> : null}
      {templates.error ? <ErrorState error={templates.error} onRetry={templates.reload} /> : null}
      {mutation.error && !draft && !removing ? <ErrorState error={mutation.error} /> : null}
      {templates.data ? (
        <DataTable
          caption="قوالب أسباب الرفض"
          columns={columns}
          rows={items}
          rowKey={(row) => row.id}
          empty={
            <EmptyState
              title="لا قوالب بعد"
              hint="أضف أسباب الرفض الأكثر تكراراً ليختار منها المراجعون."
            />
          }
        />
      ) : null}

      <ConfirmDialog
        open={draft !== null}
        title={draft?.id ? "تعديل القالب" : "قالب جديد"}
        confirmLabel={draft?.id ? "حفظ" : "إضافة القالب"}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={save}
        onCancel={() => setDraft(null)}
      >
        {draft ? (
          <>
            <label className="field">
              <span className="field-label-row">
                العنوان
                <CharCount value={draft.titleAr.trim()} max={TITLE_MAX} />
              </span>
              <input
                value={draft.titleAr}
                placeholder="مثال: الترخيص غير واضح"
                data-testid="template-title"
                aria-invalid={titleLength > TITLE_MAX || Boolean(server.titleAr)}
                onChange={(event) => setDraft({ ...draft, titleAr: event.target.value })}
              />
              <span className="field-hint">يراه المراجع في قائمة القوالب فقط.</span>
            </label>
            <label className="field">
              <span className="field-label-row">
                نص السبب
                <CharCount value={draft.bodyAr.trim()} max={BODY_MAX} />
              </span>
              <textarea
                rows={5}
                value={draft.bodyAr}
                placeholder="النص الذي يصل إلى صاحب المنشأة، ويقول ما المطلوب منه."
                data-testid="template-body"
                aria-invalid={bodyLength > BODY_MAX || Boolean(server.bodyAr)}
                onChange={(event) => setDraft({ ...draft, bodyAr: event.target.value })}
              />
            </label>
            <label className="field">
              <span>الترتيب</span>
              <input
                type="number"
                dir="ltr"
                min={0}
                value={draft.sortOrder}
                data-testid="template-order"
                onChange={(event) => setDraft({ ...draft, sortOrder: event.target.value })}
              />
              <span className="field-hint">الأصغر يظهر أولاً في القائمة.</span>
            </label>
            {problem ? <p className="form-error">{problem}</p> : null}
            <label className="switch-row">
              <span>مفعّل ويظهر للمراجعين</span>
              <input
                type="checkbox"
                checked={draft.active}
                data-testid="template-active"
                onChange={(event) => setDraft({ ...draft, active: event.target.checked })}
              />
            </label>
          </>
        ) : null}
      </ConfirmDialog>

      <ConfirmDialog
        open={removing !== null}
        title="حذف القالب"
        body={
          removing
            ? `يُحذف «${removing.titleAr}». لا يتغير أي رفض سابق استُعمل فيه هذا النص.`
            : undefined
        }
        confirmLabel="تأكيد الحذف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={remove}
        onCancel={() => setRemoving(null)}
      />

      <Toast message={toast} onDismiss={dismissToast} />
    </div>
  );
}
