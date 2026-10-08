"use client";

import Link from "next/link";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  type Column,
  DataTable,
  ErrorState,
  FieldError,
  FilterBar,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
  formatDateTime,
  Pagination,
  pageSummary,
} from "../../../components/ui";
import { SidePanel } from "../../../components/ui/extra";
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
  createdAt: string | null;
}>;

type Province = Readonly<{ id: string; nameAr: string }>;

/**
 * Accounts, filtered and ordered from the address bar, so a link (a role's holders, a search)
 * opens the same view for whoever follows it.
 */
export default function UsersPage() {
  const [filters, setFilters] = useUrlFilters({ q: "", status: "", role: "", ordering: "" });
  const users = useCursorPage<AdminUser>("users", filters);
  // Roles sit behind their own permission; without it the filter still offers "any role"
  // and "no role", which the backend answers from the user table alone.
  const roles = useResource<{ items: { id: string; code: string; name: string }[] }>("roles");
  const canManage = useCan("admin.users.manage");
  const provinces = useResource<{ items: Province[] }>("provinces", {}, { enabled: canManage });
  const mutation = useMutation();
  // The new-account sheet, open when there is a draft. Opening an account from here is how
  // an operator is appointed on a running platform; before this it needed a shell on the
  // production server, so in practice it happened once and never again.
  const [draft, setDraft] = useState<{ name: string; phone: string; provinceId: string } | null>(
    null,
  );
  const [toast, setToast] = useState<string | null>(null);

  async function create(): Promise<void> {
    if (!draft) return;
    if (await mutation.run("userCreate", draft)) {
      setDraft(null);
      setToast("فُتح الحساب. أرسل له رمز استعادة من صفحته ليختار كلمة مروره.");
      users.reload();
    }
  }

  const columns: readonly Column<AdminUser>[] = [
    {
      key: "name",
      header: "الاسم",
      required: true,
      sortKey: "name",
      render: (row) => <Link href={`/users/${row.id}`}>{row.name}</Link>,
    },
    { key: "phone", header: "الهاتف", ltr: true, render: (row) => row.phone },
    {
      key: "province",
      header: "المحافظة",
      render: (row) => row.provinceName ?? <span className="muted">—</span>,
    },
    {
      key: "active",
      header: "الحالة",
      render: (row) => (
        <StatusBadge tone={row.active ? "positive" : "danger"}>
          {row.active ? "فعّال" : "محظور"}
        </StatusBadge>
      ),
    },
    {
      key: "createdAt",
      header: "تاريخ الإنشاء",
      ltr: true,
      sortKey: "createdAt",
      sortFirst: "desc",
      render: (row) => formatDateTime(row.createdAt),
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="الحسابات"
        description="كل حساب مسجّل في التطبيق: حالته ومحافظته وما يملكه."
        actions={
          canManage ? (
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
              ...(roles.data?.items ?? []).map((role) => ({ value: role.code, label: role.name })),
            ],
          },
        ]}
        values={filters}
        onApply={setFilters}
      />
      {users.loading ? <LoadingState /> : null}
      {users.error ? <ErrorState error={users.error} onRetry={users.reload} /> : null}
      {users.data ? (
        <DataTable
          id="users"
          caption="المستخدمون"
          columns={columns}
          rows={users.data.items}
          rowKey={(row) => row.id}
          sort={filters.ordering}
          onSort={(ordering) => setFilters({ ...filters, ordering })}
          summary={pageSummary(users.data.items.length, users.data.hasMore)}
        />
      ) : null}
      {users.pagination ? <Pagination {...users.pagination} /> : null}

      <SidePanel
        open={draft !== null}
        title="حساب جديد"
        description="اللوحة لا تضع كلمة مرور لأحد. يُفتح الحساب بلا كلمة، ثم تُرسل له رمز استعادة من صفحته ليختار كلمته بنفسه."
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
                mutation.pending || !draft?.name.trim() || !draft?.phone.trim() || !draft?.provinceId
              }
              onClick={create}
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
