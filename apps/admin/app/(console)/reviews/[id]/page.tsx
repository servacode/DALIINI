"use client";

import Link from "next/link";
import { use, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import { RejectionTemplatePicker } from "../../../../components/rejection-template-picker";
import {
  AuditTimeline,
  ConfirmDialog,
  DiffViewer,
  ErrorState,
  KeyValueList,
  LoadingState,
  PageHeader,
  Panel,
  StatusBadge,
  TermBadge,
  Toast,
  formatDateTime,
  termsFor,
  labelsFor,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";

type Detail = Readonly<{
  id: string;
  facilityId: string;
  facilityNameAr: string;
  kind: string;
  status: string;
  submittedAt: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  evidenceComplete: boolean;
  categoryNameAr: string;
  provinceNameAr: string;
  ownerName: string | null;
  ownerPhone: string | null;
  snapshot: Record<string, unknown>;
  previous: Record<string, unknown> | null;
  /** CHANGE only: what the owner proposes, and the version of it this page shows. */
  proposedFields?: readonly string[];
  revision?: number;
  location: { latitude: number; longitude: number } | null;
  duplicates: readonly { id: string; nameAr: string; status: string; reasons: readonly string[] }[];
  publicImages: readonly { id: string; url: string }[];
  evidence: readonly { id: string; requirementId: number; labelAr: string }[];
  audit: readonly { action: string; requestId: string; createdAt: string }[];
}>;

const DUPLICATE_REASONS: Record<string, string> = {
  SAME_PHONE: "نفس رقم الهاتف",
  SAME_NAME_NEARBY: "اسم مطابق على مسافة قريبة",
};

const KIND = labelsFor("applicationKind");

const STATUS = termsFor("applicationStatus");

/**
 * One application, with everything a reviewer needs to decide.
 *
 * Evidence is listed by its safe descriptor only — an id and a requirement label. The
 * object key never leaves the backend and the content is fetched through the audited
 * endpoint, so opening a document is itself a recorded event.
 */
export default function ReviewDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const detail = useResource<Detail>("review", { id });
  const decision = useMutation();
  const canDecide = useCan("admin.reviews.decide");
  const canReadEvidence = useCan("admin.evidence.read");

  const [dialog, setDialog] = useState<"approve" | "reject" | null>(null);
  const [reason, setReason] = useState("");
  const [toast, setToast] = useState<string | null>(null);

  const isChange = detail.data?.kind === "CHANGE";
  const waiting = detail.data?.status === "SUBMITTED";
  // Where a waiting change would move the facility, beside where the public sees it now.
  const proposedPoint =
    isChange && waiting && detail.data?.proposedFields?.includes("location")
      ? (detail.data.snapshot.location as { latitude: number; longitude: number } | null)
      : null;

  async function submit(): Promise<void> {
    const approving = dialog === "approve";
    const ok = await decision.run(approving ? "reviewApprove" : "reviewReject", {
      id,
      reason: reason.trim(),
      // The version on screen. If the owner revised it since, the backend refuses, and the
      // page reloads so the reviewer reads what would actually be published.
      ...(approving && isChange ? { revision: detail.data?.revision } : {}),
    });
    if (!ok) {
      if (isChange) detail.reload();
      return;
    }
    setDialog(null);
    setReason("");
    setToast(approving ? "تم قبول الطلب." : "تم رفض الطلب.");
    detail.reload();
  }

  const reasonError = fieldErrorsFor(decision.error).reason;

  return (
    <div className="stack">
      <PageHeader
        back={{ href: "/reviews", label: "المراجعات" }}
        title="مراجعة طلب"
        description={detail.data?.facilityNameAr}
        actions={
          canDecide && detail.data?.status === "SUBMITTED" ? (
            <>
              <button
                type="button"
                className="button-primary"
                data-testid="approve"
                onClick={() => {
                  decision.reset();
                  setDialog("approve");
                }}
              >
                قبول
              </button>
              <button
                type="button"
                className="button-danger"
                data-testid="reject"
                onClick={() => {
                  decision.reset();
                  setDialog("reject");
                }}
              >
                رفض
              </button>
            </>
          ) : null
        }
      />

      {detail.loading ? <LoadingState /> : null}
      {detail.error ? <ErrorState error={detail.error} onRetry={detail.reload} /> : null}

      {detail.data ? (
        <>
          {detail.data.duplicates.length > 0 ? (
            <div className="state-block state-warning" role="status" data-testid="duplicates">
              <strong>منشآت مشابهة قد تكون مكررة</strong>
              <ul>
                {detail.data.duplicates.map((dup) => (
                  <li key={dup.id}>
                    <Link href={`/facilities/${dup.id}`}>{dup.nameAr}</Link>
                    {" — "}
                    {dup.reasons.map((reason) => DUPLICATE_REASONS[reason] ?? reason).join("، ")}
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          <div className="grid-main-aside">
            <Panel title="ملخص الطلب">
              <KeyValueList
                items={[
                  {
                    label: "الحالة",
                    value: (
                      <StatusBadge tone={STATUS[detail.data.status]?.tone ?? "neutral"}>
                        {STATUS[detail.data.status]?.label ?? detail.data.status}
                      </StatusBadge>
                    ),
                  },
                  { label: "النوع", value: KIND[detail.data.kind] ?? detail.data.kind },
                  // A change to a live facility is decided on what changed; the facility's
                  // documents were checked when it was approved and are not asked for again.
                  ...(isChange
                    ? []
                    : [
                        {
                          label: "الوثائق المطلوبة",
                          value: (
                            <TermBadge
                              group="evidenceState"
                              value={detail.data.evidenceComplete ? "COMPLETE" : "INCOMPLETE"}
                            />
                          ),
                        },
                      ]),
                  { label: "التصنيف", value: detail.data.categoryNameAr },
                  { label: "المحافظة", value: detail.data.provinceNameAr },
                  { label: "مقدّم الطلب", value: detail.data.ownerName ?? "—" },
                  { label: "هاتف المالك", value: detail.data.ownerPhone ?? "—", ltr: true },
                  { label: "أُرسل", value: formatDateTime(detail.data.submittedAt), ltr: true },
                  ...(detail.data.reviewedAt
                    ? [{ label: "روجع", value: formatDateTime(detail.data.reviewedAt), ltr: true }]
                    : []),
                ]}
              />
              {detail.data.rejectionReason ? (
                <p className="notice">سبب الرفض السابق: {detail.data.rejectionReason}</p>
              ) : null}
            </Panel>

            <Panel title="الموقع">
              {detail.data.location ? (
                <>
                  <KeyValueList
                    items={[
                      {
                        label: isChange && proposedPoint ? "المنشور الآن" : "الإحداثيات",
                        value: `${detail.data.location.latitude.toFixed(5)}, ${detail.data.location.longitude.toFixed(5)}`,
                        ltr: true,
                      },
                      ...(proposedPoint
                        ? [
                            {
                              label: "المقترح",
                              value: (
                                <a
                                  href={`https://www.openstreetmap.org/?mlat=${proposedPoint.latitude}&mlon=${proposedPoint.longitude}#map=18/${proposedPoint.latitude}/${proposedPoint.longitude}`}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  data-testid="open-proposed-map"
                                >
                                  {`${proposedPoint.latitude.toFixed(5)}, ${proposedPoint.longitude.toFixed(5)}`}
                                </a>
                              ),
                              ltr: true,
                            },
                          ]
                        : []),
                    ]}
                  />
                  <a
                    className="button-ghost"
                    href={`https://www.openstreetmap.org/?mlat=${detail.data.location.latitude}&mlon=${detail.data.location.longitude}#map=18/${detail.data.location.latitude}/${detail.data.location.longitude}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    data-testid="open-map"
                  >
                    فتح على الخريطة
                  </a>
                </>
              ) : (
                <span className="muted">لم يُحدَّد موقع.</span>
              )}
            </Panel>
          </div>

          {isChange && waiting ? (
            <p className="notice" data-testid="change-callout">
              المنشأة ظاهرة للعامة الآن ببياناتها المعتمدة. القبول ينشر الحقول المعدّلة فقط، والرفض
              يبقيها كما هي.
            </p>
          ) : null}

          {detail.data.previous ? (
            <div className="grid-2">
              <Panel
                title="التغييرات المطلوبة"
                description={
                  isChange
                    ? "الفرق بين ما يظهر للعامة الآن وما يطلبه المالك."
                    : "الفرق بين آخر نسخة معتمدة وما أرسله المالك."
                }
              >
                <DiffViewer before={detail.data.previous} after={detail.data.snapshot} />
              </Panel>
              <Panel title="لقطة الطلب" description="البيانات كما أُرسلت للمراجعة.">
                <DiffViewer before={{}} after={detail.data.snapshot} />
              </Panel>
            </div>
          ) : (
            <Panel
              title="لقطة الطلب"
              description={
                !isChange
                  ? "تسجيل أول: لا نسخة معتمدة سابقة للمقارنة."
                  : detail.data.status === "APPROVED"
                    ? "البيانات كما نُشرت عند اعتماد التعديل."
                    : "التعديل كما طلبه المالك."
              }
            >
              <DiffViewer before={{}} after={detail.data.snapshot} />
            </Panel>
          )}

          <Panel title="الصور العامة" description="الصور التي ستظهر للعامة بعد القبول.">
            {detail.data.publicImages.length === 0 ? (
              <span className="muted">لا صور مرفوعة.</span>
            ) : (
              <div className="image-grid" data-testid="public-images">
                {detail.data.publicImages.map((image) => (
                  <a key={image.id} href={image.url} target="_blank" rel="noopener noreferrer">
                    {/* eslint-disable-next-line @next/next/no-img-element -- CDN URL, CSP-allowed origin, no optimisation route */}
                    <img src={image.url} alt="صورة المنشأة" loading="lazy" />
                  </a>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="أدلة التحقق">
            {detail.data.evidence.length === 0 ? (
              <span className="muted">لا أدلة مرفوعة.</span>
            ) : (
              <ul className="timeline" data-testid="evidence-list">
                {detail.data.evidence.map((item) => (
                  <li key={item.id}>
                    <strong>{item.labelAr}</strong>
                    {canReadEvidence ? (
                      <a
                        className="button-ghost"
                        href={`/api/admin/evidence/${item.id}`}
                        data-testid={`evidence-${item.id}`}
                      >
                        عرض المستند
                      </a>
                    ) : (
                      <span className="muted" data-testid={`evidence-denied-${item.id}`}>
                        لا تملك صلاحية عرض المستندات
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
            <p className="muted">
              يُفتح المستند عبر مسار مُدقَّق ولمدة محدودة. لا يُعرض مفتاح التخزين ولا رابط دائم.
            </p>
          </Panel>

          <Panel title="سجل التدقيق">
            <AuditTimeline entries={detail.data.audit} />
          </Panel>
        </>
      ) : null}

      <ConfirmDialog
        open={dialog === "approve"}
        title={isChange ? "اعتماد التعديل" : "قبول الطلب"}
        body={
          isChange
            ? "ستظهر الحقول المعدّلة للعامة فوراً بدل الحالية."
            : "ستصبح المنشأة فعّالة وتظهر للعامة. لا يمكن التراجع عن هذا الإجراء من هنا."
        }
        confirmLabel="تأكيد القبول"
        pending={decision.pending}
        error={decision.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />

      <ConfirmDialog
        open={dialog === "reject"}
        title={isChange ? "رفض التعديل" : "رفض الطلب"}
        body={
          isChange
            ? "تبقى المنشأة ظاهرة كما هي، ويصل السبب الذي تكتبه إلى المالك."
            : "يعود الطلب إلى المالك مع السبب الذي تكتبه."
        }
        confirmLabel="تأكيد الرفض"
        destructive
        pending={decision.pending}
        error={decision.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      >
        <RejectionTemplatePicker onPick={(text) => setReason(text)} />
        <label className="field">
          <span>سبب الرفض</span>
          <textarea
            rows={3}
            value={reason}
            data-testid="reject-reason"
            aria-invalid={Boolean(reasonError)}
            onChange={(event) => setReason(event.target.value)}
          />
          {reasonError ? <span className="field-error">{reasonError}</span> : null}
        </label>
      </ConfirmDialog>

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
