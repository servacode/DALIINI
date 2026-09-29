"use client";

import Link from "next/link";
import { use, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  ConfirmDialog,
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  type Tone,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { REASONS } from "../../reports/page";
import { useLookups } from "../../../../lib/client/use-lookups";
import { useResource } from "../../../../lib/client/use-resource";
import { QUALITY_ISSUES, QualityMeter, STATUS } from "../page";

type Facility = Readonly<{
  id: string;
  nameAr: string;
  nameEn: string | null;
  status: string;
  provinceId: string;
  categoryId: string;
  cityId: string | null;
  updatedAt: string | null;
  location: { latitude: number; longitude: number } | null;
  categoryNameAr?: string;
  provinceNameAr?: string;
  ownerName?: string | null;
  ownerPhone?: string | null;
  qualityScore?: number;
  qualityIssues?: readonly string[];
}>;

type TimelineEvent = Readonly<{
  at: string;
  kind: string;
  titleAr: string;
  actorName: string | null;
  requestId: string | null;
}>;

const TIMELINE_TONE: Record<string, Tone> = {
  APPLICATION_SUBMITTED: "info",
  APPLICATION_APPROVED: "positive",
  APPLICATION_REJECTED: "danger",
  REPORT_CREATED: "warning",
  REPORT_RESOLVED: "positive",
  REPORT_DISMISSED: "neutral",
  DUTY_SUMMARY: "brand",
  AUDIT: "neutral",
};

type Action = Readonly<{
  operation: string;
  title: string;
  body: string;
  label: string;
  destructive?: boolean;
  reasonRequired?: boolean;
}>;

const ACTIONS: Record<string, Action> = {
  suspend: {
    operation: "facilitySuspend",
    title: "إيقاف المنشأة",
    body: "ستختفي المنشأة من النتائج العامة فوراً. يمكن إعادة تفعيلها لاحقاً.",
    label: "تأكيد الإيقاف",
    destructive: true,
    reasonRequired: true,
  },
  reactivate: {
    operation: "facilityReactivate",
    title: "إعادة تفعيل المنشأة",
    body: "ستعود المنشأة للظهور في النتائج العامة.",
    label: "تأكيد التفعيل",
  },
  close: {
    operation: "facilityClose",
    title: "إغلاق المنشأة نهائياً",
    body: "إجراء تشغيلي كبير. راجع السبب قبل التأكيد.",
    label: "تأكيد الإغلاق",
    destructive: true,
    reasonRequired: true,
  },
};

export default function FacilityDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const lookups = useLookups();
  const canReadReports = useCan("admin.reports.read");
  const facility = useResource<Facility>("facility", { id });
  const mutation = useMutation();
  const canManage = useCan("admin.facilities.manage");

  const [action, setAction] = useState<keyof typeof ACTIONS | null>(null);
  const [reason, setReason] = useState("");
  const [toast, setToast] = useState<string | null>(null);

  const chosen = action ? ACTIONS[action] : null;

  async function submit(): Promise<void> {
    if (!chosen) return;
    const ok = await mutation.run(chosen.operation, { id, reason: reason.trim() });
    if (!ok) return;
    setAction(null);
    setReason("");
    setToast("تم تنفيذ الإجراء وتسجيله في سجل التدقيق.");
    facility.reload();
  }

  const status = facility.data?.status;

  return (
    <div className="stack">
      <PageHeader
        back={{ href: "/facilities", label: "المنشآت" }}
        title={facility.data?.nameAr ?? "منشأة"}
        description="الحالة التشغيلية والإجراءات المتاحة."
        actions={
          canManage && status ? (
            <div className="button-row">
              {status === "ACTIVE" ? (
                <button
                  type="button"
                  className="button-danger"
                  data-testid="suspend"
                  onClick={() => {
                    mutation.reset();
                    setAction("suspend");
                  }}
                >
                  إيقاف
                </button>
              ) : null}
              {status === "SUSPENDED" ? (
                <button
                  type="button"
                  className="button-primary"
                  data-testid="reactivate"
                  onClick={() => {
                    mutation.reset();
                    setAction("reactivate");
                  }}
                >
                  إعادة تفعيل
                </button>
              ) : null}
              {status !== "CLOSED" ? (
                <button
                  type="button"
                  className="button-danger"
                  data-testid="close"
                  onClick={() => {
                    mutation.reset();
                    setAction("close");
                  }}
                >
                  إغلاق
                </button>
              ) : null}
            </div>
          ) : null
        }
      />

      {facility.loading ? <LoadingState /> : null}
      {facility.error ? <ErrorState error={facility.error} onRetry={facility.reload} /> : null}

      {facility.data ? (
        <Panel title="بيانات المنشأة">
          <KeyValueList
            items={[
              {
                label: "الحالة",
                value: (
                  <StatusBadge tone={STATUS[facility.data.status]?.tone ?? "neutral"}>
                    {STATUS[facility.data.status]?.label ?? facility.data.status}
                  </StatusBadge>
                ),
              },
              { label: "الاسم بالإنجليزية", value: facility.data.nameEn || "—" },
              {
                label: "المحافظة",
                value: facility.data.provinceNameAr ?? lookups.provinceName(facility.data.provinceId),
              },
              {
                label: "التصنيف",
                value: facility.data.categoryNameAr ?? lookups.categoryName(facility.data.categoryId),
              },
              ...(facility.data.qualityScore !== undefined
                ? [
                    {
                      label: "مؤشر الجودة",
                      value: (
                        <span className="quality-cell">
                          <QualityMeter score={facility.data.qualityScore} />
                          {facility.data.qualityIssues?.length ? (
                            <span className="muted quality-issues">
                              {facility.data.qualityIssues
                                .map((code) => QUALITY_ISSUES[code] ?? code)
                                .join("، ")}
                            </span>
                          ) : null}
                        </span>
                      ),
                    },
                  ]
                : []),
              { label: "المالك", value: facility.data.ownerName ?? "—" },
              { label: "هاتف المالك", value: facility.data.ownerPhone ?? "—", ltr: true },
              {
                label: "الموقع",
                value: facility.data.location ? (
                  <a
                    href={`https://www.openstreetmap.org/?mlat=${facility.data.location.latitude}&mlon=${facility.data.location.longitude}#map=18/${facility.data.location.latitude}/${facility.data.location.longitude}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    فتح على الخريطة
                  </a>
                ) : (
                  "—"
                ),
              },
              { label: "آخر تحديث", value: formatDateTime(facility.data.updatedAt), ltr: true },
            ]}
          />
        </Panel>
      ) : null}

      {facility.data ? (
        <Panel title="السجل الزمني" description="كل ما حدث لهذه المنشأة من التسجيل حتى اليوم.">
          <FacilityTimeline facilityId={facility.data.id} />
        </Panel>
      ) : null}

      {facility.data && canReadReports ? (
        <Panel
          title="البلاغات"
          description="ما أبلغ عنه المستخدمون عن هذه المنشأة."
          actions={
            <Link className="button-ghost" href="/reports">
              كل البلاغات
            </Link>
          }
        >
          <FacilityReports facilityId={facility.data.id} />
        </Panel>
      ) : null}

      <ConfirmDialog
        open={chosen !== null}
        title={chosen?.title ?? ""}
        body={chosen?.body}
        confirmLabel={chosen?.label ?? "تأكيد"}
        destructive={chosen?.destructive}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setAction(null)}
      >
        {chosen?.reasonRequired ? (
          <label className="field">
            <span>السبب</span>
            <textarea
              rows={3}
              value={reason}
              data-testid="action-reason"
              onChange={(event) => setReason(event.target.value)}
            />
          </label>
        ) : null}
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}

/** Open and past problem reports for one facility, read-only here; decisions happen on /reports. */
function FacilityReports({ facilityId }: { facilityId: string }) {
  const reports = useResource<{
    items: { id: string; reason: string; note: string; status: string; createdAt: string }[];
  }>("reports", { facility: facilityId });
  if (reports.loading) return <LoadingState />;
  if (reports.error) return <ErrorState error={reports.error} onRetry={reports.reload} />;
  const items = reports.data?.items ?? [];
  if (items.length === 0) return <span className="muted">لا بلاغات.</span>;
  return (
    <ol className="timeline">
      {items.map((report) => (
        <li key={report.id}>
          <time dateTime={report.createdAt} className="cell-ltr">
            {formatDateTime(report.createdAt)}
          </time>
          <strong>{REASONS[report.reason] ?? report.reason}</strong>
          {report.note ? <span className="muted">{report.note}</span> : null}
          <StatusBadge tone={report.status === "OPEN" ? "warning" : "neutral"}>
            {report.status === "OPEN" ? "مفتوح" : report.status === "RESOLVED" ? "مُعالَج" : "مرفوض"}
          </StatusBadge>
        </li>
      ))}
    </ol>
  );
}

/** Everything that happened to one facility, newest first: applications, decisions, reports, audit. */
function FacilityTimeline({ facilityId }: { facilityId: string }) {
  const timeline = useResource<{ items: TimelineEvent[] }>("facilityTimeline", { id: facilityId });
  if (timeline.loading) return <LoadingState />;
  if (timeline.error) return <ErrorState error={timeline.error} onRetry={timeline.reload} />;
  const items = timeline.data?.items ?? [];
  if (items.length === 0) return <span className="muted">لا أحداث مسجّلة بعد.</span>;
  return (
    <ol className="timeline" data-testid="facility-timeline">
      {items.map((event, index) => (
        <li key={`${event.at}-${index}`}>
          <time dateTime={event.at} className="cell-ltr">
            {formatDateTime(event.at)}
          </time>
          <StatusBadge tone={TIMELINE_TONE[event.kind] ?? "neutral"}>{event.titleAr}</StatusBadge>
          {event.actorName ? <span className="muted">بواسطة {event.actorName}</span> : null}
        </li>
      ))}
    </ol>
  );
}
