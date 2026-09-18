"use client";

import { use, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  ConfirmDialog,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";

type UserDetail = Readonly<{
  id: string;
  name: string;
  phone: string;
  active: boolean;
  createdAt: string | null;
  roleIds: readonly string[];
}>;

type Role = Readonly<{ id: number; code: string; name: string; permissions: readonly string[] }>;

/**
 * One account: its state, and the Admin roles it holds.
 *
 * Roles are replaced as a set rather than added one at a time, because that is what the
 * contract offers and because a partial update of an authorization set is a race waiting to
 * happen. The list is the authority; blocking is separate and revokes sessions immediately.
 */
export default function UserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const user = useResource<UserDetail>("user", { id });
  const canManageUsers = useCan("admin.users.manage");
  const canManageRoles = useCan("admin.roles.manage");
  const roles = useResource<{ items: Role[] }>("roles", {}, { enabled: canManageRoles });
  const mutation = useMutation();

  const [dialog, setDialog] = useState<"block" | "unblock" | "roles" | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  // The checkboxes are derived from what the server says, not mirrored into state by an
  // effect. An edit is held as an override stamped with the server value it was made
  // against, so a reload after saving replaces it rather than leaving a stale selection.
  const [override, setOverride] = useState<{ base: string; roles: string[] } | null>(null);
  const base = (user.data?.roleIds ?? []).map(String).join(",");
  const selected = override?.base === base ? override.roles : base ? base.split(",") : [];
  const setSelected = (next: string[]) => setOverride({ base, roles: next });

  async function submit(): Promise<void> {
    let ok = false;
    if (dialog === "block") ok = await mutation.run("userBlock", { id });
    if (dialog === "unblock") ok = await mutation.run("userUnblock", { id });
    if (dialog === "roles") ok = await mutation.run("userRoles", { id, roleIds: selected });
    if (!ok) return;
    setDialog(null);
    setToast("تم تنفيذ الإجراء وتسجيله في سجل التدقيق.");
    user.reload();
  }

  return (
    <div className="stack">
      <PageHeader
        title={user.data?.name ?? "مستخدم"}
        description="حالة الحساب والأدوار الإدارية."
        actions={
          canManageUsers && user.data ? (
            user.data.active ? (
              <button
                type="button"
                className="button-danger"
                data-testid="block"
                onClick={() => {
                  mutation.reset();
                  setDialog("block");
                }}
              >
                حظر
              </button>
            ) : (
              <button
                type="button"
                className="button-primary"
                data-testid="unblock"
                onClick={() => {
                  mutation.reset();
                  setDialog("unblock");
                }}
              >
                رفع الحظر
              </button>
            )
          ) : null
        }
      />

      {user.loading ? <LoadingState /> : null}
      {user.error ? <ErrorState error={user.error} onRetry={user.reload} /> : null}

      {user.data ? (
        <section className="panel stack">
          <h2>الحساب</h2>
          <div className="button-row">
            <StatusBadge tone={user.data.active ? "positive" : "danger"}>
              {user.data.active ? "فعّال" : "محظور"}
            </StatusBadge>
            <span className="muted cell-ltr">{user.data.phone}</span>
            <span className="muted">
              أُنشئ: <span className="cell-ltr">{formatDateTime(user.data.createdAt)}</span>
            </span>
          </div>
        </section>
      ) : null}

      {canManageRoles ? (
        <section className="panel stack">
          <h2>الأدوار الإدارية</h2>
          {roles.loading ? <LoadingState /> : null}
          {roles.data ? (
            <>
              <div className="stack">
                {roles.data.items.map((role) => (
                  <label key={role.id} className="switch-row">
                    <span>
                      <strong>{role.name}</strong>{" "}
                      <span className="muted">({role.permissions.length} صلاحية)</span>
                    </span>
                    <input
                      type="checkbox"
                      data-testid={`role-${role.code}`}
                      checked={selected.includes(String(role.id))}
                      onChange={(event) =>
                        setSelected(
                          event.target.checked
                            ? [...selected, String(role.id)]
                            : selected.filter((value) => value !== String(role.id)),
                        )
                      }
                    />
                  </label>
                ))}
              </div>
              <div className="form-footer">
                <button
                  type="button"
                  className="button-primary"
                  data-testid="save-roles"
                  onClick={() => {
                    mutation.reset();
                    setDialog("roles");
                  }}
                >
                  حفظ الأدوار
                </button>
              </div>
            </>
          ) : null}
        </section>
      ) : null}

      <ConfirmDialog
        open={dialog === "block"}
        title="حظر الحساب"
        body="ستُلغى جلسات المستخدم فوراً ولن يستطيع تسجيل الدخول."
        confirmLabel="تأكيد الحظر"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog === "unblock"}
        title="رفع الحظر"
        body="سيستطيع المستخدم تسجيل الدخول من جديد."
        confirmLabel="تأكيد"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog === "roles"}
        title="استبدال الأدوار الإدارية"
        body="تحدد هذه الأدوار ما يستطيع المستخدم فعله داخل اللوحة. يُستبدل الطقم بالكامل."
        confirmLabel="تأكيد الاستبدال"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
