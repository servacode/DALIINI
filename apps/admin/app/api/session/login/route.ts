import { authApi, toFailure } from "../../../../lib/api/client";
import { startSession } from "../../../../lib/auth/session";
import { checkSameOrigin } from "../../../../lib/http/csrf";
import { badRequest, fail, ok, readJson, refuseOrigin } from "../../../../lib/http/responses";

/**
 * Exchange credentials for a session, and keep both tokens on this side of the boundary.
 *
 * `authLogin` returns an access token and a rotating refresh secret. Neither is in the
 * response the browser receives: they go into `HttpOnly` cookies here, and every later call
 * comes back through the BFF, which reads them server-side.
 */
export async function POST(request: Request): Promise<Response> {
  const rejection = checkSameOrigin(request);
  if (rejection) return refuseOrigin(rejection, "/api/session/login");

  const body = await readJson(request);
  const phone = typeof body?.phone === "string" ? body.phone.trim() : "";
  const password = typeof body?.password === "string" ? body.password : "";

  // Shape only. Whether the credentials are correct is the backend's answer, and asking it
  // here would mean two copies of the rule.
  const details: Record<string, string[]> = {};
  if (!phone) details.phone = ["أدخل رقم الهاتف."];
  if (!password) details.password = ["أدخل كلمة المرور."];
  if (Object.keys(details).length > 0) {
    return badRequest("VALIDATION_ERROR", "تعذر تسجيل الدخول.", details);
  }

  try {
    const credentials = await authApi().authLogin({
      login: { phone, password, platform: "WEB", deviceName: "Admin console" },
    });
    await startSession(credentials);
    return ok({ ok: true });
  } catch (error) {
    // An upstream envelope is written to be shown to a person and names no internals, so it
    // is forwarded unchanged. Anything else — a refused connection, a reset socket — becomes
    // a bare 502, because a framework error page would carry a stack trace and the API URL.
    const failure = await toFailure(error, "login");
    return fail(failure.status, failure.body);
  }
}
