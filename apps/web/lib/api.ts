/*
 * Server-side reader for the public (anonymous) Daliini API.
 *
 * The shapes below mirror the generated models in packages/api-typescript
 * (CompactFacility, PublicFacilityDetail, PublicProvince, PublicCategory,
 * FacilityCursorPage). We do not import the generated client itself: it ships
 * as an unbuilt package (dist/ is produced by its own `prepare` step) and the
 * public site only needs a handful of GET endpoints, so a small typed fetch
 * keeps the web build independent of that package's build. Keep these in sync
 * with the OpenAPI schema when public fields change.
 *
 * Only server components import this module; PUBLIC_API_ORIGIN has no
 * NEXT_PUBLIC_ prefix so it is never inlined into client bundles.
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
  /* When staff last confirmed the details, and when anything last changed. */
  lastVerifiedAt?: string | null;
  updatedAt?: string | null;
}

export interface HoursEntry { id: string; weekday: number; opensAt: string; closesAt: string; sequence: number }

export interface FacilityDetail extends CompactFacility {
  descriptionAr: string | null;
  phone: string | null;
  addressAr: string | null;
  neighborhood: Ref | null;
  location: { latitude: number; longitude: number } | null;
  hours: HoursEntry[];
  whatsapp?: string | null;
}

export interface Province { id: string; code: string; nameAr: string; nameEn: string | null }

export interface Category {
  id: string;
  nameAr: string;
  nameEn: string | null;
  iconKey: string | null;
  group: Ref;
  capabilities: { duty: boolean; hours: boolean; ratings: boolean };
}

export interface FacilityPage { items: CompactFacility[]; nextCursor: string | null; hasMore: boolean }

/* Seconds a rendered page (and each upstream response) stays fresh. */
export const REVALIDATE_SECONDS = 300;

function apiOrigin(): string | null {
  const origin = process.env.PUBLIC_API_ORIGIN?.trim();
  if (!origin || origin.includes("ROOT_DOMAIN")) return null;
  return origin.replace(/\/+$/, "");
}

async function getJson<T>(path: string, query: Record<string, string | undefined> = {}): Promise<T | null | undefined> {
  const origin = apiOrigin();
  if (!origin) return null;
  const url = new URL(`${origin}/api/v1/public/${path}`);
  for (const [key, value] of Object.entries(query)) if (value) url.searchParams.set(key, value);
  try {
    const response = await fetch(url, {
      headers: { Accept: "application/json", "Accept-Language": "ar" },
      next: { revalidate: REVALIDATE_SECONDS },
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
  return (await getJson<{ items: Province[] }>("provinces/"))?.items ?? null;
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
  return (await getJson<{ items: Category[] }>(`provinces/${encodeURIComponent(provinceId)}/categories/`))?.items ?? null;
}

export async function getFacilities(params: {
  provinceId: string;
  categoryId?: string;
  cursor?: string;
  dutyNow?: boolean;
  limit?: number;
}): Promise<FacilityPage | null> {
  return (await getJson<FacilityPage>("facilities/", {
    provinceId: params.provinceId,
    categoryId: params.categoryId,
    cursor: params.cursor,
    dutyNow: params.dutyNow ? "true" : undefined,
    limit: String(params.limit ?? 30),
  })) ?? null;
}

/* undefined = no such (published) facility; null = API unavailable. */
export async function getFacility(id: string): Promise<FacilityDetail | null | undefined> {
  if (!isUuid(id)) return undefined;
  return getJson<FacilityDetail>(`facilities/${id}/`);
}

/*
 * The duty roster the public API can answer: "now" (a shift is running) or
 * "today" (on today's roster). The API has no roster for other dates yet.
 */
export type DutyWhen = "now" | "today";

/* Duty pharmacies for every active province; null when the API is down. */
export async function getDutyByProvince(when: DutyWhen = "now"): Promise<{ province: Province; items: CompactFacility[] }[] | null> {
  const provinces = await getProvinces();
  if (!provinces) return null;
  const pages = await Promise.all(
    provinces.map((p) =>
      getJson<FacilityPage>("facilities/", {
        provinceId: p.id,
        dutyNow: when === "now" ? "true" : undefined,
        dutyToday: when === "today" ? "true" : undefined,
        limit: "50",
      }),
    ),
  );
  return provinces.map((province, i) => ({ province, items: pages[i]?.items ?? [] }));
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
  return (await getJson<FacilityPage>("search/", {
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

export interface PublishedPage { key: string; titleAr: string; version: number; publishedAt: string | null; bodyAr: string }

/* A page the team publishes from the console (FAQ, about…); undefined when unpublished. */
export async function getPublishedPage(key: "FAQ" | "ABOUT" | "INSTRUCTIONS"): Promise<PublishedPage | null | undefined> {
  return getJson<PublishedPage>(`legal/${key}/`);
}
