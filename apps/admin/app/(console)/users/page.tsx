"use client";

import Link from "next/link";

import {
  type Column,
  DataTable,
  ErrorState,
  FilterBar,
  LoadingState,
  PageHeader,
  StatusBadge,
  formatDateTime,
  Pagination,
  pageSummary,
} from "../../../components/ui";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useResource } from "../../../lib/client/use-resource";
import { useUrlFilters } from "../../../lib/client/use-url-filters";

type AdminUser = Readonly<{
  id: string;
  name: string;
  phone: string;
  active: boolean;
  createdAt: string | null;
}>;

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
      <PageHeader title="المستخدمون" description="الحسابات وحالتها والأدوار الإدارية." />
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
    </div>
  );
}
