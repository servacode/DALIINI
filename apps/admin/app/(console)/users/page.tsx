"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  FieldError,
  FilterBar,
  LoadingState,
  PageHeader,
  Pagination,
  termsFor,
  Toast,
  pageSummary,
} from "../../../components/ui";
import { ProfileCard, SidePanel, relativeTime } from "../../../components/ui/extra";
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
  lastLoginAt: string | null;
  lastSeenAt: string | null;
  recentlyActive: boolean;
  facilityCount: number;
  facilities: readonly Facility[];
  hasTwoFactor: boolean;
  createdAt: string | null;
}>;

type Facility = Readonly<{ id: string; nameAr: string; role: string; status: string }>;

type Role = Readonly<{ id: number; code: string; name: string; permissions: readonly string[] }>;

type Province = Readonly<{ id: string; nameAr: string }>;

type Action = "block" | "unblock" | "recovery" | "mfa";

const NUMBER = new Intl.NumberFormat("ar-SY");
const MEMBER_ROLE: Record<string, string> = { OWNER: "مالك", MANAGER: "مدير" };
const FACILITY_STATUS = termsFor("facilityStatus");

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
  const [filters, setFilters] = useUrlFilters({
    q: "",
    status: "",
    role: "",
    ordering: "",
    id: "",
  });
  const users = useCursorPage<AdminUser>("users", filters);
  const canManageUsers = useCan("admin.users.manage");
  // Roles sit behind their own permission; without it the filter still offers "any role"
  // and "no role", which the backend answers from the user table alone.
  const roles = useResource<{ items: Role[] }>("roles");
  const provinces = useResource<{ items: Province[] }>(
    "provinces",
    {},
    { enabled: canManageUsers },
  );
  const mutation = useMutation();

  // `/users/<id>` redirects here with `?id=<id>`: a link written down before the detail
  // page was removed arrives at that account alone, outlined, with a way back to all.
  const linked = filters.id || null;
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
          {linked ? (
            <p>
              <button
                type="button"
                className="button-link"
                onClick={() => setFilters({ ...filters, id: "" })}
              >
                عرض كل الحسابات
              </button>
            </p>
          ) : null}
          <ul className="profile-grid" data-testid="accounts">
            {users.data.items.map((user) => (
              <AccountCard
                key={user.id}
                user={user}
                focused={linked === user.id}
                canManageUsers={canManageUsers}
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
  focused,
  canManageUsers,
  onAction,
}: {
  user: AdminUser;
  focused: boolean;
  canManageUsers: boolean;
  onAction: (action: Action, user: AdminUser) => void;
}) {
  const owner = user.facilityCount > 0;
  const kind = !user.active ? "blocked" : owner ? "owner" : "plain";
  const status = user.lastSeenAt
    ? user.recentlyActive
      ? "نشط الآن"
      : `آخر ظهور ${relativeTime(user.lastSeenAt)}`
    : "لم يدخل بعد";
  return (
    <ProfileCard
      testId={`account-${user.id}`}
      focused={focused}
      kind={kind}
      tag={!user.active ? "محظور" : owner ? "صاحب منشأة" : "مستخدم"}
      live={user.recentlyActive}
      mark={initials(user.name)}
      name={user.name}
      phone={<span dir="ltr">{user.phone}</span>}
      meta={user.provinceName ?? "بلا محافظة"}
      status={status}
      stats={[
        { value: NUMBER.format(user.facilityCount), label: "منشآت" },
        { value: shortDate(user.createdAt), label: "التسجيل" },
        {
          value: user.lastLoginAt ? shortAgo(user.lastLoginAt) : "—",
          label: "آخر دخول",
        },
      ]}
      listTitle="المنشآت باسمه"
      items={user.facilities.map((facility) => ({
        key: facility.id,
        label: facility.nameAr,
        aside: MEMBER_ROLE[facility.role] ?? facility.role,
        tone: FACILITY_STATUS[facility.status]?.tone,
        toneLabel: FACILITY_STATUS[facility.status]?.label,
      }))}
      more={user.facilityCount - user.facilities.length}
      empty="لا منشأة باسمه"
      actions={
        canManageUsers ? (
          <>
            <button
              type="button"
              className="button-ghost"
              data-testid={`recovery-${user.id}`}
              onClick={() => onAction("recovery", user)}
            >
              رمز استعادة
            </button>
            {/* Only an operator ever sets up an authenticator, so only their card is
                offered the button that clears one. */}
            {user.hasTwoFactor ? (
              <button
                type="button"
                className="button-ghost"
                data-testid={`mfa-${user.id}`}
                onClick={() => onAction("mfa", user)}
              >
                تصفير التحقق
              </button>
            ) : null}
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
    />
  );
}

/** «٨/١٠/٢٦» — a date that fits a third of a narrow card; the month's name did not. */
function shortDate(value: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleDateString("ar-SY", { day: "numeric", month: "numeric", year: "2-digit" });
}

/** «٣ س», «أمس», «٥ أيام» — the last sign-in, short enough for a third of a card. */
function shortAgo(value: string): string {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60000));
  if (minutes < 60) return `${NUMBER.format(minutes)} د`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${NUMBER.format(hours)} س`;
  const days = Math.round(hours / 24);
  if (days === 1) return "أمس";
  return `${NUMBER.format(days)} يوم`;
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
