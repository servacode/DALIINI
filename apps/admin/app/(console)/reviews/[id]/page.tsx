"use client";

import { use, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  AuditTimeline,
  ConfirmDialog,
  DiffViewer,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
  type Tone,
  formatDateTime,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { fieldErrorsFor } from "../../../../lib/errors/messages";

type Detail = Readonly<{
  id: string;
  facilityNameAr: string;
  kind: string;
  status: string;
  submittedAt: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  facility: Record<string, unknown>;
  snapshot: Record<string, unknown>;
  publicImageIds: readonly string[];
  evidence: readonly { id: string; requirementId: string; labelAr: string }[];
  audit: readonly { action: string; requestId: string; createdAt: string }[];
}>;

const STATUS: Record<string, { label: string; tone: Tone }> = {
  SUBMITTED: { label: "قيد المراجعة", tone: "info" },
  APPROVED: { label: "مقبول", tone: "positive" },
  REJECTED: { label: "مرفوض", tone: "danger" },
};

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

  async function submit(): Promise<void> {
    const approving = dialog === "approve";
    const ok = await decision.run(approving ? "reviewApprove" : "reviewReject", {
      id,
      reason: reason.trim(),
    });
    if (!ok) return;
    setDialog(null);
    setReason("");
    setToast(approving ? "تم قبول الطلب." : "تم رفض الطلب.");
    detail.reload();
  }

  const reasonError = fieldErrorsFor(decision.error).reason;

  return (
    <div className="stack">
      <PageHeader
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
          <section className="panel stack">
            <h2>حالة الطلب</h2>
            <div className="button-row">
              <StatusBadge tone={STATUS[detail.data.status]?.tone ?? "neutral"}>
                {STATUS[detail.data.status]?.label ?? detail.data.status}
              </StatusBadge>
              <span className="muted">
                أُرسل: <span className="cell-ltr">{formatDateTime(detail.data.submittedAt)}</span>
              </span>
              {detail.data.reviewedAt ? (
                <span className="muted">
                  روجع: <span className="cell-ltr">{formatDateTime(detail.data.reviewedAt)}</span>
                </span>
              ) : null}
            </div>
            {detail.data.rejectionReason ? (
              <p className="notice">سبب الرفض السابق: {detail.data.rejectionReason}</p>
            ) : null}
          </section>

          <section className="panel stack">
            <h2>لقطة الطلب</h2>
            <DiffViewer before={{}} after={detail.data.snapshot} />
          </section>

          <section className="panel stack">
            <h2>أدلة التحقق</h2>
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
          </section>

          <section className="panel stack">
            <h2>سجل التدقيق</h2>
            <AuditTimeline entries={detail.data.audit} />
          </section>
        </>
      ) : null}

      <ConfirmDialog
        open={dialog === "approve"}
        title="قبول الطلب"
        body="ستصبح المنشأة فعّالة وتظهر للعامة. لا يمكن التراجع عن هذا الإجراء من هنا."
        confirmLabel="تأكيد القبول"
        pending={decision.pending}
        error={decision.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      />

      <ConfirmDialog
        open={dialog === "reject"}
        title="رفض الطلب"
        body="يعود الطلب إلى المالك مع السبب الذي تكتبه."
        confirmLabel="تأكيد الرفض"
        destructive
        pending={decision.pending}
        error={decision.error}
        onConfirm={submit}
        onCancel={() => setDialog(null)}
      >
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
