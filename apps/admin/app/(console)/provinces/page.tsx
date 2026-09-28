"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  Card,
  type Column,
  ConfirmDialog,
  DataTable,
  ErrorState,
  LoadingState,
  PageHeader,
  StatusBadge,
  Toast,
} from "../../../components/ui";
import { useMutation } from "../../../lib/client/use-mutation";
import { useResource } from "../../../lib/client/use-resource";

type Province = Readonly<{
  id: string;
  code: string;
  nameAr: string;
  nameEn: string;
  active: boolean;
  sortOrder: number;
}>;

/**
 * Province rollout.
 *
 * All fourteen are present from the launch seed; activation is the lever. Turning one on
 * changes what the public province list serves immediately, which is why it is confirmed
 * rather than toggled inline — and why re-running the seed will not undo it.
 */
export default function ProvincesPage() {
  const provinces = useResource<{ items: Province[] }>("provinces");
  const mutation = useMutation();
  const canManage = useCan("admin.provinces.manage");

  const [pending, setPending] = useState<Province | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  async function submit(): Promise<void> {
    if (!pending) return;
    const ok = await mutation.run("provinceUpdate", {
      id: pending.id,
      active: !pending.active,
    });
    if (!ok) return;
    setToast(pending.active ? "تم تعطيل المحافظة." : "تم تفعيل المحافظة.");
    setPending(null);
    provinces.reload();
  }

  const columns: readonly Column<Province>[] = [
    { key: "nameAr", header: "المحافظة", render: (row) => row.nameAr },
    { key: "code", header: "الرمز", ltr: true, render: (row) => <code>{row.code}</code> },
    { key: "sortOrder", header: "الترتيب", ltr: true, render: (row) => row.sortOrder },
    {
      key: "active",
      header: "الحالة",
      render: (row) => (
        <StatusBadge tone={row.active ? "positive" : "neutral"}>
          {row.active ? "مفعّلة" : "غير مفعّلة"}
        </StatusBadge>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "1%",
      render: (row) =>
        canManage ? (
          <button
            type="button"
            className="button-ghost"
            data-testid={`toggle-province-${row.code}`}
            onClick={() => {
              mutation.reset();
              setPending(row);
            }}
          >
            {row.active ? "تعطيل" : "تفعيل"}
          </button>
        ) : null,
    },
  ];

  return (
    <div className="operation-stack">
      <PageHeader
        eyebrow="الدليل"
        title="المحافظات"
        description="التحكم في التوسّع الجغرافي للمنصة."
      />
      <Card flush>
      {provinces.loading ? <LoadingState /> : null}
      {provinces.error ? (
        <ErrorState error={provinces.error} onRetry={provinces.reload} />
      ) : null}
      {provinces.data ? (
        <DataTable
          caption="المحافظات"
          columns={columns}
          rows={provinces.data.items}
          rowKey={(row) => row.id}
        />
      ) : null}
      </Card>

      <ConfirmDialog
        open={pending !== null}
        title={pending?.active ? "تعطيل المحافظة" : "تفعيل المحافظة"}
        body={
          pending?.active
            ? "ستختفي المحافظة وكل ما فيها من الواجهات العامة."
            : "ستظهر المحافظة للعامة فوراً، وستُتاح تصنيفاتها المفعّلة فيها."
        }
        confirmLabel="تأكيد"
        destructive={pending?.active}
        pending={mutation.pending}
        error={mutation.error}
        onConfirm={submit}
        onCancel={() => setPending(null)}
      />

      <Toast message={toast} onDismiss={() => setToast(null)} />
    </div>
  );
}
