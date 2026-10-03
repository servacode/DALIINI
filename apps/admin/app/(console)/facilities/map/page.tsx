import { FacilitiesMap } from "../../../../components/facilities-map";

/**
 * Read on the server at request time, so the deployment sets the map without a rebuild and a
 * console with no map configured sends no map host to the browser at all (DECISION-075).
 */
export default function FacilitiesMapPage() {
  return <FacilitiesMap styleUrl={process.env.ADMIN_MAP_STYLE_URL?.trim() ?? ""} />;
}
