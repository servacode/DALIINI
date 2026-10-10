"use client";

import Link from "next/link";
import { useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { Icon } from "../../../../components/icons";
import { ConfirmDialog, ErrorState, LoadingState, PageHeader, Toast } from "../../../../components/ui";
import { counted, FormDialog, ItemCard, ItemChips, ItemMeter } from "../../../../components/ui/extra";
import { type PermissionArea, areasFor, permissionLabel } from "../../../../lib/client/permissions";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";
import { LOCALE } from "../../../../lib/locale";

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

const NUMBER = new Intl.NumberFormat(LOCALE);

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

  return (
    <div className="stack">
      <PageHeader
        eyebrow="إدارة المشغّلين"
        title="الأدوار والصلاحيات"
        description="لكل دور اسم ومجموعة صلاحيات. يصل التغيير إلى كل من يحمل الدور في طلبه التالي."
        actions={
          canManage ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-role"
              onClick={() => open(null)}
            >
              <Icon name="plus" />
              دور جديد
            </button>
          ) : undefined
        }
      />
      {roles.loading || catalogue.loading ? <LoadingState /> : null}
      {roles.error ? <ErrorState error={roles.error} onRetry={roles.reload} /> : null}
      {catalogue.error ? <ErrorState error={catalogue.error} onRetry={catalogue.reload} /> : null}
      {roles.data && catalogue.data ? (
        <ul className="profile-grid" data-testid="roles">
          {roles.data.items.map((role, index) => (
            <RoleCard
              key={role.id}
              role={role}
              index={index}
              areas={areas}
              total={total}
              canManage={canManage}
              onEdit={() => open(role)}
              onDelete={() => {
                mutation.reset();
                setDeleting(role);
              }}
            />
          ))}
        </ul>
      ) : null}

      <FormDialog
        open={draft !== null}
        wide
        icon="shield"
        title={draft?.id === null ? "دور جديد" : viewing ? (draft?.name ?? "") : "تعديل الدور"}
        description={
          draft
            ? `${counted(draft.permissions.size, "صلاحية واحدة", "صلاحيتان", "صلاحيات", "صلاحية")} من ${NUMBER.format(total)}`
            : undefined
        }
        onClose={() => setDraft(null)}
        locked={mutation.pending}
        testId="role-sheet"
        footer={
          viewing ? (
            <button type="button" className="button-ghost" onClick={() => setDraft(null)}>
              إغلاق
            </button>
          ) : (
            <>
              {/* Beside the button, so a refusal is seen however far down the list the edit was. */}
              {mutation.error && Object.keys(errors).length === 0 ? (
                <ErrorState error={mutation.error} />
              ) : null}
              <div className="button-row">
                <button
                  type="button"
                  className="button-ghost"
                  disabled={mutation.pending}
                  onClick={() => setDraft(null)}
                >
                  إلغاء
                </button>
                <button
                  type="button"
                  className="button-primary"
                  disabled={mutation.pending || !draft?.name.trim()}
                  data-testid="save-role"
                  onClick={save}
                >
                  {mutation.pending ? "جارٍ الحفظ…" : draft?.id === null ? "إضافة الدور" : "حفظ"}
                </button>
              </div>
            </>
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
                  placeholder="مثال: مراجع الطلبات"
                  data-testid="role-name"
                  aria-invalid={Boolean(errors.name)}
                  onChange={(event) => setDraft({ ...draft, name: event.target.value })}
                />
                {errors.name ? <span className="field-error">{errors.name}</span> : null}
              </label>
            )}
            {errors.permissions ? <p className="field-error">{errors.permissions}</p> : null}
            <div className="permission-grid">
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
          </div>
        ) : null}
      </FormDialog>

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

function RoleCard({
  role,
  index,
  areas,
  total,
  canManage,
  onEdit,
  onDelete,
}: {
  role: Role;
  index: number;
  areas: readonly PermissionArea[];
  total: number;
  canManage: boolean;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const held = new Set(role.permissions);
  const covered = areas
    .filter((area) => area.permissions.some((permission) => held.has(permission.code)))
    .map((area) => area.label);
  const share = total ? Math.round((role.permissions.length / total) * 100) : 0;
  return (
    <ItemCard
      index={index}
      icon="shield"
      tone={role.locked ? "brand" : role.holderCount > 0 ? "positive" : "neutral"}
      title={role.name}
      subtitle={
        role.locked
          ? "دور مالك المنصة"
          : role.holderCount > 0
            ? `يحمله ${NUMBER.format(role.holderCount)} من المشغّلين`
            : "لا يحمله أحد بعد"
      }
      state={
        role.locked
          ? { label: "دور المنصة", tone: "brand" }
          : role.holderCount > 0
            ? { label: "مُسنَد", tone: "positive" }
            : { label: "غير مُسنَد" }
      }
      facts={[
        {
          label: "الصلاحيات",
          value: role.locked
            ? "كلها"
            : `${NUMBER.format(role.permissions.length)} من ${NUMBER.format(total)}`,
        },
        {
          label: "الحاملون",
          value:
            role.holderCount > 0 ? (
              <Link href={`/users?role=${encodeURIComponent(role.code)}`}>
                {NUMBER.format(role.holderCount)}
              </Link>
            ) : (
              NUMBER.format(0)
            ),
        },
      ]}
      testId={`role-${role.code}`}
      actions={
        role.locked ? (
          <span className="item-card-quiet">يملك كل الصلاحيات دائماً، ولا يُعدَّل.</span>
        ) : (
          <>
            <button
              type="button"
              className="profile-act-main"
              data-testid={`edit-role-${role.code}`}
              onClick={onEdit}
            >
              <Icon name={canManage ? "edit" : "eye"} width={16} height={16} />
              {canManage ? "تعديل الصلاحيات" : "عرض الصلاحيات"}
            </button>
            {canManage ? (
              <button
                type="button"
                className="profile-act-icon"
                aria-label="حذف الدور"
                data-testid={`delete-role-${role.code}`}
                disabled={role.holderCount > 0}
                title={role.holderCount > 0 ? "لا يُحذف دور يحمله أحد." : "حذف الدور"}
                onClick={onDelete}
              >
                <Icon name="trash" width={16} height={16} />
              </button>
            ) : null}
          </>
        )
      }
    >
      <ItemMeter
        value={role.locked ? total : role.permissions.length}
        total={total}
        label={
          role.locked ? "كل صلاحيات المنصة" : `${NUMBER.format(share)}٪ من صلاحيات المنصة`
        }
      />
      <ItemChips
        items={role.locked ? areas.map((area) => area.label) : covered}
        max={3}
        empty="لا صلاحيات بعد."
      />
    </ItemCard>
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
          <span>{permissionLabel(permission.code, permission.label)}</span>
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
