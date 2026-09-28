"use client";

import { use, useState } from "react";

import { useCan } from "../../../../components/admin-shell";
import {
  ConfirmDialog,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
  formatDateTime,
} from "../../../../components/ui";
import { useMutation } from "../../../../lib/client/use-mutation";
import { useResource } from "../../../../lib/client/use-resource";
import { STATUS } from "../page";

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
}>;

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
        <section className="panel stack">
          <h2>الحالة</h2>
          <div className="button-row">
            <StatusBadge tone={STATUS[facility.data.status]?.tone ?? "neutral"}>
              {STATUS[facility.data.status]?.label ?? facility.data.status}
            </StatusBadge>
            <span className="muted">
              آخر تحديث:{" "}
              <span className="cell-ltr">{formatDateTime(facility.data.updatedAt)}</span>
            </span>
          </div>
          <dl className="diff">
            <div className="diff-row">
              <dt>الاسم بالإنجليزية</dt>
              <dd>{facility.data.nameEn || "—"}</dd>
            </div>
            <div className="diff-row">
              <dt>المحافظة</dt>
              <dd className="cell-ltr">{facility.data.provinceId}</dd>
            </div>
            <div className="diff-row">
              <dt>التصنيف</dt>
              <dd className="cell-ltr">{facility.data.categoryId}</dd>
            </div>
          </dl>
        </section>
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
