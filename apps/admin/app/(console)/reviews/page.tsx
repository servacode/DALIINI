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
  termsFor,
  labelsFor,
} from "../../../components/ui";
import { useLookups } from "../../../lib/client/use-lookups";
import { useResource } from "../../../lib/client/use-resource";
import { useUrlFilters } from "../../../lib/client/use-url-filters";

type Application = Readonly<{
  id: string;
  facilityId: string;
  facilityNameAr: string;
  kind: string;
  status: string;
  submittedAt: string | null;
  reviewedAt: string | null;
  evidenceComplete: boolean;
}>;

const STATUS = termsFor("applicationStatus");

// The two kinds the model declares. `INITIAL` is a first registration; `REVERIFICATION` is
// a facility asked to prove itself again.
const KIND = labelsFor("applicationKind");

const EVIDENCE = termsFor("evidenceState");

/**
 * The review queue.
 *
 * The filters are exactly the ones the contract declares on `adminReviewsList`, the
 * submission days and whether every required document is in. They are sent
 * through the generated client, which is why nothing here builds a query string.
 */
export default function ReviewsPage() {
  const [filters, setFilters] = useUrlFilters({
    status: "SUBMITTED",
    kind: "",
    province: "",
    category: "",
    from: "",
    to: "",
    evidence: "",
  });
  const lookups = useLookups();
  const queue = useResource<{ items: Application[] }>("reviews", filters, {
    refreshMs: 60_000,
  });

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
      key: "evidence",
      header: "الوثائق",
      render: (row) => {
        const state = EVIDENCE[row.evidenceComplete ? "COMPLETE" : "INCOMPLETE"];
        return <StatusBadge tone={state?.tone ?? "neutral"}>{state?.label}</StatusBadge>;
      },
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
    <div className="stack">
      <PageHeader title="طلبات المراجعة" description="طلبات التسجيل وإعادة التحقق." />
      <FilterBar
        fields={[
          {
            name: "status",
            label: "الحالة",
            type: "select",
            options: ["SUBMITTED", "APPROVED", "REJECTED"].map((value) => ({
              value,
              label: STATUS[value]?.label ?? value,
            })),
          },
          {
            name: "kind",
            label: "النوع",
            type: "select",
            options: ["INITIAL", "REVERIFICATION"].map((value) => ({
              value,
              label: KIND[value] ?? value,
            })),
          },
          lookups.provinceFilter,
          lookups.categoryFilter,
          {
            name: "evidence",
            label: "الوثائق",
            type: "select",
            options: [
              { value: "incomplete", label: EVIDENCE.INCOMPLETE?.label ?? "" },
              { value: "complete", label: EVIDENCE.COMPLETE?.label ?? "" },
            ],
          },
          { name: "from", label: "أُرسل من", type: "date" },
          { name: "to", label: "إلى", type: "date" },
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
    </div>
  );
}
