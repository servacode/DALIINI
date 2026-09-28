"use client";

import Link from "next/link";
import { useState } from "react";

import {
  type Column,
  DataTable,
  ErrorState,
  FilterBar,
  LoadingState,
  PageHeader,
  StatusBadge,
  formatDateTime,
} from "../../../components/ui";
import { useResource } from "../../../lib/client/use-resource";

type AdminUser = Readonly<{
  id: string;
  name: string;
  phone: string;
  active: boolean;
  createdAt: string | null;
}>;

export default function UsersPage() {
  const [filters, setFilters] = useState<Record<string, string>>({ q: "", status: "" });
  const users = useResource<{ items: AdminUser[] }>("users", filters);

  const columns: readonly Column<AdminUser>[] = [
    {
      key: "name",
      header: "الاسم",
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
        ]}
        values={filters}
        onApply={setFilters}
      />
      {users.loading ? <LoadingState /> : null}
      {users.error ? <ErrorState error={users.error} onRetry={users.reload} /> : null}
      {users.data ? (
        <DataTable
          caption="المستخدمون"
          columns={columns}
          rows={users.data.items}
          rowKey={(row) => row.id}
        />
      ) : null}
    </div>
  );
}
