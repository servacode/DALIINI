import type { AvailabilityState } from "./api";
import { facilityPath } from "./paths";

/* One pin on the map: where a facility is, what it is called, and the page it leads to. */
export type MapPoint = Readonly<{
  id: string;
  name: string;
  href: string;
  latitude: number;
  longitude: number;
  state: AvailabilityState;
}>;

type Placeable = {
  id: string;
  slug?: string | null;
  nameAr: string;
  location: { latitude: number; longitude: number } | null;
  availability: { state: AvailabilityState };
};

/* The pins for a list of facilities: only those with a place on the map, each to its own page. */
export function pointsOf(items: readonly Placeable[]): MapPoint[] {
  return items.flatMap((item) =>
    item.location
      ? [
          {
            id: item.id,
            name: item.nameAr,
            href: facilityPath(item),
            latitude: item.location.latitude,
            longitude: item.location.longitude,
            state: item.availability.state,
          },
        ]
      : [],
  );
}
