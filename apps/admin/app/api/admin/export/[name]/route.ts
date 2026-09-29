import {
  EXPORTS,
  exportFileName,
  exportQuery,
  isExportName,
} from "../../../../../lib/api/exports";
import { callWithSession } from "../../../../../lib/auth/session";
import { fail } from "../../../../../lib/http/responses";

/**
 * Stream one CSV export to the operator's browser.
 *
 * Separate from the JSON operation route because the payload is a file: the upstream body
 * is handed through as a stream, never buffered or parsed, so a fifty-thousand-row export
 * does not sit in this process's memory. The session and its single refresh work exactly as
 * for every other call, and a refusal still leaves as the JSON error envelope, so the page
 * can say what went wrong instead of saving an error as a spreadsheet.
 *
 * Only the three names in `lib/api/exports.ts` exist, each with its own filters. The saved
 * file name is built here, never taken from the request.
 */

type Context = { params: Promise<{ name: string }> };

export async function GET(request: Request, context: Context): Promise<Response> {
  const { name } = await context.params;
  if (!isExportName(name)) {
    return fail(404, {
      code: "NOT_FOUND",
      message: "العنصر المطلوب غير موجود.",
      details: {},
      requestId: "",
    });
  }

  const query = exportQuery(name, new URL(request.url).searchParams);
  const outcome = await callWithSession(async (apis) => {
    const response = await EXPORTS[name].run(apis, query);
    return response.raw.body;
  });
  if (!outcome.ok) return fail(outcome.status, outcome.body);

  return new Response(outcome.data, {
    headers: {
      "Content-Type": "text/csv; charset=utf-8",
      "Content-Disposition": `attachment; filename="${exportFileName(name)}"`,
      // An export of the audit trail or the owners' phone numbers must not outlive the
      // session in a browser or proxy cache.
      "Cache-Control": "no-store, private",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
