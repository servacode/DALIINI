"use client";

import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  type Column,
  DataTable,
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

type Group = Readonly<{
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  active: boolean;
  sortOrder: number;
}>;

/**
 * Category groups: the first step of Cycle J.
 *
 * `code` is set once. The edit row does not offer it, and the backend refuses it outright
 * rather than ignoring it, so a rename that looks like it worked cannot happen.
 */
export default function TaxonomyGroupsPage() {
  const groups = useResource<{ items: Group[] }>("categoryGroups");
  const mutation = useMutation();
  const canManage = useCan("admin.taxonomy.manage");

  const [draft, setDraft] = useState({ code: "", nameAr: "", nameEn: "", sortOrder: "0" });
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);

  async function create(): Promise<void> {
    const ok = await mutation.run("categoryGroupCreate", {
      code: draft.code.trim(),
      nameAr: draft.nameAr.trim(),
      nameEn: draft.nameEn.trim(),
      sortOrder: Number(draft.sortOrder) || 0,
    });
    if (!ok) return;
    setDraft({ code: "", nameAr: "", nameEn: "", sortOrder: "0" });
    setToast("تمت إضافة المجموعة.");
    groups.reload();
  }

  async function toggle(group: Group): Promise<void> {
    const ok = await mutation.run("categoryGroupUpdate", {
      id: group.id,
      active: !group.active,
    });
    if (!ok) return;
    setToast(group.active ? "تم تعطيل المجموعة." : "تم تفعيل المجموعة.");
    groups.reload();
  }

  const columns: readonly Column<Group>[] = [
    { key: "nameAr", header: "الاسم", render: (row) => row.nameAr },
    { key: "code", header: "الرمز", ltr: true, render: (row) => <code>{row.code}</code> },
    { key: "sortOrder", header: "الترتيب", ltr: true, render: (row) => row.sortOrder },
    {
      key: "active",
      header: "الحالة",
      render: (row) => (
        <StatusBadge tone={row.active ? "positive" : "neutral"}>
          {row.active ? "مفعّلة" : "معطّلة"}
        </StatusBadge>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) =>
        canManage ? (
          <button
            type="button"
            className="button-ghost"
            disabled={mutation.pending}
            data-testid={`toggle-group-${row.code}`}
            onClick={() => toggle(row)}
          >
            {row.active ? "تعطيل" : "تفعيل"}
          </button>
        ) : null,
    },
  ];

  return (
    <div className="operation-stack">
      <PageHeader
        eyebrow="الدليل"
        title="مجموعات التصنيفات"
        description="البنية العليا للتصنيفات."
      />
      {groups.loading ? <LoadingState /> : null}
      {groups.error ? <ErrorState error={groups.error} onRetry={groups.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {groups.data ? (
        <DataTable
          caption="مجموعات التصنيفات"
          columns={columns}
          rows={groups.data.items}
          rowKey={(row) => row.id}
        />
      ) : null}

      {canManage ? (
        <FormSection
          title="إضافة مجموعة"
          description="الرمز يُحدَّد مرة واحدة ولا يمكن تغييره لاحقاً."
          footer={
            <button
              type="button"
              className="button-primary"
              disabled={mutation.pending}
              data-testid="create-group"
              onClick={create}
            >
              {mutation.pending ? "جارٍ الحفظ…" : "إضافة"}
            </button>
          }
        >
          <label className="field">
            <span>الرمز</span>
            <input
              dir="ltr"
              value={draft.code}
              data-testid="group-code"
              aria-invalid={Boolean(errors.code)}
              onChange={(event) => setDraft({ ...draft, code: event.target.value })}
            />
            {errors.code ? <span className="field-error">{errors.code}</span> : null}
          </label>
          <label className="field">
            <span>الاسم بالعربية</span>
            <input
              value={draft.nameAr}
              data-testid="group-name-ar"
              aria-invalid={Boolean(errors.nameAr)}
              onChange={(event) => setDraft({ ...draft, nameAr: event.target.value })}
            />
            {errors.nameAr ? <span className="field-error">{errors.nameAr}</span> : null}
          </label>
          <label className="field">
            <span>الاسم بالإنجليزية</span>
            <input
              dir="ltr"
              value={draft.nameEn}
              onChange={(event) => setDraft({ ...draft, nameEn: event.target.value })}
            />
          </label>
          <label className="field">
            <span>الترتيب</span>
            <input
              type="number"
              dir="ltr"
              value={draft.sortOrder}
              onChange={(event) => setDraft({ ...draft, sortOrder: event.target.value })}
            />
          </label>
        </FormSection>
      ) : null}

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
