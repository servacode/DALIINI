"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import { Icon } from "../../../components/icons";
import {
  ConfirmDialog,
  EmptyState,
  ErrorState,
  FieldError,
  LoadingState,
  Pagination,
  termsFor,
  Toast,
} from "../../../components/ui";
import {
  counted,
  FilterChips,
  FormDialog,
  PageHero,
  ProfileCard,
  SearchBox,
  relativeTime,
} from "../../../components/ui/extra";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";
import { useUrlFilters } from "../../../lib/client/use-url-filters";
import { LOCALE } from "../../../lib/locale";

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

type Created = AdminUser & Readonly<{ codeSent: boolean; codeError: string | null }>;

type Facility = Readonly<{
  id: string;
  nameAr: string;
  role: string;
  status: string;
  imageUrl: string | null;
}>;

type Province = Readonly<{ id: string; nameAr: string }>;

type Action = "block" | "unblock" | "recovery" | "mfa" | "delete" | "roles";

type Role = Readonly<{ id: number; code: string; name: string; permissions: readonly string[] }>;

/** Which accounts the list shows. One choice, so it is one row of chips, not three menus. */
type Show = "" | "owners" | "users" | "blocked";

const NUMBER = new Intl.NumberFormat(LOCALE);
const MEMBER_ROLE: Record<string, string> = { OWNER: "مالك", MANAGER: "مدير" };
const FACILITY_STATUS = termsFor("facilityStatus");

const SHOW: readonly { value: Show; label: string }[] = [
  { value: "", label: "الكل" },
  { value: "owners", label: "أصحاب منشآت" },
  { value: "users", label: "مستخدمون" },
  { value: "blocked", label: "محظورون" },
];

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
 * Every account registered in the app, each one an identity card that holds all of itself
 * (DECISION-106), on a page in the cards' own language (DECISION-107).
 *
 * The filters apply as they change — a search after a pause in typing, a chip the moment it
 * is pressed — so there is no «apply» button for the operator to remember. Opening an account
 * is a short form in a window, and the account's first code goes to WhatsApp by itself.
 */
export default function UsersPage() {
  const [filters, setFilters] = useUrlFilters({
    q: "",
    show: "",
    ordering: "",
    id: "",
  });
  // The chips are one choice for the reader and two parameters for the server: who runs a
  // place is `kind`, who is shut is `status`.
  const show = (filters.show || "") as Show;
  const query = {
    q: filters.q,
    ordering: filters.ordering,
    id: filters.id,
    kind: show === "owners" || show === "users" ? show : "",
    status: show === "blocked" ? "blocked" : "",
  };
  const users = useCursorPage<AdminUser>("users", query);
  const canManageUsers = useCan("admin.users.manage");
  const canManageRoles = useCan("admin.roles.manage");
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
  const [created, setCreated] = useState<Created | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  function ask(action: Action, user: AdminUser): void {
    mutation.reset();
    setDialog({ action, user });
  }

  function closeNew(): void {
    setDraft(null);
    setCreated(null);
  }

  return (
    <div className="stack">
      <PageHero
        eyebrow="إدارة المستخدمين"
        title="الحسابات"
        description="كل حساب مسجّل في التطبيق، ببطاقته: من هو، وأين، وما المنشآت التي باسمه، ومتى كان آخر ظهور له."
        action={
          canManageUsers ? (
            <button
              type="button"
              className="page-hero-action"
              data-testid="new-account"
              onClick={() => {
                mutation.reset();
                setCreated(null);
                setDraft({ name: "", phone: "", provinceId: "" });
              }}
            >
              <Icon name="plus" />
              حساب جديد
            </button>
          ) : null
        }
      />

      <div className="live-toolbar" data-testid="accounts-toolbar">
        <SearchBox
          value={filters.q}
          placeholder="ابحث باسم أو رقم هاتف"
          testId="accounts-search"
          onChange={(q) => setFilters({ ...filters, q, id: "" })}
        />
        <FilterChips
          label="عرض"
          value={show}
          options={SHOW}
          testId="accounts-show"
          onChange={(next) => setFilters({ ...filters, show: next, id: "" })}
        />
        <select
          className="toolbar-select"
          aria-label="الترتيب"
          value={filters.ordering}
          data-testid="accounts-ordering"
          onChange={(event) => setFilters({ ...filters, ordering: event.target.value })}
        >
          <option value="">الأحدث تسجيلاً</option>
          <option value="createdAt">الأقدم تسجيلاً</option>
          <option value="name">الاسم (أ إلى ي)</option>
          <option value="-name">الاسم (ي إلى أ)</option>
        </select>
      </div>

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

      {users.loading ? <LoadingState /> : null}
      {users.error ? <ErrorState error={users.error} onRetry={users.reload} /> : null}

      {users.data && users.data.items.length === 0 ? (
        <EmptyState
          title="لا حساب يطابق البحث."
          hint="جرّب اسماً أو رقماً آخر، أو اختر «الكل»."
          illustration="noResults"
        />
      ) : null}

      {users.data && users.data.items.length > 0 ? (
        <ul className="profile-grid" data-testid="accounts">
          {users.data.items.map((user, index) => (
            <AccountCard
              key={user.id}
              index={index}
              user={user}
              focused={linked === user.id}
              canManageUsers={canManageUsers}
              canManageRoles={canManageRoles}
              onAction={ask}
            />
          ))}
        </ul>
      ) : null}

      {users.pagination ? <Pagination {...users.pagination} /> : null}

      {dialog?.action === "roles" ? (
        <RolesDialog
          user={dialog.user}
          onDone={(message) => {
            setDialog(null);
            setToast(message);
            users.reload();
          }}
          onCancel={() => setDialog(null)}
        />
      ) : null}

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

      <FormDialog
        open={draft !== null}
        icon={created ? "check" : "user"}
        title={created ? "فُتح الحساب" : "حساب جديد"}
        description={
          created
            ? created.name
            : "لا تُكتب كلمة مرور هنا. يصل صاحب الحساب رمز على واتساب ويختار كلمته بنفسه."
        }
        onClose={closeNew}
        locked={mutation.pending}
        testId="new-account-dialog"
        footer={
          created ? (
            <button type="button" className="button-primary" onClick={closeNew}>
              تمّ
            </button>
          ) : (
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
                  const result = await mutation.runFor<Created>("userCreate", {
                    ...draft,
                    phone: `0${draft.phone.replace(/^0+/, "")}`,
                  });
                  if (result) {
                    setCreated(result);
                    users.reload();
                  }
                }}
              >
                {mutation.pending ? "جارٍ الفتح…" : "فتح الحساب وإرسال الرمز"}
              </button>
              <button
                type="button"
                className="button-ghost"
                disabled={mutation.pending}
                onClick={closeNew}
              >
                إلغاء
              </button>
            </>
          )
        }
      >
        {created ? (
          <div
            className={created.codeSent ? "form-result" : "form-result form-result-warn"}
            data-testid="new-account-result"
          >
            <span className="form-result-mark" aria-hidden="true">
              <Icon name={created.codeSent ? "whatsapp" : "alert"} width={26} height={26} />
            </span>
            <strong>{created.codeSent ? "أُرسل الرمز على واتساب" : "لم يُرسل الرمز"}</strong>
            <p>
              {created.codeSent
                ? `وصل الرمز إلى ${created.phone}. يفتح صاحبه التطبيق ويختار «نسيت كلمة المرور» برقمه، فيكتب الرمز ويضع كلمته.`
                : `${created.codeError ?? "تعذّر الإرسال."} الحساب مفتوح، وتستطيع إعادة الإرسال من بطاقته بزرّ «رمز استعادة».`}
            </p>
          </div>
        ) : draft ? (
          <>
            <label className="field">
              <span>الاسم</span>
              <input
                data-testid="account-name"
                value={draft.name}
                maxLength={120}
                placeholder="الاسم الكامل"
                onChange={(event) => setDraft({ ...draft, name: event.target.value })}
              />
            </label>
            <label className="field">
              <span>رقم الهاتف (واتساب)</span>
              <span className="phone-input">
                <span>+963</span>
                <input
                  data-testid="account-phone"
                  inputMode="tel"
                  placeholder="9XX XXX XXX"
                  value={draft.phone}
                  onChange={(event) =>
                    setDraft({ ...draft, phone: event.target.value.replace(/[^\d]/g, "") })
                  }
                />
              </span>
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
          </>
        ) : null}
      </FormDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}

function AccountCard({
  user,
  index,
  focused,
  canManageUsers,
  canManageRoles,
  onAction,
}: {
  user: AdminUser;
  index: number;
  focused: boolean;
  canManageUsers: boolean;
  canManageRoles: boolean;
  onAction: (action: Action, user: AdminUser) => void;
}) {
  const owner = user.facilityCount > 0;
  const kind = !user.active ? "blocked" : owner ? "owner" : "plain";
  // No live session is not «never signed in»: someone who signed out has none either, and
  // the card said «لم يدخل بعد» beside their own «آخر دخول».
  const activity = user.lastSeenAt
    ? user.recentlyActive
      ? "نشط الآن"
      : `آخر ظهور ${relativeTime(user.lastSeenAt)}`
    : user.lastLoginAt
      ? `آخر دخول ${relativeTime(user.lastLoginAt)}`
      : "لم يدخل بعد";
  return (
    <ProfileCard
      testId={`account-${user.id}`}
      index={index}
      focused={focused}
      kind={kind}
      tag={
        !user.active
          ? { label: "محظور", icon: "lock" }
          : owner
            ? { label: "صاحب منشأة", icon: "building" }
            : { label: "مستخدم", icon: "user" }
      }
      live={user.recentlyActive}
      mark={initials(user.name)}
      name={user.name}
      tiles={[
        { icon: "phone", value: user.phone, label: "رقم الجوال", ltr: true },
        {
          icon: "mapPin",
          tone: "gold",
          value: user.provinceName ?? "—",
          label: "المحافظة",
        },
      ]}
      pills={[
        user.active
          ? { label: "فعّال", tone: "positive", icon: "checkCircle" }
          : { label: "محظور", tone: "danger", icon: "lock" },
        { label: activity, tone: user.recentlyActive ? "positive" : undefined },
      ]}
      places={{
        title: facilitiesPhrase(user.facilityCount),
        subtitle: "المنشآت التابعة للحساب",
        empty: "لا منشأة باسمه",
        more: user.facilityCount - user.facilities.length,
        items: user.facilities.map((facility) => ({
          key: facility.id,
          label: facility.nameAr,
          aside: MEMBER_ROLE[facility.role] ?? facility.role,
          image: facility.imageUrl,
          state: FACILITY_STATUS[facility.status]?.label,
          stateTone: FACILITY_STATUS[facility.status]?.tone,
        })),
      }}
      dates={[
        { icon: "calendar", label: "تاريخ التسجيل", value: isoDay(user.createdAt) },
        {
          icon: "clock",
          tone: "info",
          label: "آخر دخول",
          value: user.lastLoginAt ? isoDay(user.lastLoginAt) : "لم يدخل بعد",
        },
      ]}
      actions={
        canManageUsers ? (
          <>
            <button
              type="button"
              className="profile-act-main"
              data-testid={`recovery-${user.id}`}
              onClick={() => onAction("recovery", user)}
            >
              <Icon name="whatsapp" width={16} height={16} />
              إرسال رمز استعادة
            </button>
            {user.active ? (
              <button
                type="button"
                className="profile-act-danger"
                data-testid={`block-${user.id}`}
                onClick={() => onAction("block", user)}
              >
                <Icon name="lock" width={15} height={15} />
                حظر
              </button>
            ) : (
              <button
                type="button"
                className="profile-act-danger"
                data-testid={`unblock-${user.id}`}
                onClick={() => onAction("unblock", user)}
              >
                <Icon name="lock" width={15} height={15} />
                رفع الحظر
              </button>
            )}
            {/* Deletion at the owner's request (Google Play requires it from outside the app):
                rare and final, so a small button that opens a plain warning. */}
            <button
              type="button"
              className="profile-act-icon"
              data-testid={`delete-${user.id}`}
              title="حذف الحساب بطلب صاحبه"
              aria-label={`حذف حساب ${user.name} بطلب صاحبه`}
              onClick={() => onAction("delete", user)}
            >
              <Icon name="trash" width={16} height={16} />
            </button>
            {/* Only an operator ever sets up an authenticator, so only their card carries the
                small button that clears one. */}
            {user.hasTwoFactor ? (
              <button
                type="button"
                className="profile-act-icon"
                data-testid={`mfa-${user.id}`}
                title="تصفير التحقق بخطوتين"
                aria-label="تصفير التحقق بخطوتين"
                onClick={() => onAction("mfa", user)}
              >
                <Icon name="shield" width={16} height={16} />
              </button>
            ) : null}
            {canManageRoles ? (
              <button
                type="button"
                className="profile-act-main"
                data-testid={`roles-${user.id}`}
                onClick={() => onAction("roles", user)}
              >
                الأدوار
              </button>
            ) : null}
          </>
        ) : null
      }
    />
  );
}

/**
 * How many places, said as Arabic says it: «منشأة واحدة», «منشأتان», «3 منشآت», «11 منشأة».
 * The mockup's «2 منشأة» reads as a slip, and on a page of cards it would be one on most of them.
 */
function facilitiesPhrase(count: number): string {
  if (count === 0) return "لا منشآت";
  if (count === 1) return "منشأة واحدة";
  if (count === 2) return "منشأتان";
  const n = NUMBER.format(count);
  const lastTwo = count % 100;
  return lastTwo >= 3 && lastTwo <= 10 ? `${n} منشآت` : `${n} منشأة`;
}

/** «2024/01/15» — year first, as in the owner's mockup, and the same width on every card. */
function isoDay(value: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const parts = new Intl.DateTimeFormat("en-CA", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone: "Asia/Damascus",
  }).format(date);
  return parts.replace(/-/g, "/");
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
        open={dialog?.action === "delete"}
        title={`حذف حساب ${user?.name ?? ""}`}
        body="لطلب حذف وصلك من صاحب الحساب نفسه، عبر صفحة حذف الحساب أو البريد. يُمسح الاسم والرقم والعنوان والصورة والمحفوظات والإشعارات، وتخرج كل أجهزته، ولا يمكن التراجع. التقييمات تبقى بلا اسم لأنها جزء من تقييم المنشأة. تحقق من هوية صاحب الطلب قبل التأكيد."
        confirmLabel="حذف الحساب نهائياً"
        destructive
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={() => run("userDelete", "حُذف الحساب وسُجّل الحذف في سجل التدقيق باسمك.")}
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

/**
 * Which console roles an account holds. The card redesign dropped this with the details it
 * used to open, and nothing else gave anyone a role: a reviewer could be described in «الأدوار
 * والصلاحيات» but never made.
 */
function RolesDialog({
  user,
  onDone,
  onCancel,
}: {
  user: AdminUser;
  onDone: (message: string) => void;
  onCancel: () => void;
}) {
  const detail = useResource<{ roleIds: readonly number[] }>("user", { id: user.id });
  const roles = useResource<{ items: Role[] }>("roles");
  const mutation = useMutation();
  const [picked, setPicked] = useState<string[] | null>(null);
  const selected = picked ?? (detail.data?.roleIds ?? []).map(String);
  const loading = detail.loading || roles.loading;
  const failed = detail.error ?? roles.error;

  return (
    <FormDialog
      open
      icon="shield"
      title={`أدوار ${user.name}`}
      description="ما يستطيع هذا الحساب فعله داخل اللوحة. حساب بلا دور لا يرى اللوحة أصلاً."
      onClose={onCancel}
      locked={mutation.pending}
      testId="roles-dialog"
      footer={
        <>
          <button
            type="button"
            className="button-primary"
            data-testid="save-roles"
            disabled={mutation.pending || picked === null}
            onClick={async () => {
              if (await mutation.run("userRoles", { id: user.id, roleIds: selected })) {
                onDone("حُفظت الأدوار وسُجّلت في سجل التدقيق.");
              }
            }}
          >
            {mutation.pending ? "جارٍ الحفظ…" : "حفظ الأدوار"}
          </button>
          <button
            type="button"
            className="button-ghost"
            disabled={mutation.pending}
            onClick={onCancel}
          >
            إلغاء
          </button>
        </>
      }
    >
      {loading ? <LoadingState /> : null}
      {failed ? (
        <ErrorState error={failed} onRetry={detail.error ? detail.reload : roles.reload} />
      ) : null}
      {!loading && !failed && roles.data ? (
        <div>
          {roles.data.items.map((role) => (
            <label key={role.id} className="switch-row">
              <span>
                <strong>{role.name}</strong>{" "}
                <span className="muted">
                  ({counted(role.permissions.length, "صلاحية واحدة", "صلاحيتان", "صلاحيات", "صلاحية")})
                </span>
              </span>
              <input
                type="checkbox"
                data-testid={`role-${role.code}`}
                checked={selected.includes(String(role.id))}
                onChange={(event) =>
                  setPicked(
                    event.target.checked
                      ? [...selected, String(role.id)]
                      : selected.filter((id) => id !== String(role.id)),
                  )
                }
              />
            </label>
          ))}
        </div>
      ) : null}
      <FieldError id="roles-error" message={mutation.error?.message} />
    </FormDialog>
  );
}
