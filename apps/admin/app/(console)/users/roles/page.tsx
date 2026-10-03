"use client";

import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
} from "../../../../components/ui";
import { SidePanel } from "../../../../components/ui/extra";
import { type PermissionArea, areasFor, permissionLabel } from "../../../../lib/client/permissions";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";

type Role = Readonly<{
  id: number;
  code: string;
  name: string;
  permissions: readonly string[];
  holderCount: number;
  locked: boolean;
}>;

type Catalogue = Readonly<{ items: readonly Readonly<{ code: string; description: string }>[] }>;

/** The role in the sheet: a new one has no id. */
type Draft = Readonly<{ id: number | null; name: string; permissions: ReadonlySet<string> }>;

const NUMBER = new Intl.NumberFormat("ar-SY");

/**
 * The roles operators hold, and what each one may do (DECISION-072).
 *
 * A role is a name and a set of permissions, chosen here by job (reviews, facilities, people…)
 * rather than from a list of codes. A change reaches every holder on their next request. The
 * owner role is the platform's own: it always holds everything and is shown, not edited. The
 * backend refuses any change that would leave nobody able to grant roles, and says so here.
 */
export default function RolesPage() {
  const roles = useResource<{ items: Role[] }>("roles");
  const catalogue = useResource<Catalogue>("permissions");
  const mutation = useMutation();
  const canManage = useCan("admin.roles.manage");

  const [draft, setDraft] = useState<Draft | null>(null);
  const [deleting, setDeleting] = useState<Role | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const errors = fieldErrorsFor(mutation.error);
  const areas = catalogue.data ? areasFor(catalogue.data.items) : [];
  const total = catalogue.data?.items.length ?? 0;
  const viewing = draft !== null && draft.id !== null && !canManage;

  function open(role: Role | null): void {
    mutation.reset();
    setDraft(
      role
        ? { id: role.id, name: role.name, permissions: new Set(role.permissions) }
        : { id: null, name: "", permissions: new Set() },
    );
  }

  function toggle(codes: readonly string[], on: boolean): void {
    if (!draft) return;
    const next = new Set(draft.permissions);
    for (const code of codes) {
      if (on) next.add(code);
      else next.delete(code);
    }
    setDraft({ ...draft, permissions: next });
  }

  async function save(): Promise<void> {
    if (!draft) return;
    const body = { name: draft.name.trim(), permissions: [...draft.permissions].sort() };
    const ok = draft.id === null
      ? await mutation.run("roleCreate", body)
      : await mutation.run("roleUpdate", { id: draft.id, ...body });
    if (!ok) return;
    setToast(draft.id === null ? "تمت إضافة الدور." : "تم حفظ الدور.");
    setDraft(null);
    roles.reload();
  }

  async function remove(): Promise<void> {
    if (!deleting) return;
    const ok = await mutation.run("roleDelete", { id: deleting.id });
    if (!ok) return;
    setToast("تم حذف الدور.");
    setDeleting(null);
    roles.reload();
  }

  const columns: readonly Column<Role>[] = [
    {
      key: "name",
      header: "الدور",
      render: (row) => (
        <span className="stack-tight">
          <strong>{row.name}</strong>
          {row.locked ? <StatusBadge tone="brand">دور المنصة</StatusBadge> : null}
        </span>
      ),
    },
    {
      key: "permissions",
      header: "الصلاحيات",
      render: (row) =>
        row.locked ? (
          <span className="muted">كل الصلاحيات</span>
        ) : (
          <span className="tabular">
            {NUMBER.format(row.permissions.length)}
            {total ? <span className="muted"> من {NUMBER.format(total)}</span> : null}
          </span>
        ),
    },
    {
      key: "holders",
      header: "الحاملون",
      render: (row) => <span className="tabular">{NUMBER.format(row.holderCount)}</span>,
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) =>
        row.locked ? null : (
          <div className="button-row">
            <button
              type="button"
              className="button-ghost"
              data-testid={`edit-role-${row.code}`}
              onClick={() => open(row)}
            >
              {canManage ? "تعديل" : "عرض"}
            </button>
            {canManage ? (
              <button
                type="button"
                className="button-ghost"
                data-testid={`delete-role-${row.code}`}
                disabled={row.holderCount > 0}
                title={row.holderCount > 0 ? "لا يُحذف دور يحمله أحد." : undefined}
                onClick={() => {
                  mutation.reset();
                  setDeleting(row);
                }}
              >
                حذف
              </button>
            ) : null}
          </div>
        ),
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="الأدوار والصلاحيات"
        description="لكل دور اسم ومجموعة صلاحيات. يصل التغيير إلى كل من يحمل الدور في طلبه التالي."
        actions={
          canManage ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-role"
              onClick={() => open(null)}
            >
              دور جديد
            </button>
          ) : undefined
        }
      />
      {roles.loading || catalogue.loading ? <LoadingState /> : null}
      {roles.error ? <ErrorState error={roles.error} onRetry={roles.reload} /> : null}
      {catalogue.error ? <ErrorState error={catalogue.error} onRetry={catalogue.reload} /> : null}
      {roles.data ? (
        <DataTable
          caption="الأدوار"
          columns={columns}
          rows={roles.data.items}
          rowKey={(row) => String(row.id)}
        />
      ) : null}

      <SidePanel
        open={draft !== null}
        title={draft?.id === null ? "دور جديد" : viewing ? (draft?.name ?? "") : "تعديل الدور"}
        description={
          draft
            ? `${NUMBER.format(draft.permissions.size)} صلاحية من ${NUMBER.format(total)}`
            : undefined
        }
        onClose={() => setDraft(null)}
        locked={mutation.pending}
        testId="role-sheet"
        footer={
          viewing ? null : (
            <div className="stack">
              {/* Beside the button, so a refusal is seen however far down the list the edit was. */}
              {mutation.error && Object.keys(errors).length === 0 ? (
                <ErrorState error={mutation.error} />
              ) : null}
              <div className="button-row">
                <button
                  type="button"
                  className="button-primary"
                  disabled={mutation.pending || !draft?.name.trim()}
                  data-testid="save-role"
                  onClick={save}
                >
                  {mutation.pending ? "جارٍ الحفظ…" : "حفظ"}
                </button>
                <button
                  type="button"
                  className="button-ghost"
                  disabled={mutation.pending}
                  onClick={() => setDraft(null)}
                >
                  إلغاء
                </button>
              </div>
            </div>
          )
        }
      >
        {draft ? (
          <div className="stack">
            {viewing ? null : (
              <label className="field">
                <span>اسم الدور</span>
                <input
                  value={draft.name}
                  maxLength={120}
                  data-testid="role-name"
                  aria-invalid={Boolean(errors.name)}
                  onChange={(event) => setDraft({ ...draft, name: event.target.value })}
                />
                {errors.name ? <span className="field-error">{errors.name}</span> : null}
              </label>
            )}
            {errors.permissions ? <p className="field-error">{errors.permissions}</p> : null}
            {areas.map((area) => (
              <AreaSection
                key={area.key}
                area={area}
                chosen={draft.permissions}
                readOnly={viewing}
                onToggle={toggle}
              />
            ))}
          </div>
        ) : null}
      </SidePanel>

      <ConfirmDialog
        open={deleting !== null}
        title="حذف الدور"
        body={deleting ? `سيُحذف دور «${deleting.name}». لا يحمله أحد الآن.` : undefined}
        confirmLabel="حذف"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={remove}
        onCancel={() => setDeleting(null)}
      />

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}

function AreaSection({
  area,
  chosen,
  readOnly,
  onToggle,
}: {
  area: PermissionArea;
  chosen: ReadonlySet<string>;
  readOnly: boolean;
  onToggle: (codes: readonly string[], on: boolean) => void;
}) {
  const codes = area.permissions.map((p) => p.code);
  const all = codes.every((code) => chosen.has(code));
  return (
    <fieldset className="permission-area" data-testid={`area-${area.key}`}>
      <legend>
        <span>{area.label}</span>
        {readOnly ? null : (
          <button type="button" className="button-link" onClick={() => onToggle(codes, !all)}>
            {all ? "إلغاء الكل" : "اختيار الكل"}
          </button>
        )}
      </legend>
      {area.permissions.map((permission) => (
        <label key={permission.code} className="switch-row">
          <span>
            {permissionLabel(permission.code, permission.label)}
            <code className="permission-code">{permission.code}</code>
          </span>
          <input
            type="checkbox"
            data-testid={`perm-${permission.code}`}
            checked={chosen.has(permission.code)}
            disabled={readOnly}
            onChange={(event) => onToggle([permission.code], event.target.checked)}
          />
        </label>
      ))}
    </fieldset>
  );
}
