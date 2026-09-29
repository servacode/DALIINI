import "server-only";

/*
 * Server-side reader for the public (anonymous) Daliini API.
 *
 * The shapes below mirror the generated models in packages/api-typescript
 * (CompactFacility, PublicFacilityDetail, PublicProvince, PublicCategory,
 * PublicCategoryTags, FacilityCursorPage, PublicDutyRoster, ContentPage, FaqList,
 * EmergencyNumberList). We do not import the generated client itself: it ships
 * as an unbuilt package (dist/ is produced by its own `prepare` step) and the
 * public site only needs a handful of GET endpoints, so a small typed fetch
 * keeps the web build independent of that package's build. Keep these in sync
 * with the OpenAPI schema when public fields change.
 *
 * Only server code imports this module (`server-only` fails the build if a
 * client component does). PUBLIC_API_ORIGIN and WEB_SERVER_API_KEY have no
 * NEXT_PUBLIC_ prefix, so neither is ever inlined into a browser bundle.
 *
 * Every call degrades to `null` instead of throwing: `next build` runs with no
 * API reachable, and a flaky API must render an explanatory state rather than
 * a 500. Pages distinguish "API unavailable" (null) from "empty" ([]) and,
 * where it matters, "not found" (undefined, from an upstream 404).
 */

export type AvailabilityState = "OPEN" | "CLOSED" | "DUTY" | "TEMP_CLOSED";

export interface Ref { id: string; nameAr: string; nameEn?: string | null }

export interface CompactFacility {
  id: string;
  nameAr: string;
  nameEn: string | null;
  category: Ref;
  city: Ref | null;
  ratingAverage: number | null;
  ratingCount: number;
  availability: { state: AvailabilityState; nextOpenAt: string | null };
  /* When staff last approved the details (trust signal). */
  lastVerifiedAt?: string | null;
  /* The later of lastVerifiedAt and the owner's own "the hours are still right". */
  infoConfirmedAt?: string | null;
  /* The last change to the facility record. */
  updatedAt?: string | null;
}

export interface HoursEntry { id: string; weekday: number; opensAt: string; closesAt: string; sequence: number }

/* A specialty or a service (NamedIntRef): unlike every other reference, its id is an integer. */
export interface TagRef { id: number; nameAr: string }

export interface FacilityDetail extends CompactFacility {
  descriptionAr: string | null;
  phone: string | null;
  /* E.164 Syrian mobile (+9639XXXXXXXX). */
  whatsapp?: string | null;
  addressAr: string | null;
  neighborhood: Ref | null;
  location: { latitude: number; longitude: number } | null;
  hours: HoursEntry[];
  /* Active ones only, in the team's order. */
  specialties: TagRef[];
  services: TagRef[];
}

export interface Province { id: string; code: string; nameAr: string; nameEn: string | null }

export interface Category {
  id: string;
  nameAr: string;
  nameEn: string | null;
  iconKey: string | null;
  group: Ref;
  capabilities: {
    duty: boolean;
    hours: boolean;
    ratings: boolean;
    /* Whether the category's list can be narrowed by specialty, and by service. */
    specialtyFilter: boolean;
    serviceFilter: boolean;
  };
}

/* The choices behind a category's specialty and service filters (publicCategoryTagsRetrieve). */
export interface CategoryTags { specialties: TagRef[]; services: TagRef[] }

export interface FacilityPage { items: CompactFacility[]; nextCursor: string | null; hasMore: boolean }

/* Seconds a rendered page (and each upstream response) stays fresh. */
export const REVALIDATE_SECONDS = 300;
/* The duty roster by date is cacheable upstream for one minute. */
export const DUTY_REVALIDATE_SECONDS = 60;

function apiOrigin(): string | null {
  const origin = process.env.PUBLIC_API_ORIGIN?.trim();
  if (!origin || origin.includes("ROOT_DOMAIN")) return null;
  return origin.replace(/\/+$/, "");
}

/*
 * The website server's shared key (the API's WEB_SERVER_API_KEY). Every visitor
 * reaches the API through this one server, so a per-address limit meant for one
 * person would throttle the whole site; with the key, the API counts these reads
 * under the site's own limit. It grants no data or permission. Server-only.
 */
function webServerHeaders(): Record<string, string> {
  const key = process.env.WEB_SERVER_API_KEY?.trim();
  return key ? { "X-Daliini-Web-Key": key } : {};
}

/* GET /api/v1/<path>; null when the API is unavailable, undefined on a 404. */
async function getJson<T>(
  path: string,
  query: Record<string, string | undefined> = {},
  revalidate: number = REVALIDATE_SECONDS,
): Promise<T | null | undefined> {
  const origin = apiOrigin();
  if (!origin) return null;
  const url = new URL(`${origin}/api/v1/${path}`);
  for (const [key, value] of Object.entries(query)) if (value) url.searchParams.set(key, value);
  try {
    const response = await fetch(url, {
      headers: { Accept: "application/json", "Accept-Language": "ar", ...webServerHeaders() },
      next: { revalidate },
      signal: AbortSignal.timeout(8000),
    });
    if (response.status === 404) return undefined;
    if (!response.ok) return null;
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export const isUuid = (value: string) => UUID.test(value);

export async function getProvinces(): Promise<Province[] | null> {
  return (await getJson<{ items: Province[] }>("public/provinces/"))?.items ?? null;
}

/*
 * Provinces are addressed by their stable `code` in URLs (e.g. /raqqa).
 * Returns null when the API is unavailable and undefined when the code is
 * unknown, so callers can show an error state vs. a 404.
 */
export async function getProvinceByCode(code: string): Promise<Province | null | undefined> {
  const provinces = await getProvinces();
  if (!provinces) return null;
  const wanted = decodeURIComponent(code).toLowerCase();
  return provinces.find((p) => p.code.toLowerCase() === wanted);
}

export async function getCategories(provinceId: string): Promise<Category[] | null> {
  return (await getJson<{ items: Category[] }>(`public/provinces/${encodeURIComponent(provinceId)}/categories/`))?.items ?? null;
}

export async function getFacilities(params: {
  provinceId: string;
  categoryId?: string;
  cursor?: string;
  dutyNow?: boolean;
  /* Integer ids from getCategoryTags; the API matches them only where the category offers the filter. */
  specialtyId?: number;
  serviceTagId?: number;
  limit?: number;
}): Promise<FacilityPage | null> {
  return (await getJson<FacilityPage>("public/facilities/", {
    provinceId: params.provinceId,
    categoryId: params.categoryId,
    cursor: params.cursor,
    dutyNow: params.dutyNow ? "true" : undefined,
    specialtyId: params.specialtyId ? String(params.specialtyId) : undefined,
    serviceTagId: params.serviceTagId ? String(params.serviceTagId) : undefined,
    limit: String(params.limit ?? 30),
  })) ?? null;
}

/*
 * The active specialties and services of a category, in the team's order.
 * null when the API is unavailable, undefined when the category is not public.
 */
export async function getCategoryTags(categoryId: string): Promise<CategoryTags | null | undefined> {
  if (!isUuid(categoryId)) return undefined;
  return getJson<CategoryTags>(`public/categories/${categoryId}/tags/`);
}

/* A specialty or service id from the address: a positive whole number (exact as a JS number), or nothing. */
export function tagId(value: string | string[] | undefined): number | undefined {
  return typeof value === "string" && /^[1-9][0-9]{0,14}$/.test(value) ? Number(value) : undefined;
}

/* undefined = no such (published) facility; null = API unavailable. */
export async function getFacility(id: string): Promise<FacilityDetail | null | undefined> {
  if (!isUuid(id)) return undefined;
  return getJson<FacilityDetail>(`public/facilities/${id}/`);
}

/* Pharmacies whose duty shift is running right now, for every active province; null when the API is down. */
export async function getDutyByProvince(): Promise<{ province: Province; items: CompactFacility[] }[] | null> {
  const provinces = await getProvinces();
  if (!provinces) return null;
  const pages = await Promise.all(
    provinces.map((p) => getJson<FacilityPage>("public/facilities/", { provinceId: p.id, dutyNow: "true", limit: "50" })),
  );
  return provinces.map((province, i) => ({ province, items: pages[i]?.items ?? [] }));
}

export interface DutyShift { facilityId: string; startsAt: string; endsAt: string }

/* One Damascus calendar day of the roster: its pharmacies (by name) and their shifts overlapping it. */
export interface DutyDay { date: string; items: CompactFacility[]; shifts: DutyShift[] }

export const DUTY_WEEK_DAYS = 7;

/*
 * The duty roster for today and the six days after it, per active province
 * (GET public/duty/). "Today" is the API's own Damascus day, so the site never
 * guesses a date. Today, tomorrow and the week all read this one response, so
 * the three pages share one cached upstream call per province. A province whose
 * roster could not be loaded has `days: null`; the whole result is null when the
 * provinces themselves could not be loaded.
 */
export async function getDutyRosterByProvince(): Promise<{ province: Province; days: DutyDay[] | null }[] | null> {
  const provinces = await getProvinces();
  if (!provinces) return null;
  const rosters = await Promise.all(
    provinces.map((p) =>
      getJson<{ provinceId: string; days: DutyDay[] }>(
        "public/duty/",
        { provinceId: p.id, days: String(DUTY_WEEK_DAYS) },
        DUTY_REVALIDATE_SECONDS,
      ),
    ),
  );
  return provinces.map((province, i) => ({ province, days: rosters[i]?.days ?? null }));
}

/* Search needs at least this many characters (the API rejects shorter terms). */
export const MIN_QUERY_LENGTH = 2;

/* One page of search results inside a province; null when the API is down. */
export async function searchFacilities(params: {
  q: string;
  provinceId: string;
  categoryId?: string;
  cursor?: string;
  limit?: number;
}): Promise<FacilityPage | null> {
  if (params.q.trim().length < MIN_QUERY_LENGTH) return { items: [], nextCursor: null, hasMore: false };
  return (await getJson<FacilityPage>("public/search/", {
    q: params.q.trim(),
    provinceId: params.provinceId,
    categoryId: params.categoryId && isUuid(params.categoryId) ? params.categoryId : undefined,
    cursor: params.cursor,
    limit: String(params.limit ?? 30),
  })) ?? null;
}

/* Every category offered in at least one of the given provinces, first seen first. */
export async function getCategoriesFor(provinces: Province[]): Promise<Category[] | null> {
  const lists = await Promise.all(provinces.map((p) => getCategories(p.id)));
  if (lists.length > 0 && lists.every((l) => l === null)) return null;
  const seen = new Map<string, Category>();
  for (const list of lists) for (const c of list ?? []) if (!seen.has(c.id)) seen.set(c.id, c);
  return [...seen.values()];
}

export type ContentKind = "LEGAL" | "FAQ" | "PAGE";

/* A page the team publishes from the console. `bodyAr` is plain text; see lib/content.ts. */
export interface ContentPage {
  slug: string;
  kind: ContentKind;
  titleAr: string;
  bodyAr: string;
  version: number;
  publishedAt: string | null;
  updatedAt: string;
}

/* The API's page slugs: lower-case letters, digits and single hyphens, at most 64 characters. */
const CONTENT_SLUG = /^[a-z0-9](?:[a-z0-9-]{0,62}[a-z0-9])?$/;
export const isContentSlug = (value: string) => CONTENT_SLUG.test(value);

/* The published version of a page; undefined when unknown or unpublished, null when the API is down. */
export async function getContentPage(slug: string): Promise<ContentPage | null | undefined> {
  if (!isContentSlug(slug)) return undefined;
  return getJson<ContentPage>(`content/pages/${slug}/`);
}

export interface FaqEntry { id: string; questionAr: string; answerAr: string; sortOrder: number }

/* The published questions and answers, in the team's order; null when the API is down. */
export async function getFaq(): Promise<FaqEntry[] | null> {
  return (await getJson<{ items: FaqEntry[] }>("content/faq/"))?.items ?? null;
}

export type EmergencyKind = "AMBULANCE" | "FIRE" | "POLICE" | "HOSPITAL" | "OTHER";

export interface EmergencyNumber {
  id: string;
  /* NATIONAL numbers apply everywhere; PROVINCE ones only to provinceId. */
  scope: "NATIONAL" | "PROVINCE";
  provinceId: string | null;
  labelAr: string;
  /* What to dial: digits with an optional leading +. */
  phone: string;
  kind: EmergencyKind;
  sortOrder: number;
}

/* National numbers first, then the given province's; null when the API is down. */
export async function getEmergencyNumbers(provinceId?: string): Promise<EmergencyNumber[] | null> {
  return (
    (await getJson<{ items: EmergencyNumber[] }>("emergency-numbers/", {
      provinceId: provinceId && isUuid(provinceId) ? provinceId : undefined,
    }))?.items ?? null
  );
}
