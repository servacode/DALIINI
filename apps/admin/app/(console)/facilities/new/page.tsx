"use client";

import { useRouter } from "next/navigation";

import { useCan } from "../../../../components/admin-shell";
import { FacilityForm } from "../../../../components/facility-form";
import { EmptyState, PageHeader } from "../../../../components/ui";

/** A facility the directory lists itself, before or without its owner registering. */
export default function NewFacilityPage() {
  const router = useRouter();
  const canEdit = useCan("admin.facilities.edit");

  return (
    <div className="stack">
      <PageHeader
        back={{ href: "/facilities", label: "المنشآت" }}
        title="إضافة منشأة"
        description="تُضاف بلا مالك، ويمكن لصاحبها المطالبة بها لاحقاً. تُسجَّل الإضافة في سجل التدقيق."
      />
      {canEdit ? (
        <FacilityForm facility={null} onSaved={(id) => router.push(`/facilities/${id}`)} />
      ) : (
        <EmptyState title="لا تملك صلاحية إضافة المنشآت" hint="اطلبها من مدير المنصة." />
      )}
    </div>
  );
}
