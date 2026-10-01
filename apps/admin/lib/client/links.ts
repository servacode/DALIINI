/**
 * Where an alert or search hit leads inside the console.
 *
 * The backend describes a destination as an entity type, an optional id and an optional
 * query string for the matching list screen; the console alone knows its own URLs. Keeping
 * the mapping here means a moved screen is one edit, not a hunt through every widget.
 */
export type EntityLink = Readonly<{
  entityType: string;
  entityId?: string | null;
  query?: string | null;
}>;

const LISTS: Record<string, string> = {
  FACILITY_LIST: "/facilities",
  APPLICATION_LIST: "/reviews",
  REPORT_LIST: "/reports",
  DUTY_ROSTER: "/duty",
  SETTINGS: "/settings",
  PROVINCE: "/provinces",
};

const DETAILS: Record<string, string> = {
  FACILITY: "/facilities",
  APPLICATION: "/reviews",
  USER: "/users",
};

export function hrefFor(link: EntityLink | null | undefined): string | null {
  if (!link) return null;
  const detail = DETAILS[link.entityType];
  if (detail && link.entityId) return `${detail}/${link.entityId}`;
  const list = LISTS[link.entityType];
  if (!list) return null;
  const query = link.query?.replace(/^\?/, "");
  return query ? `${list}?${query}` : list;
}
