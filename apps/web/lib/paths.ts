/*
 * Where each thing lives on the site, in one place (DECISION-069).
 *
 * The id stays the address and the words after it are for the reader: a facility is
 * `/f/<id>/<slug>`, its slug made by the API from the Arabic name, so a renamed facility keeps
 * every link ever shared and the old words simply redirect to the new. A category is
 * `/<province>/<slug>`, whose slug is fixed in the reference data and never changes.
 *
 * Plain functions with no server-only import, so a client component builds the same links.
 */

type Addressable = { id: string; slug?: string | null };

/* Arabic words travel percent-encoded; a slug never contains a `/` or a `%` of its own. */
const segment = (slug: string) => encodeURIComponent(slug);

export function facilityPath(facility: Addressable): string {
  return facility.slug ? `/f/${facility.id}/${segment(facility.slug)}` : `/f/${facility.id}`;
}

export function categoryPath(provinceCode: string, category: Addressable): string {
  return `/${provinceCode}/${category.slug ? segment(category.slug) : category.id}`;
}

/* A path segment as the reader typed it: decoded when it arrives encoded, unchanged otherwise. */
export function decodedSegment(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}
