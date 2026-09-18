import { callWithSession } from "../../../../../lib/auth/session";
import { fail } from "../../../../../lib/http/responses";

/**
 * Stream one verification document.
 *
 * Separate from the JSON operation route because the payload is a file, not an envelope.
 * The rules around it are the point: the request carries the operator's bearer token
 * server-side, the backend re-checks `admin.evidence.read` and records the access, and the
 * browser receives bytes from this origin. No storage key, no signed object-store URL and
 * no permanent link ever reaches the page — a link that outlived the session would be a
 * private document handed out without a further check.
 */

type Context = { params: Promise<{ id: string }> };

export async function GET(_request: Request, context: Context): Promise<Response> {
  const { id } = await context.params;

  const outcome = await callWithSession(async (apis) => {
    const response = await apis.reviews.adminEvidenceContentRetrieveRaw({ evidenceId: id });
    return {
      body: await response.raw.blob(),
      contentType: response.raw.headers.get("content-type") ?? "application/octet-stream",
    };
  });

  if (!outcome.ok) return fail(outcome.status, outcome.body);

  return new Response(outcome.data.body, {
    headers: {
      "Content-Type": outcome.data.contentType,
      // Never rendered inline and never cached: a private document must not sit in a
      // browser cache or a shared proxy after the operator has moved on.
      "Content-Disposition": "attachment",
      "Cache-Control": "no-store, private",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
