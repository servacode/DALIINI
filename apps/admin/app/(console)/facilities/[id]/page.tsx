import { redirect } from "next/navigation";

/**
 * There is no facility detail page any more: a card holds its whole facility (DECISION-109).
 * Links to this address are already written down — in audit entries, in messages — so it
 * sends the reader to the list with that card outlined.
 */
export default async function FacilityRedirect({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  redirect(`/facilities?id=${encodeURIComponent(id)}`);
}
