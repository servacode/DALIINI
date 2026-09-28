"use client";

import Link from "next/link";
import { useState } from "react";

import {
  type Column,
  Card,
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

type Application = Readonly<{
  id: string;
  facilityId: string;
  facilityNameAr: string;
  kind: string;
  status: string;
  submittedAt: string | null;
  reviewedAt: string | null;
}>;

const STATUS: Record<string, { label: string; tone: Tone }> = {
  SUBMITTED: { label: "قيد المراجعة", tone: "info" },
  APPROVED: { label: "مقبول", tone: "positive" },
  REJECTED: { label: "مرفوض", tone: "danger" },
  DRAFT: { label: "مسودة", tone: "neutral" },
};

// The two kinds the model declares. `INITIAL` is a first registration; `REVERIFICATION` is
// a facility asked to prove itself again.
const KIND: Record<string, string> = {
  INITIAL: "تسجيل أولي",
  REVERIFICATION: "إعادة تحقق",
};

/**
 * The review queue.
 *
 * The filters are exactly the four the contract declares on `adminReviewsList`. They are
 * sent through the generated client, which is why nothing here builds a query string.
 */
export default function ReviewsPage() {
  const [filters, setFilters] = useState<Record<string, string>>({
    status: "SUBMITTED",
    kind: "",
  });
  const queue = useResource<{ items: Application[] }>("reviews", filters);

  const columns: readonly Column<Application>[] = [
    {
      key: "facility",
      header: "المنشأة",
      render: (row) => <Link href={`/reviews/${row.id}`}>{row.facilityNameAr}</Link>,
    },
    { key: "kind", header: "النوع", render: (row) => KIND[row.kind] ?? row.kind },
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
      key: "submittedAt",
      header: "تاريخ الإرسال",
      ltr: true,
      render: (row) => formatDateTime(row.submittedAt),
    },
    {
      key: "open",
      header: "",
      width: "1%",
      render: (row) => (
        <Link className="button-ghost" href={`/reviews/${row.id}`} data-testid={`open-${row.id}`}>
          فتح
        </Link>
      ),
    },
  ];

  return (
    <div className="operation-stack">
      <PageHeader
        eyebrow="العمليات"
        title="طلبات المراجعة"
        description="طلبات التسجيل وإعادة التحقق."
      />
      <Card flush>
        <FilterBar
          fields={[
            {
              name: "status",
              label: "الحالة",
              type: "select",
              options: [
                { value: "SUBMITTED", label: "قيد المراجعة" },
                { value: "APPROVED", label: "مقبول" },
                { value: "REJECTED", label: "مرفوض" },
              ],
            },
            {
              name: "kind",
              label: "النوع",
              type: "select",
              options: [
                { value: "INITIAL", label: "تسجيل أولي" },
                { value: "REVERIFICATION", label: "إعادة تحقق" },
              ],
            },
            { name: "province", label: "معرّف المحافظة", placeholder: "UUID" },
            { name: "category", label: "معرّف التصنيف", placeholder: "UUID" },
          ]}
          values={filters}
          onApply={setFilters}
        />
        {queue.loading ? <LoadingState /> : null}
        {queue.error ? <ErrorState error={queue.error} onRetry={queue.reload} /> : null}
        {queue.data ? (
          <DataTable
            caption="طلبات المراجعة"
            columns={columns}
            rows={queue.data.items}
            rowKey={(row) => row.id}
          />
        ) : null}
      </Card>
    </div>
  );
}
