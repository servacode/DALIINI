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
import { LOCALE, withoutDirectionMarks } from "../../../lib/locale";

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

type Facility = Readonly<{ id: string; nameAr: string; role: string; status: string }>;

type Province = Readonly<{ id: string; nameAr: string }>;

type Action = "block" | "unblock" | "recovery" | "mfa";

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

/** «8/10/26» — a date that fits a third of a narrow card; the month's name did not. */
function shortDate(value: string | null): string {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : withoutDirectionMarks(
        date.toLocaleDateString(LOCALE, { day: "numeric", month: "numeric", year: "2-digit" }),
      );
}

/** «3 س», «أمس», «5 يوم» — the last sign-in, short enough for a third of a card. */
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
