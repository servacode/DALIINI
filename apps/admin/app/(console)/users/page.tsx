"use client";

import { useSearchParams } from "next/navigation";
import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { Icon } from "../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  FieldError,
  FilterBar,
  LoadingState,
  PageHeader,
  Pagination,
  StatusBadge,
  TermBadge,
  Toast,
  formatDateTime,
  pageSummary,
} from "../../../components/ui";
import {
  LastSeen,
  RecordCard,
  RecordSection,
  SidePanel,
  relativeTime,
} from "../../../components/ui/extra";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { useUrlFilters } from "../../../lib/client/use-url-filters";

type AdminUser = Readonly<{
  id: string;
  name: string;
  phone: string;
  active: boolean;
  provinceId: string | null;
  provinceName: string | null;
  phoneVerifiedAt: string | null;
  lastLoginAt: string | null;
  lastSeenAt: string | null;
  recentlyActive: boolean;
  facilityCount: number;
  sessionCount: number;
  createdAt: string | null;
}>;

type Facility = Readonly<{ id: string; nameAr: string; role: string; status: string }>;

type Session = Readonly<{
  id: string;
  platform: string;
  deviceName: string;
  createdAt: string | null;
  lastSeenAt: string | null;
}>;

type UserDetail = AdminUser &
  Readonly<{
    roleIds: readonly number[];
    facilities: readonly Facility[];
    sessions: readonly Session[];
  }>;

type Role = Readonly<{ id: number; code: string; name: string; permissions: readonly string[] }>;

type Province = Readonly<{ id: string; nameAr: string }>;

type Action = "block" | "unblock" | "recovery" | "sessions" | "mfa" | "roles";

const NUMBER = new Intl.NumberFormat("ar-SY");
const MEMBER_ROLE: Record<string, string> = { OWNER: "مالك", MANAGER: "مدير" };
const PLATFORM: Record<string, string> = { ANDROID: "أندرويد", IOS: "آيفون", WEB: "متصفّح" };

/**
 * The first letters of the name, which is what stands in for a photograph.
 *
 * The definite article is dropped first: «مالك الجوال» read letter by letter gives «ما»,
 * because the second word begins with it, and half the marks on the page came out the same.
 */
function initials(name: string): string {
  const words = name
    .trim()
    .split(/\s+/)
    .map((word) => (word.length > 2 && word.startsWith("ال") ? word.slice(2) : word))
    .filter(Boolean);
  if (words.length === 0) return "؟";
  return words.length === 1 ? words[0]!.slice(0, 2) : words[0]![0]! + words[1]![0]!;
}

/**
 * Every account registered in the app, each one a card that holds all of itself
 * (DECISION-106).
 *
 * There is no separate detail page. A card opens in place: the operator stays in the list,
 * keeps the page they searched and filtered, and can open a second account without going
 * back. `/users/<id>` still resolves — it redirects here with that card open — so the links
 * already written in audit entries and messages keep working.
 *
 * What a card shows closed is what a decision usually needs: who, where, whether they are
 * blocked, when they were last active, how many places hang off them. What opening adds is
 * the detail behind those counts, and it is not fetched until it is asked for, so a page of
 * a hundred cards costs one request.
 */
export default function UsersPage() {
  const [filters, setFilters] = useUrlFilters({ q: "", status: "", role: "", ordering: "" });
  const users = useCursorPage<AdminUser>("users", filters);
  const canManageUsers = useCan("admin.users.manage");
  const canManageRoles = useCan("admin.roles.manage");
  // Roles sit behind their own permission; without it the filter still offers "any role"
  // and "no role", which the backend answers from the user table alone.
  const roles = useResource<{ items: Role[] }>("roles");
  const provinces = useResource<{ items: Province[] }>(
    "provinces",
    {},
    { enabled: canManageUsers },
  );
  const mutation = useMutation();

  // `/users/<id>` redirects here with `?open=<id>`, so a link written down before the
  // detail page was removed still arrives at the same account. The parameter is the first
  // value of the state and nothing keeps writing to it: opening another card is not a
  // navigation, and the back button should leave the list rather than close a card.
  const linked = useSearchParams().get("open");
  const [openId, setOpenId] = useState<string | null>(linked);
  const [dialog, setDialog] = useState<{ action: Action; user: AdminUser } | null>(null);
  const [draft, setDraft] = useState<{ name: string; phone: string; provinceId: string } | null>(
    null,
  );
  const [toast, setToast] = useState<string | null>(null);

  function ask(action: Action, user: AdminUser): void {
    mutation.reset();
    setDialog({ action, user });
  }

  return (
    <div className="stack">
      <PageHeader
        title="الحسابات"
        description="كل حساب مسجّل في التطبيق. كل بطاقة تحمل حسابها كاملاً: حالته، وما يملكه، وأجهزته، وما يُفعل به."
        actions={
          canManageUsers ? (
            <button
              type="button"
              className="button-primary"
              data-testid="new-account"
              onClick={() => {
                mutation.reset();
                setDraft({ name: "", phone: "", provinceId: "" });
              }}
            >
              حساب جديد
            </button>
          ) : null
        }
      />

      <FilterBar
        fields={[
          { name: "q", label: "بحث", placeholder: "اسم أو رقم هاتف" },
          {
            name: "status",
            label: "الحالة",
            type: "select",
            options: [
              { value: "active", label: "فعّال" },
              { value: "blocked", label: "محظور" },
            ],
          },
          {
            name: "role",
            label: "الدور",
            type: "select",
            options: [
              { value: "any", label: "أي دور إداري" },
              { value: "none", label: "بلا دور إداري" },
              ...(roles.data?.items ?? []).map((role) => ({
                value: role.code,
                label: role.name,
              })),
            ],
          },
          {
            name: "ordering",
            label: "الترتيب",
            type: "select",
            options: [
              { value: "-createdAt", label: "الأحدث تسجيلاً" },
              { value: "createdAt", label: "الأقدم تسجيلاً" },
              { value: "name", label: "الاسم (أ إلى ي)" },
              { value: "-name", label: "الاسم (ي إلى أ)" },
            ],
          },
        ]}
        values={filters}
        onApply={setFilters}
      />

      {users.loading ? <LoadingState /> : null}
      {users.error ? <ErrorState error={users.error} onRetry={users.reload} /> : null}

      {users.data && users.data.items.length === 0 ? (
        <EmptyState
          title="لا حساب يطابق البحث."
          hint="جرّب اسماً أو رقماً آخر، أو امسح المرشّحات."
          illustration="noResults"
        />
      ) : null}

      {users.data && users.data.items.length > 0 ? (
        <>
          <p className="muted" data-testid="accounts-summary">
            {pageSummary(users.data.items.length, users.data.hasMore)}
          </p>
          <ul className="record-list" data-testid="accounts">
            {users.data.items.map((user) => (
              <AccountCard
                key={user.id}
                user={user}
                open={openId === user.id}
                onToggle={() => setOpenId(openId === user.id ? null : user.id)}
                canManageUsers={canManageUsers}
                canManageRoles={canManageRoles}
                onAction={ask}
              />
            ))}
          </ul>
        </>
      ) : null}

      {users.pagination ? <Pagination {...users.pagination} /> : null}

      <AccountDialogs
        dialog={dialog}
        mutation={mutation}
        onDone={(message) => {
          setDialog(null);
          setToast(message);
          users.reload();
        }}
        onCancel={() => setDialog(null)}
      />

      <SidePanel
        open={draft !== null}
        title="حساب جديد"
        description="اللوحة لا تضع كلمة مرور لأحد. يُفتح الحساب بلا كلمة، ثم تُرسل له رمز استعادة من بطاقته ليختار كلمته بنفسه."
        onClose={() => setDraft(null)}
        locked={mutation.pending}
        testId="new-account-panel"
        footer={
          <>
            <button
              type="button"
              className="button-primary"
              data-testid="create-account"
              disabled={
                mutation.pending ||
                !draft?.name.trim() ||
                !draft?.phone.trim() ||
                !draft?.provinceId
              }
              onClick={async () => {
                if (!draft) return;
                if (await mutation.run("userCreate", draft)) {
                  setDraft(null);
                  setToast("فُتح الحساب. أرسل له رمز استعادة من بطاقته ليختار كلمة مروره.");
                  users.reload();
                }
              }}
            >
              {mutation.pending ? "جارٍ الفتح…" : "فتح الحساب"}
            </button>
            <button
              type="button"
              className="button-ghost"
              disabled={mutation.pending}
              onClick={() => setDraft(null)}
            >
              إلغاء
            </button>
          </>
        }
      >
        {draft ? (
          <div className="stack">
            <label className="field">
              <span>الاسم</span>
              <input
                data-testid="account-name"
                value={draft.name}
                maxLength={120}
                onChange={(event) => setDraft({ ...draft, name: event.target.value })}
              />
            </label>
            <label className="field">
              <span>رقم الهاتف</span>
              <input
                data-testid="account-phone"
                dir="ltr"
                inputMode="tel"
                placeholder="09XXXXXXXX"
                value={draft.phone}
                onChange={(event) => setDraft({ ...draft, phone: event.target.value })}
              />
            </label>
            <label className="field">
              <span>المحافظة</span>
              <select
                data-testid="account-province"
                value={draft.provinceId}
                onChange={(event) => setDraft({ ...draft, provinceId: event.target.value })}
              >
                <option value="">اختر محافظة</option>
                {(provinces.data?.items ?? []).map((province) => (
                  <option key={province.id} value={province.id}>
                    {province.nameAr}
                  </option>
                ))}
              </select>
            </label>
            <FieldError id="create-account-error" message={mutation.error?.message} />
          </div>
        ) : null}
      </SidePanel>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}

function AccountCard({
  user,
  open,
  onToggle,
  canManageUsers,
  canManageRoles,
  onAction,
}: {
  user: AdminUser;
  open: boolean;
  onToggle: () => void;
  canManageUsers: boolean;
  canManageRoles: boolean;
  onAction: (action: Action, user: AdminUser) => void;
}) {
  return (
    <RecordCard
      testId={`account-${user.id}`}
      mark={initials(user.name)}
      title={user.name}
      muted={!user.active}
      facts={[
        <span key="phone" dir="ltr">
          {user.phone}
        </span>,
        user.provinceName ?? "بلا محافظة",
        <LastSeen key="seen" at={user.lastSeenAt} recent={user.recentlyActive} />,
      ]}
      badges={
        <>
          {!user.active ? <StatusBadge tone="danger">محظور</StatusBadge> : null}
          {user.facilityCount > 0 ? (
            <StatusBadge tone="info">
              {NUMBER.format(user.facilityCount)} منشأة
            </StatusBadge>
          ) : null}
          {user.sessionCount > 0 ? (
            <StatusBadge tone="neutral">
              {NUMBER.format(user.sessionCount)} جهاز
            </StatusBadge>
          ) : null}
          {!user.phoneVerifiedAt ? (
            <StatusBadge tone="warning">رقم غير موثّق</StatusBadge>
          ) : null}
        </>
      }
      actions={
        canManageUsers ? (
          <>
            {/* The everyday actions first, the irreversible one last and quiet. A filled
                red block on every card draws the eye before the name does. */}
            <button
              type="button"
              className="button-ghost"
              data-testid={`recovery-${user.id}`}
              onClick={() => onAction("recovery", user)}
            >
              إرسال رمز استعادة
            </button>
            <button
              type="button"
              className="button-ghost"
              data-testid={`sessions-${user.id}`}
              disabled={user.sessionCount === 0}
              onClick={() => onAction("sessions", user)}
            >
              إخراج الأجهزة
            </button>
            <button
              type="button"
              className="button-ghost"
              data-testid={`mfa-${user.id}`}
              onClick={() => onAction("mfa", user)}
            >
              إعادة ضبط التحقق بخطوتين
            </button>
            {user.active ? (
              <button
                type="button"
                className="button-ghost button-danger-quiet"
                data-testid={`block-${user.id}`}
                onClick={() => onAction("block", user)}
              >
                حظر
              </button>
            ) : (
              <button
                type="button"
                className="button-primary"
                data-testid={`unblock-${user.id}`}
                onClick={() => onAction("unblock", user)}
              >
                رفع الحظر
              </button>
            )}
          </>
        ) : null
      }
      open={open}
      onToggle={onToggle}
    >
      {open ? <AccountBody user={user} canManageRoles={canManageRoles} /> : null}
    </RecordCard>
  );
}

/** What is behind the counts. Fetched when the card opens, never before. */
function AccountBody({
  user,
  canManageRoles,
}: {
  user: AdminUser;
  canManageRoles: boolean;
}) {
  const detail = useResource<UserDetail>("user", { id: user.id });
  const roles = useResource<{ items: Role[] }>("roles", {}, { enabled: canManageRoles });
  const mutation = useMutation();
  const [toast, setToast] = useState<string | null>(null);
  const [confirm, setConfirm] = useState(false);

  // Derived from the server's answer, not mirrored into state: an edit is held as an
  // override stamped with the value it was made against, so a reload replaces it rather
  // than leaving a stale selection behind.
  const [override, setOverride] = useState<{ base: string; roles: string[] } | null>(null);
  const base = (detail.data?.roleIds ?? []).map(String).join(",");
  const selected = override?.base === base ? override.roles : base ? base.split(",") : [];

  if (detail.loading) return <LoadingState />;
  if (detail.error) return <ErrorState error={detail.error} onRetry={detail.reload} />;
  if (!detail.data) return null;
  const account = detail.data;

  return (
    <>
      <div className="record-columns">
        <RecordSection title="الحساب">
          <ul className="plain-list">
            <li className="plain-row">
              <span>الرقم موثّق</span>
              <span>
                {account.phoneVerifiedAt ? (
                  <StatusBadge tone="positive">نعم</StatusBadge>
                ) : (
                  <StatusBadge tone="warning">لا</StatusBadge>
                )}
              </span>
            </li>
            <li className="plain-row">
              <span>التسجيل</span>
              <span className="muted">{formatDateTime(account.createdAt)}</span>
            </li>
            <li className="plain-row">
              <span>آخر دخول</span>
              <span className="muted">
                {account.lastLoginAt ? relativeTime(account.lastLoginAt) : "لم يدخل بعد"}
              </span>
            </li>
          </ul>
        </RecordSection>

        <RecordSection title={`المنشآت (${NUMBER.format(account.facilities.length)})`}>
          {account.facilities.length === 0 ? (
            <p className="muted">لا منشأة لهذا الحساب.</p>
          ) : (
            <ul className="plain-list" data-testid="user-facilities">
              {account.facilities.map((facility) => (
                <li key={facility.id} className="plain-row">
                  <span>
                    {facility.nameAr}{" "}
                    <span className="muted">
                      ({MEMBER_ROLE[facility.role] ?? facility.role})
                    </span>
                  </span>
                  <TermBadge group="facilityStatus" value={facility.status} />
                </li>
              ))}
            </ul>
          )}
        </RecordSection>

        <RecordSection title={`الأجهزة (${NUMBER.format(account.sessions.length)})`}>
          {account.sessions.length === 0 ? (
            <p className="muted">لا جهاز داخل الآن.</p>
          ) : (
            <ul className="plain-list" data-testid="user-sessions">
              {account.sessions.map((session) => (
                <li key={session.id} className="plain-row">
                  <span>
                    <Icon name="phone" />{" "}
                    {session.deviceName || PLATFORM[session.platform] || "جهاز"}
                  </span>
                  <span className="muted">
                    {session.lastSeenAt
                      ? relativeTime(session.lastSeenAt)
                      : formatDateTime(session.createdAt)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </RecordSection>
      </div>

      {canManageRoles && roles.data ? (
        <RecordSection title="الأدوار الإدارية">
          <p className="muted">
            ما يستطيع هذا الحساب فعله داخل اللوحة. حساب بلا دور لا يرى اللوحة أصلاً.
          </p>
          <div>
            {roles.data.items.map((role) => (
              <label key={role.id} className="switch-row">
                <span>
                  <strong>{role.name}</strong>{" "}
                  <span className="muted">
                    ({NUMBER.format(role.permissions.length)} صلاحية)
                  </span>
                </span>
                <input
                  type="checkbox"
                  data-testid={`role-${role.code}`}
                  checked={selected.includes(String(role.id))}
                  onChange={(event) =>
                    setOverride({
                      base,
                      roles: event.target.checked
                        ? [...selected, String(role.id)]
                        : selected.filter((value) => value !== String(role.id)),
                    })
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
                setConfirm(true);
              }}
            >
              حفظ الأدوار
            </button>
          </div>
          <ConfirmDialog
            open={confirm}
            title="استبدال الأدوار الإدارية"
            body="تحدد هذه الأدوار ما يستطيع المستخدم فعله داخل اللوحة. يُستبدل الطقم بالكامل."
            confirmLabel="تأكيد الاستبدال"
            destructive
            pending={mutation.pending}
            error={mutation.error}
            onConfirm={async () => {
              if (await mutation.run("userRoles", { id: account.id, roleIds: selected })) {
                setConfirm(false);
                setToast("حُفظت الأدوار وسُجّلت في سجل التدقيق.");
                detail.reload();
              }
            }}
            onCancel={() => setConfirm(false)}
          />
        </RecordSection>
      ) : null}

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </>
  );
}

/** The four confirmations an action on an account asks for, in one place. */
function AccountDialogs({
  dialog,
  mutation,
  onDone,
  onCancel,
}: {
  dialog: { action: Action; user: AdminUser } | null;
  mutation: ReturnType<typeof useMutation>;
  onDone: (message: string) => void;
  onCancel: () => void;
}) {
  const user = dialog?.user;
  const run = async (operation: string, message: string): Promise<void> => {
    if (!user) return;
    if (await mutation.run(operation, { id: user.id })) onDone(message);
  };

  return (
    <>
      <ConfirmDialog
        open={dialog?.action === "block"}
        title={`حظر ${user?.name ?? "الحساب"}`}
        body={
          user && user.facilityCount > 0
            ? `ستُلغى جلساته فوراً ولن يستطيع تسجيل الدخول. هذا الحساب مرتبط بـ${NUMBER.format(user.facilityCount)} منشأة — راجعها قبل التأكيد.`
            : "ستُلغى جلساته فوراً ولن يستطيع تسجيل الدخول."
        }
        confirmLabel="تأكيد الحظر"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={() => run("userBlock", "حُظر الحساب وسُجّل ذلك في سجل التدقيق.")}
        onCancel={onCancel}
      />
      <ConfirmDialog
        open={dialog?.action === "unblock"}
        title={`رفع الحظر عن ${user?.name ?? "الحساب"}`}
        body="سيستطيع المستخدم تسجيل الدخول من جديد."
        confirmLabel="تأكيد"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={() => run("userUnblock", "رُفع الحظر وسُجّل ذلك في سجل التدقيق.")}
        onCancel={onCancel}
      />
      <ConfirmDialog
        open={dialog?.action === "recovery"}
        title="إرسال رمز استعادة"
        body={`يصل الرمز إلى ${user?.phone ?? "رقم الحساب"} على واتساب، ويختار صاحبه كلمة مروره بنفسه. لن تظهر لك الكلمة ولا الرمز. تحقق من هويته قبل الإرسال.`}
        confirmLabel="إرسال"
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={() =>
          run(
            "userRecovery",
            `أُرسل رمز الاستعادة إلى ${user?.phone ?? "رقم الحساب"}. يختار صاحب الحساب كلمته بنفسه.`,
          )
        }
        onCancel={onCancel}
      />
      <ConfirmDialog
        open={dialog?.action === "sessions"}
        title="إخراج كل الأجهزة"
        body="تخرج كل الأجهزة من الحساب ويتوقف وصول الإشعارات إليها. الحساب يبقى فعّالاً، فيستطيع صاحبه الدخول من جديد — وهذا هو الجواب الصحيح لهاتف ضاع، لا الحظر."
        confirmLabel="إخراج الأجهزة"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={() =>
          run("userSessionsRevoke", "خرجت كل الأجهزة، والحساب نفسه ما زال فعّالاً.")
        }
        onCancel={onCancel}
      />
      <ConfirmDialog
        open={dialog?.action === "mfa"}
        title="إعادة ضبط التحقق بخطوتين"
        body="لمن فقد هاتفه ورموزه الاحتياطية. يُمسح تطبيق المصادقة المرتبط بالحساب ورموزه، ويُطلب منه إعداد جديد عند دخوله التالي إلى اللوحة. تحقق من هويته قبل التأكيد."
        confirmLabel="إعادة الضبط"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={() => run("userMfaReset", "أُعيد ضبط التحقق بخطوتين وسُجّل في سجل التدقيق.")}
        onCancel={onCancel}
      />
    </>
  );
}
