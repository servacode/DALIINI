import { redirect } from "next/navigation";

/** Adding a facility happens in a window over the list now (DECISION-109). */
export default function NewFacilityRedirect() {
  redirect("/facilities?add=1");
}
