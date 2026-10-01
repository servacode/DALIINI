import "server-only";
import type { AdminApis } from "./client";

/**
 * The CSV exports the BFF will stream, and nothing else.
 *
 * Exports are the one read whose answer is a file rather than JSON, so they cannot go
 * through the operation registry (whose route parses and re-serialises every body). They
 * get their own route instead, and this is its whole surface: three names, each with the
 * filters its list screen already uses. A name outside this table is a 404 before anything
 * reaches the network, and a query parameter outside a name's filters is dropped rather
 * than forwarded, so the browser cannot use the route to reach any other upstream option.
 *
 * The permission is the upstream one — each export needs the same read permission as the
 * list it mirrors — so nothing here decides who may export.
 */

type Query = Readonly<Record<string, string>>;

type ExportDefinition = Readonly<{
  /** The filters this export accepts, spelled as the list screen sends them. */
  filters: readonly string[];
  /** The generated raw call: the response body is streamed, never parsed. */
  run: (apis: AdminApis, query: Query) => Promise<{ raw: Response }>;
}>;

export const EXPORTS = {
  facilities: {
    filters: ["status", "province", "category", "q", "issue", "ordering"],
    run: (apis, query) => apis.exports.adminExportFacilitiesCsvRaw(query),
  },
  reports: {
    filters: ["status", "facility"],
    run: (apis, query) => apis.exports.adminExportReportsCsvRaw(query),
  },
  audit: {
    filters: ["action", "actor", "resource", "requestId", "from", "to"],
    run: (apis, query) => apis.exports.adminExportAuditCsvRaw(query),
  },
} as const satisfies Record<string, ExportDefinition>;

export type ExportName = keyof typeof EXPORTS;

/** Own keys only, so `__proto__` or `constructor` can never name an export. */
export function isExportName(name: string): name is ExportName {
  return Object.hasOwn(EXPORTS, name);
}

/** The filters to forward: declared ones only, trimmed, and only when they carry a value. */
export function exportQuery(name: ExportName, search: URLSearchParams): Record<string, string> {
  const out: Record<string, string> = {};
  for (const key of EXPORTS[name].filters) {
    const value = search.get(key)?.trim();
    if (value) out[key] = value;
  }
  return out;
}

/**
 * The file name the browser saves, always built here.
 *
 * Nothing from the query string or the upstream response goes into the header, so a crafted
 * filter value cannot inject a header or a path. The stamp is Damascus time, which is what
 * the operator's clock says.
 */
export function exportFileName(name: ExportName, now: Date = new Date()): string {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("en-GB", {
      timeZone: "Asia/Damascus",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(now)
      .map((part) => [part.type, part.value]),
  );
  return `daliini-${name}-${parts.year}${parts.month}${parts.day}-${parts.hour}${parts.minute}.csv`;
}
