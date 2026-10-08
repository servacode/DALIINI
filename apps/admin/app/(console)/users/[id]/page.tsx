"use client";

import Link from "next/link";
import { use, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { Icon } from "../../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  TermBadge,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";

type Facility = Readonly<{
  id: string;
  nameAr: string;
  role: string;
  status: string;
}>;

type Session = Readonly<{
  id: string;
  platform: string;
  deviceName: string;
  createdAt: string | null;
  lastSeenAt: string | null;
}>;

type UserDetail = Readonly<{
  id: string;
  name: string;
  phone: string;
  active: boolean;
  provinceId: string | null;
  provinceName: string | null;
  phoneVerifiedAt: string | null;
  lastLoginAt: string | null;
  createdAt: string | null;
  roleIds: readonly number[];
  facilities: readonly Facility[];
  sessions: readonly Session[];
}>;

type Role = Readonly<{ id: number; code: string; name: string; permissions: readonly string[] }>;

type Dialog = "block" | "unblock" | "roles" | "mfa" | "recovery" | "sessions";

const MEMBER_ROLE: Record<string, string> = { OWNER: "مالك", MANAGER: "مدير" };

const PLATFORM: Record<string, string> = {
  ANDROID: "أندرويد",
  IOS: "آيفون",
  WEB: "متصفّح",
};

/**
 * One account, read in three passes: who it is, what hangs off it, and what may be done.
 *
 * The order is the order an operator decides in. Before blocking someone, the pharmacies
 * behind their name have to be on the same screen — the console used to show an account's
 * state and its roles and nothing else, so «block» was pressed without knowing whether three
 * live facilities went quiet with it.
 *
 * **No password is set here, ever.** The recovery action sends a code to the account's own
 * number and the person chooses their own password (DECISION-104). An operator who could set
 * one could sign in as that person, and the audit trail would name the wrong human.
 */
export default function UserDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const user = useResource<UserDetail>("user", { id });
  const canManageUsers = useCan("admin.users.manage");
  const canManageRoles = useCan("admin.roles.manage");
  const canReadFacilities = useCan("admin.facilities.read");
  const roles = useResource<{ items: Role[] }>("roles", {}, { enabled: canManageRoles });
  const mutation = useMutation();

  const [dialog, setDialog] = useState<Dialog | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  // The checkboxes are derived from what the server says, not mirrored into state by an
  // effect. An edit is held as an override stamped with the server value it was made
  // against, so a reload after saving replaces it rather than leaving a stale selection.
  const [override, setOverride] = useState<{ base: string; roles: string[] } | null>(null);
  const base = (user.data?.roleIds ?? []).map(String).join(",");
  const selected = override?.base === base ? override.roles : base ? base.split(",") : [];
  const setSelected = (next: string[]) => setOverride({ base, roles: next });

  function open(next: Dialog): void {
    mutation.reset();
    setDialog(next);
  }

  async function submit(): Promise<void> {
    let ok = false;
    let done = "تم تنفيذ الإجراء وتسجيله في سجل التدقيق.";
    if (dialog === "block") ok = await mutation.run("userBlock", { id });
    if (dialog === "unblock") ok = await mutation.run("userUnblock", { id });
    if (dialog === "roles") ok = await mutation.run("userRoles", { id, roleIds: selected });
    if (dialog === "mfa") ok = await mutation.run("userMfaReset", { id });
    if (dialog === "recovery") {
      ok = await mutation.run("userRecovery", { id });
      done = `أُرسل رمز الاستعادة إلى ${user.data?.phone ?? "رقم الحساب"}. يختار صاحب الحساب كلمته بنفسه.`;
    }
    if (dialog === "sessions") {
      ok = await mutation.run("userSessionsRevoke", { id });
      done = "خرجت كل الأجهزة من الحساب، والحساب نفسه ما زال فعّالاً.";
    }
    if (!ok) return;
    setDialog(null);
    setToast(done);
    user.reload();
  }

  const account = user.data;

  return (
    <div className="stack">
      <PageHeader
        back={{ href: "/users", label: "الحسابات" }}
        title={account?.name ?? "حساب"}
        description="كل ما يخص هذا الحساب، وما يملكه، والإجراءات عليه."
        actions={
          canManageUsers && account ? (
            account.active ? (
              <button
                type="button"
                className="button-danger"
                data-testid="block"
                onClick={() => open("block")}
              >
                حظر الحساب
              </button>
            ) : (
              <button
                type="button"
                className="button-primary"
                data-testid="unblock"
                onClick={() => open("unblock")}
              >
                رفع الحظر
              </button>
            )
          ) : null
        }
      />

      {user.loading ? <LoadingState /> : null}
      {user.error ? <ErrorState error={user.error} onRetry={user.reload} /> : null}

      {account ? (
        <>
          <Panel title="الحساب">
            <KeyValueList
              items={[
                {
                  label: "الحالة",
                  value: (
                    <StatusBadge tone={account.active ? "positive" : "danger"}>
                      {account.active ? "فعّال" : "محظور"}
                    </StatusBadge>
                  ),
                },
                { label: "رقم الهاتف", value: account.phone, ltr: true },
                {
                  label: "الرقم موثّق",
                  value: account.phoneVerifiedAt ? (
                    <StatusBadge tone="positive">
                      نعم · {formatDateTime(account.phoneVerifiedAt)}
                    </StatusBadge>
                  ) : (
                    // An account opened from the console has not proved its number yet: the
                    // person proves it the first time they sign in.
                    <StatusBadge tone="warning">لم يُوثَّق بعد</StatusBadge>
                  ),
                },
                {
                  label: "المحافظة",
                  value: account.provinceName ?? <span className="muted">—</span>,
                },
                { label: "تاريخ الإنشاء", value: formatDateTime(account.createdAt), ltr: true },
                {
                  label: "آخر دخول",
                  value: account.lastLoginAt ? (
                    formatDateTime(account.lastLoginAt)
                  ) : (
                    <span className="muted">لم يدخل بعد</span>
                  ),
                  ltr: true,
                },
              ]}
            />
          </Panel>

          <Panel
            title="المنشآت"
            description="ما يملكه هذا الحساب أو يديره. يُقرأ قبل أي قرار بحظره."
          >
            {account.facilities.length === 0 ? (
              <EmptyState title="لا منشأة لهذا الحساب." />
            ) : (
              <ul className="plain-list" data-testid="user-facilities">
                {account.facilities.map((facility) => (
                  <li key={facility.id} className="plain-row">
                    <span>
                      {canReadFacilities ? (
                        <Link href={`/facilities/${facility.id}`}>{facility.nameAr}</Link>
                      ) : (
                        facility.nameAr
                      )}{" "}
                      <span className="muted">({MEMBER_ROLE[facility.role] ?? facility.role})</span>
                    </span>
                    <TermBadge group="facilityStatus" value={facility.status} />
                  </li>
                ))}
              </ul>
            )}
          </Panel>

          <Panel
            title="الأجهزة المسجّلة"
            description="الأجهزة الداخلة الآن. لهاتف ضاع أو سُرق، أخرِجها كلها دون حظر الحساب."
            actions={
              canManageUsers && account.sessions.length > 0 ? (
                <button
                  type="button"
                  className="button-ghost"
                  data-testid="revoke-sessions"
                  onClick={() => open("sessions")}
                >
                  إخراج كل الأجهزة
                </button>
              ) : null
            }
          >
            {account.sessions.length === 0 ? (
              <EmptyState title="لا جهاز داخل الآن." />
            ) : (
              <ul className="plain-list" data-testid="user-sessions">
                {account.sessions.map((session) => (
                  <li key={session.id} className="plain-row">
                    <span>
                      <Icon name="phone" />{" "}
                      {session.deviceName || PLATFORM[session.platform] || "جهاز"}{" "}
                      <span className="muted">{PLATFORM[session.platform] ?? session.platform}</span>
                    </span>
                    <span className="muted" dir="ltr">
                      {formatDateTime(session.lastSeenAt ?? session.createdAt)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </Panel>

          {canManageUsers ? (
            <Panel
              title="الدخول"
              description="اللوحة لا تعرف كلمة مرور أحد ولا تعيّنها. ما تستطيعه هو إرسال رمز استعادة إلى رقم صاحب الحساب، ليختار هو كلمته."
            >
              <div className="form-footer">
                <button
                  type="button"
                  className="button-primary"
                  data-testid="send-recovery"
                  onClick={() => open("recovery")}
                >
                  إرسال رمز استعادة
                </button>
                <button
                  type="button"
                  className="button-ghost"
                  data-testid="mfa-reset"
                  onClick={() => open("mfa")}
                >
                  إعادة ضبط التحقق بخطوتين
                </button>
              </div>
            </Panel>
          ) : null}

          {canManageRoles ? (
            <Panel
              title="الأدوار الإدارية"
              description="ما يستطيع هذا الحساب فعله داخل اللوحة. حساب بلا دور لا يرى اللوحة أصلاً."
            >
              {roles.loading ? <LoadingState /> : null}
              {roles.data ? (
                <>
                  <div>
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
                      onClick={() => open("roles")}
                    >
                      حفظ الأدوار
                    </button>
                  </div>
                </>
              ) : null}
            </Panel>
          ) : null}
        </>
      ) : null}

      <ConfirmDialog
        open={dialog === "block"}
        title="حظر الحساب"
        body={
          account?.facilities.length
            ? `ستُلغى جلسات المستخدم فوراً ولن يستطيع تسجيل الدخول. هذا الحساب مرتبط بـ${account.facilities.length} منشأة — راجعها قبل التأكيد.`
            : "ستُلغى جلسات المستخدم فوراً ولن يستطيع تسجيل الدخول."
        }
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
      <ConfirmDialog
        open={dialog === "recovery"}
        title="إرسال رمز استعادة"
        body={`يصل الرمز إلى ${account?.phone ?? "رقم الحساب"} على واتساب، ويختار صاحبه كلمة مروره بنفسه. لن تظهر لك الكلمة ولا الرمز. تحقق من هويته قبل الإرسال.`}
        confirmLabel="إرسال"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog === "sessions"}
        title="إخراج كل الأجهزة"
        body="تخرج كل الأجهزة من الحساب ويتوقف وصول الإشعارات إليها. الحساب يبقى فعّالاً، فيستطيع صاحبه الدخول من جديد — وهذا هو الجواب الصحيح لهاتف ضاع، لا الحظر."
        confirmLabel="إخراج الأجهزة"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />
      <ConfirmDialog
        open={dialog === "mfa"}
        title="إعادة ضبط التحقق بخطوتين"
        body="لمن فقد هاتفه ورموزه الاحتياطية. يُمسح تطبيق المصادقة المرتبط بالحساب ورموزه، ويُطلب منه إعداد جديد عند دخوله التالي إلى اللوحة. تحقق من هويته قبل التأكيد."
        confirmLabel="إعادة الضبط"
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
