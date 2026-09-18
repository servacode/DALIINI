import { endSession } from "../../../../lib/auth/session";
import { checkSameOrigin } from "../../../../lib/http/csrf";
import { ok, refuseOrigin } from "../../../../lib/http/responses";

/**
 * End the session upstream and locally.
 *
 * Always answers 200. Whether the backend had already revoked the session, or could not be
 * reached at all, the browser's cookies are gone either way and there is nothing the
 * operator could usefully do differently — reporting a failure here would only invite a
 * retry that changes nothing.
 */
export async function POST(request: Request): Promise<Response> {
  const rejection = checkSameOrigin(request);
  if (rejection) return refuseOrigin(rejection, "/api/session/logout");

  await endSession();
  return ok({ ok: true });
}
