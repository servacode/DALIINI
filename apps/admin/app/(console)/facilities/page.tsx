"use client";

import Link from "next/link";

import { useCan } from "../../../components/admin-shell";
import { ExportButton } from "../../../components/export-button";

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
  Pagination,
  pageSummary,
} from "../../../components/ui";
import { useLookups } from "../../../lib/client/use-lookups";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useUrlFilters } from "../../../lib/client/use-url-filters";

type Facility = Readonly<{
  id: string;
  nameAr: string;
  nameEn: string | null;
  status: string;
  provinceId: string;
  categoryId: string;
  updatedAt: string | null;
  categoryNameAr?: string;
  provinceNameAr?: string;
  ownerName?: string | null;
  qualityScore?: number;
  qualityIssues?: readonly string[];
}>;

/** What lowers a listing's quality, in the operator's words. */
export const QUALITY_ISSUES: Record<string, string> = {
  NO_PHOTOS: "بلا صور",
  NO_HOURS: "بلا أوقات دوام",
  NO_LOCATION: "بلا موقع",
  NO_PHONE: "بلا هاتف",
  STALE: "لم تُحدَّث منذ ٩٠ يوماً",
  OPEN_REPORTS: "عليها بلاغات",
  NOT_VERIFIED_RECENTLY: "لم يُتحقق منها مؤخراً",
};

export function QualityMeter({ score }: { score: number }) {
  const tone = score >= 80 ? "positive" : score >= 50 ? "warning" : "danger";
  return (
    <span className="quality" data-tone={tone} title={`مؤشر الجودة ${score} من 100`}>
      <span className="quality-bar" aria-hidden="true">
        <span style={{ inlineSize: `${Math.max(4, score)}%` }} />
      </span>
      <span className="tabular">{score}</span>
    </span>
  );
}

export const STATUS = termsFor("facilityStatus");

/** The four filters here are the ones INT-041 declared; the client sends them typed. */
export default function FacilitiesPage() {
  const [filters, setFilters] = useUrlFilters({
    q: "",
    status: "",
    province: "",
    category: "",
    issue: "",
    ordering: "",
  });
  const lookups = useLookups();
  const facilities = useCursorPage<Facility>("facilities", filters);
  const canEdit = useCan("admin.facilities.edit");

  const columns: readonly Column<Facility>[] = [
    {
      key: "name",
      header: "المنشأة",
      required: true,
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
      key: "quality",
      header: "الجودة",
      // The weakest listings first: the order this column is for.
      sortKey: "qualityScore",
      render: (row) =>
        row.qualityScore === undefined ? (
          <span className="muted">—</span>
        ) : (
          <span className="quality-cell">
            <QualityMeter score={row.qualityScore} />
            {row.qualityIssues && row.qualityIssues.length > 0 ? (
              <span className="muted quality-issues">
                {row.qualityIssues.map((code) => QUALITY_ISSUES[code] ?? code).join("، ")}
              </span>
            ) : null}
          </span>
        ),
    },
    {
      key: "category",
      header: "التصنيف",
      render: (row) => row.categoryNameAr ?? lookups.categoryName(row.categoryId),
    },
    {
      key: "province",
      header: "المحافظة",
      render: (row) => row.provinceNameAr ?? lookups.provinceName(row.provinceId),
    },
    {
      key: "owner",
      header: "المالك",
      hiddenByDefault: true,
      render: (row) => row.ownerName ?? <span className="muted">—</span>,
    },
    {
      key: "updatedAt",
      header: "آخر تحديث",
      ltr: true,
      sortKey: "updatedAt",
      sortFirst: "desc",
      render: (row) => formatDateTime(row.updatedAt),
    },
    {
      key: "open",
      header: "",
      width: "1%",
      required: true,
      render: (row) => (
        <Link className="button-ghost" href={`/facilities/${row.id}`}>
          فتح
        </Link>
      ),
    },
  ];

  return (
    <div className="stack">
      <PageHeader
        title="المنشآت"
        description="متابعة الحالة التشغيلية للمنشآت وإدارتها."
        actions={
          <>
            {/* The file holds what the filters hold, without the page's limit. */}
            <ExportButton name="facilities" params={filters} />
            {canEdit ? (
              <Link className="button-primary" href="/facilities/new" data-testid="facility-new">
                إضافة منشأة
              </Link>
            ) : null}
          </>
        }
      />
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
          lookups.provinceFilter,
          lookups.categoryFilter,
          {
            name: "issue",
            label: "مشكلة في البيانات",
            type: "select",
            options: Object.entries(QUALITY_ISSUES).map(([value, label]) => ({ value, label })),
          },
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
          id="facilities"
          caption="المنشآت"
          columns={columns}
          rows={facilities.data.items}
          rowKey={(row) => row.id}
          sort={filters.ordering}
          onSort={(ordering) => setFilters({ ...filters, ordering })}
          summary={pageSummary(facilities.data.items.length, facilities.data.hasMore)}
        />
      ) : null}
      {facilities.pagination ? <Pagination {...facilities.pagination} /> : null}
    </div>
  );
}
