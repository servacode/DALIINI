import { FacilitiesView } from "./facilities-view";

/**
 * Read on the server at request time, so the deployment sets the map without a rebuild and a
 * console with no map configured sends no map host to the browser at all (DECISION-075). The
 * add and edit windows place a facility on that map (DECISION-109).
 */
export default function FacilitiesPage() {
  return <FacilitiesView mapStyleUrl={process.env.ADMIN_MAP_STYLE_URL?.trim() ?? ""} />;
}
