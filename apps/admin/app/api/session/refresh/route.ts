import { rotateSession } from "../../../../lib/auth/session";
import { checkSameOrigin } from "../../../../lib/http/csrf";
import { SESSION_EXPIRED, fail, ok, refuseOrigin } from "../../../../lib/http/responses";

/**
 * Rotate the session on demand.
 *
 * Data routes refresh on their own when they meet a 401, so this exists for the cases that
 * cannot: a tab returning from sleep that wants to know whether it still has a session
 * before rendering, and the qualification tests, which need to drive rotation deliberately
 * rather than wait for a token to age out.
 *
 * The guarantees come from `rotateSession`: one refresh at a time per session, and a
 * cleared cookie jar on failure so the browser lands on the login screen instead of looping.
 */
export async function POST(request: Request): Promise<Response> {
  const rejection = checkSameOrigin(request);
  if (rejection) return refuseOrigin(rejection, "/api/session/refresh");

  const rotated = await rotateSession();
  if (!rotated) return fail(401, SESSION_EXPIRED);
  return ok({ ok: true });
}
