"use client";

import { useState } from "react";

import { useCan } from "../../../components/admin-shell";
import {
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  LoadingState,
  PageHeader,
  Panel,
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
  const [citiesOf, setCitiesOf] = useState<Province | null>(null);
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
      render: (row) => (
        <div className="button-row">
          <button
            type="button"
            className="button-ghost"
            data-testid={`cities-${row.code}`}
            onClick={() => setCitiesOf(citiesOf?.id === row.id ? null : row)}
          >
            {citiesOf?.id === row.id ? "إخفاء المدن" : "المدن"}
          </button>
          {canManage ? (
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
          ) : null}
        </div>
      ),
    },
  ];

  return (
    <div className="stack">
      <PageHeader title="المحافظات" description="التحكم في التوسّع الجغرافي للمنصة." />
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

      {citiesOf ? (
        <CitiesPanel key={citiesOf.id} province={citiesOf} canManage={canManage} />
      ) : null}

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

type City = Readonly<{ id: string; code: string; nameAr: string; nameEn: string; active: boolean }>;

/**
 * The cities inside one province, with their own activation switch.
 *
 * A city switch narrows what the public sees inside an active province; it cannot open a
 * city in a province that is itself off. Each toggle is audited by the backend.
 */
function CitiesPanel({ province, canManage }: { province: Province; canManage: boolean }) {
  const cities = useResource<{ items: City[] }>("provinceCities", { id: province.id });
  const mutation = useMutation();

  async function toggle(city: City): Promise<void> {
    const ok = await mutation.run("cityUpdate", {
      provinceId: province.id,
      id: city.id,
      active: !city.active,
    });
    if (ok) cities.reload();
  }

  return (
    <Panel title={`مدن ${province.nameAr}`} flush testId="cities-panel">
      {cities.loading ? <LoadingState /> : null}
      {cities.error ? <ErrorState error={cities.error} onRetry={cities.reload} /> : null}
      {mutation.error ? <ErrorState error={mutation.error} /> : null}
      {cities.data ? (
        <DataTable
          caption={`مدن ${province.nameAr}`}
          rows={cities.data.items}
          rowKey={(row) => row.id}
          empty={<EmptyState title="لا مدن مسجّلة لهذه المحافظة" />}
          columns={[
            { key: "nameAr", header: "المدينة", render: (row) => row.nameAr },
            { key: "code", header: "الرمز", ltr: true, render: (row) => <code>{row.code}</code> },
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
                    disabled={mutation.pending}
                    data-testid={`toggle-city-${row.code}`}
                    onClick={() => toggle(row)}
                  >
                    {row.active ? "تعطيل" : "تفعيل"}
                  </button>
                ) : null,
            },
          ]}
        />
      ) : null}
    </Panel>
  );
}
