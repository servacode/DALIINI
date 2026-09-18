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
  type Tone,
  formatDateTime,
} from "../../../components/ui";
import { useResource } from "../../../lib/client/use-resource";

type Facility = Readonly<{
  id: string;
  nameAr: string;
  nameEn: string | null;
  status: string;
  provinceId: string;
  categoryId: string;
  updatedAt: string | null;
}>;

export const STATUS: Record<string, { label: string; tone: Tone }> = {
  DRAFT: { label: "مسودة", tone: "neutral" },
  SUBMITTED: { label: "قيد المراجعة", tone: "info" },
  ACTIVE: { label: "فعّالة", tone: "positive" },
  SUSPENDED: { label: "موقوفة", tone: "warning" },
  CLOSED: { label: "مغلقة", tone: "danger" },
  REVERIFICATION_REQUIRED: { label: "تحتاج إعادة تحقق", tone: "warning" },
};

/** The four filters here are the ones INT-041 declared; the client sends them typed. */
export default function FacilitiesPage() {
  const [filters, setFilters] = useState<Record<string, string>>({ q: "", status: "" });
  const facilities = useResource<{ items: Facility[] }>("facilities", filters);

  const columns: readonly Column<Facility>[] = [
    {
      key: "name",
      header: "المنشأة",
      render: (row) => <Link href={`/facilities/${row.id}`}>{row.nameAr}</Link>,
    },
    {
      key: "status",
      header: "الحالة",
      render: (row) => (
        <StatusBadge tone={STATUS[row.status]?.tone ?? "neutral"}>
          {STATUS[row.status]?.label ?? row.status}
        </StatusBadge>
      ),
    },
    {
      key: "updatedAt",
      header: "آخر تحديث",
      ltr: true,
      render: (row) => formatDateTime(row.updatedAt),
    },
    {
      key: "open",
      header: "",
      width: "1%",
      render: (row) => (
        <Link className="button-ghost" href={`/facilities/${row.id}`}>
          فتح
        </Link>
      ),
    },
  ];

  return (
    <div className="stack">
      <PageHeader title="المنشآت" description="متابعة الحالة التشغيلية للمنشآت وإدارتها." />
      <FilterBar
        fields={[
          { name: "q", label: "بحث", placeholder: "اسم المنشأة" },
          {
            name: "status",
            label: "الحالة",
            type: "select",
            options: Object.entries(STATUS).map(([value, meta]) => ({
              value,
              label: meta.label,
            })),
          },
          { name: "province", label: "معرّف المحافظة", placeholder: "UUID" },
          { name: "category", label: "معرّف التصنيف", placeholder: "UUID" },
        ]}
        values={filters}
        onApply={setFilters}
      />
      {facilities.loading ? <LoadingState /> : null}
      {facilities.error ? (
        <ErrorState error={facilities.error} onRetry={facilities.reload} />
      ) : null}
      {facilities.data ? (
        <DataTable
          caption="المنشآت"
          columns={columns}
          rows={facilities.data.items}
          rowKey={(row) => row.id}
        />
      ) : null}
    </div>
  );
}
