"use client";

import { useRouter } from "next/navigation";
import { use } from "react";

import { useCan } from "../../../../../components/admin-shell";
import { type EditableFacility, FacilityForm } from "../../../../../components/facility-form";
import { EmptyState, ErrorState, LoadingState, PageHeader } from "../../../../../components/ui";
import { useResource } from "../../../../../lib/client/use-resource";

/**
 * Correct a facility's details. The status is not touched: a correction by the directory is
 * itself a verification. The owners are told their listing changed.
 */
export default function EditFacilityPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const canEdit = useCan("admin.facilities.edit");
  const facility = useResource<EditableFacility>("facility", { id });

  return (
    <div className="stack">
      <PageHeader
        back={{ href: `/facilities/${id}`, label: facility.data?.nameAr ?? "المنشأة" }}
        title="تعديل بيانات المنشأة"
        description="يُرسَل ما تغيّر فقط، ويُسجَّل قبل التعديل وبعده في سجل التدقيق."
      />
      {!canEdit ? (
        <EmptyState title="لا تملك صلاحية تعديل المنشآت" hint="اطلبها من مدير المنصة." />
      ) : null}
      {canEdit && facility.loading ? <LoadingState /> : null}
      {canEdit && facility.error ? (
        <ErrorState error={facility.error} onRetry={facility.reload} />
      ) : null}
      {canEdit && facility.data ? (
        <FacilityForm
          key={facility.data.id}
          facility={facility.data}
          onSaved={() => router.push(`/facilities/${id}`)}
        />
      ) : null}
    </div>
  );
}
