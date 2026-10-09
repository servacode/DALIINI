"use client";

import Link from "next/link";
import { useState } from "react";

import { Icon } from "../../../components/icons";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Pagination,
  formatDateTime,
  labelsFor,
  termsFor,
} from "../../../components/ui";
import {
  FilterChips,
  ItemCard,
  type ItemTone,
  relativeTime,
} from "../../../components/ui/extra";
import { addDays, damascusDay } from "../../../lib/client/calendar";
import { useCursorPage } from "../../../lib/client/use-cursor-page";
import { useLookups } from "../../../lib/client/use-lookups";
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

const STATUSES = [
  { value: "SUBMITTED", label: "بانتظار القرار" },
  { value: "APPROVED", label: "المقبولة" },
  { value: "REJECTED", label: "المرفوضة" },
] as const;
type StatusChoice = (typeof STATUSES)[number]["value"];

/** How far back a submission may be, as chips: the dates themselves are rarely what is meant. */
const SINCE = [
  { value: "", label: "أي وقت" },
  { value: "7", label: "آخر 7 أيام" },
  { value: "30", label: "آخر 30 يوماً" },
] as const;
type Since = (typeof SINCE)[number]["value"];

function toneOf(application: Application): ItemTone {
  if (application.status === "APPROVED") return "brand";
  if (application.status === "REJECTED") return "danger";
  return application.evidenceComplete ? "info" : "gold";
}

function stateTone(tone: string | undefined): "positive" | "danger" | "warning" | "info" | undefined {
  if (tone === "positive" || tone === "danger" || tone === "warning" || tone === "info") return tone;
  return tone === "accent" || tone === "brand" ? "info" : undefined;
}

/**
 * The review queue, each application a card (DECISION-113).
 *
 * The filters are exactly the ones the contract declares on `adminReviewsList` — status,
 * kind, province, category, whether every required document is in, and the submission days
 * — and each applies the moment it is chosen. They are sent through the generated client,
 * which is why nothing here builds a query string.
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
  const [today] = useState(() => damascusDay(new Date()));
  const lookups = useLookups();
  const { provinceFilter, categoryFilter } = lookups;
  const queue = useCursorPage<Application>("reviews", filters, {
    refreshMs: 60_000,
  });

  const since: Since =
    filters.from === addDays(today, -6) ? "7" : filters.from === addDays(today, -29) ? "30" : "";
  const status = (STATUSES.find((s) => s.value === filters.status)?.value ??
    "SUBMITTED") as StatusChoice;

  return (
    <div className="stack">
      <PageHeader
        eyebrow="المراجعات والبلاغات"
        title="طلبات المراجعة"
        description="طلبات التسجيل وإعادة التحقق: افتح الطلب، راجع وثائقه، واقبله أو ارفضه بسبب."
      />

      <div className="live-toolbar">
        <FilterChips
          label="الحالة"
          value={status}
          options={STATUSES}
          testId="filter-status"
          onChange={(next) => setFilters({ ...filters, status: next })}
        />
        <select
          className="toolbar-select"
          aria-label="النوع"
          value={filters.kind}
          data-testid="filter-kind"
          onChange={(event) => setFilters({ ...filters, kind: event.target.value })}
        >
          <option value="">كل الأنواع</option>
          {["INITIAL", "REVERIFICATION"].map((value) => (
            <option key={value} value={value}>
              {KIND[value] ?? value}
            </option>
          ))}
        </select>
        {provinceFilter.options ? (
          <select
            className="toolbar-select"
            aria-label="المحافظة"
            value={filters.province}
            data-testid="filter-province"
            onChange={(event) => setFilters({ ...filters, province: event.target.value })}
          >
            <option value="">كل المحافظات</option>
            {provinceFilter.options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        ) : null}
        {categoryFilter.options ? (
          <select
            className="toolbar-select"
            aria-label="التصنيف"
            value={filters.category}
            data-testid="filter-category"
            onChange={(event) => setFilters({ ...filters, category: event.target.value })}
          >
            <option value="">كل التصنيفات</option>
            {categoryFilter.options.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        ) : null}
        <select
          className="toolbar-select"
          aria-label="الوثائق"
          value={filters.evidence}
          data-testid="filter-evidence"
          onChange={(event) => setFilters({ ...filters, evidence: event.target.value })}
        >
          <option value="">كل الوثائق</option>
          <option value="incomplete">{EVIDENCE.INCOMPLETE?.label ?? ""}</option>
          <option value="complete">{EVIDENCE.COMPLETE?.label ?? ""}</option>
        </select>
        <FilterChips
          label="أُرسل"
          value={since}
          options={SINCE}
          testId="filter-since"
          onChange={(next) =>
            setFilters({
              ...filters,
              from: next ? addDays(today, -(Number(next) - 1)) : "",
              to: "",
            })
          }
        />
      </div>

      {queue.loading ? <LoadingState /> : null}
      {queue.error ? <ErrorState error={queue.error} onRetry={queue.reload} /> : null}

      {queue.data && queue.data.items.length === 0 ? (
        <EmptyState
          title={status === "SUBMITTED" ? "لا طلبات تنتظر قراراً." : "لا طلبات في هذا العرض."}
          hint={status === "SUBMITTED" ? "كل ما وصل رُوجع." : "جرّب حالة أو فترة أخرى."}
          illustration={status === "SUBMITTED" ? "success" : "noResults"}
        />
      ) : null}

      {queue.data && queue.data.items.length > 0 ? (
        <ul className="profile-grid" data-testid="reviews">
          {queue.data.items.map((row, index) => {
            const evidence = EVIDENCE[row.evidenceComplete ? "COMPLETE" : "INCOMPLETE"];
            const href = `/reviews/${row.id}`;
            return (
              <ItemCard
                key={row.id}
                index={index}
                icon={row.kind === "REVERIFICATION" ? "refresh" : "inbox"}
                tone={toneOf(row)}
                title={
                  <Link className="item-card-link" href={href}>
                    {row.facilityNameAr}
                  </Link>
                }
                subtitle={KIND[row.kind] ?? row.kind}
                state={{
                  label: STATUS[row.status]?.label ?? row.status,
                  tone: stateTone(STATUS[row.status]?.tone),
                }}
                facts={[
                  { label: "الوثائق", value: evidence?.label ?? "" },
                  {
                    label: "أُرسل",
                    value: row.submittedAt ? relativeTime(row.submittedAt) : "لم يُرسل",
                  },
                  {
                    label: "القرار",
                    value: row.reviewedAt ? relativeTime(row.reviewedAt) : "لم يُتّخذ",
                  },
                ]}
                testId={`application-${row.id}`}
                actions={
                  <Link className="profile-act-main" href={href} data-testid={`open-${row.id}`}>
                    <Icon name="eye" width={16} height={16} />
                    فتح الطلب
                  </Link>
                }
              >
                <p className="item-card-quiet">
                  {row.submittedAt ? `أُرسل ${formatDateTime(row.submittedAt)}` : "مسودة لم تُرسل بعد"}
                  {row.evidenceComplete ? "" : " · تنقصه وثائق مطلوبة"}
                </p>
              </ItemCard>
            );
          })}
        </ul>
      ) : null}
      {queue.pagination ? <Pagination {...queue.pagination} /> : null}
    </div>
  );
}
