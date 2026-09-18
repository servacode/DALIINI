import "server-only";
import type { SessionCredentials } from "@servacode/api-typescript";

import { type AdminApis, type ApiErrorBody, adminApis, authApi, toFailure } from "../api/client";
import {
  clearSessionCookies,
  getAccessCookie,
  getRefreshCookie,
  setAccessCookie,
  setRefreshCookie,
} from "./cookies";

/**
 * Session lifecycle for the Admin BFF: start it, use it, refresh it once, end it.
 *
 * The browser never holds either token. It sends its `HttpOnly` cookies to a same-origin
 * route handler here, the handler puts the access token in an `Authorization` header on a
 * server-to-server call, and only the result crosses back.
 *
 * Refreshing has to happen in a route handler, not during a page render: Next only allows
 * `cookies().set` where there is a response to attach it to, and a rotated refresh secret
 * that cannot be written back is a refresh secret that has been thrown away.
 */

const UNAUTHENTICATED: ApiErrorBody = {
  code: "AUTHENTICATION_REQUIRED",
  message: "انتهت الجلسة. سجّل الدخول من جديد.",
  details: {},
  requestId: "",
};

/** Fallback access-cookie lifetime when the token carries no readable expiry. */
const FALLBACK_ACCESS_SECONDS = 600;

/**
 * Read `exp` out of the access token so the cookie expires with the token it holds.
 *
 * The payload is read, never trusted: this only decides when to stop sending a token the
 * server would reject anyway. Verification is the backend's job and stays there.
 */
function accessLifetimeSeconds(accessToken: string): number {
  const payload = accessToken.split(".")[1];
  if (!payload) return FALLBACK_ACCESS_SECONDS;
  try {
    const decoded: unknown = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    const exp = (decoded as { exp?: unknown })?.exp;
    if (typeof exp === "number") {
      // A minute of headroom, so a token is replaced slightly before it is refused.
      const remaining = Math.floor(exp - Date.now() / 1000) - 60;
      if (remaining > 0) return remaining;
    }
  } catch {
    // Not a readable JWT payload. The fallback below is short enough to be harmless.
  }
  return FALLBACK_ACCESS_SECONDS;
}

export async function startSession(credentials: SessionCredentials): Promise<void> {
  const refreshSeconds = Math.floor((credentials.expiresAt.getTime() - Date.now()) / 1000);
  await setRefreshCookie(
    { refreshToken: credentials.refreshToken, sessionId: credentials.sessionId },
    refreshSeconds,
  );
  await setAccessCookie(credentials.accessToken, accessLifetimeSeconds(credentials.accessToken));
}

/**
 * Revoke the session upstream, then drop the cookies.
 *
 * The cookies are cleared whatever the upstream call does. A logout that leaves a browser
 * holding a session because the network hiccuped is worse than a server-side session that
 * outlives its cookie and expires on its own.
 */
export async function endSession(): Promise<void> {
  const material = await getRefreshCookie();
  const access = await getAccessCookie();
  if (material && access) {
    try {
      await adminApis(access).auth.authLogout({
        logoutRequest: { sessionId: material.sessionId },
      });
    } catch {
      // Already revoked, expired, or unreachable. Clearing the cookies is what matters.
    }
  }
  await clearSessionCookies();
}

/**
 * One refresh at a time per session.
 *
 * The refresh secret rotates on use and the backend treats a replayed one as a stolen one,
 * so two concurrent requests both noticing a stale access token must not both refresh — the
 * second would present a secret the first has already spent and get the whole session
 * flagged as compromised. Concurrent callers join the in-flight attempt instead.
 *
 * This coalesces within one Node process. Several Admin instances behind a load balancer
 * would still need a shared lock; with sticky sessions or a single instance, which is what
 * the deployment describes today, this is sufficient.
 */
const inFlight = new Map<string, Promise<string | null>>();

async function refreshAccessToken(): Promise<string | null> {
  const material = await getRefreshCookie();
  if (!material) return null;

  const existing = inFlight.get(material.sessionId);
  if (existing) return existing;

  const attempt = (async (): Promise<string | null> => {
    try {
      const credentials = await authApi().authRefresh({
        refresh: { refreshToken: material.refreshToken },
      });
      await startSession(credentials);
      return credentials.accessToken;
    } catch {
      // Expired, revoked, or already rotated by someone else. Either way this browser has
      // nothing usable left, so the cookies go and the operator signs in again. No retry:
      // retrying a rejected refresh secret is what reuse detection is watching for.
      await clearSessionCookies();
      return null;
    } finally {
      inFlight.delete(material.sessionId);
    }
  })();

  inFlight.set(material.sessionId, attempt);
  return attempt;
}

export type SessionOutcome<T> =
  | Readonly<{ ok: true; data: T }>
  | Readonly<{ ok: false; status: number; body: ApiErrorBody }>;

/**
 * Run one upstream call with the current session, refreshing at most once.
 *
 * The retry is limited to a 401. Django's authentication runs before the view, so a 401
 * means the operation did not happen and replaying it cannot duplicate anything — which is
 * why the same closure can safely be re-run for a mutation as well as a read. Any other
 * status is returned as it came; a 403 is a permission answer, not a session problem, and
 * refreshing would only hide it.
 */
export async function callWithSession<T>(
  run: (apis: AdminApis) => Promise<T>,
): Promise<SessionOutcome<T>> {
  let access = await getAccessCookie();
  if (!access) {
    access = await refreshAccessToken();
    if (!access) return { ok: false, status: 401, body: UNAUTHENTICATED };
  }

  try {
    return { ok: true, data: await run(adminApis(access)) };
  } catch (error) {
    const failure = await toFailure(error, "admin call");
    if (failure.status !== 401) return { ok: false, status: failure.status, body: failure.body };

    const refreshed = await refreshAccessToken();
    if (!refreshed) return { ok: false, status: 401, body: UNAUTHENTICATED };

    try {
      return { ok: true, data: await run(adminApis(refreshed)) };
    } catch (retryError) {
      const retryFailure = await toFailure(retryError, "admin call retry");
      if (retryFailure.status === 401) {
        // A fresh token was refused. Something is wrong with the session itself; stop here
        // rather than loop.
        await clearSessionCookies();
        return { ok: false, status: 401, body: UNAUTHENTICATED };
      }
      return { ok: false, status: retryFailure.status, body: retryFailure.body };
    }
  }
}

/**
 * Rotate the session now, rather than waiting for a token to be refused.
 *
 * Used by the `/api/session/refresh` route: a tab coming back from sleep wants to know
 * whether it still has a session before it renders, and the qualification tests need to
 * drive rotation deliberately instead of waiting fifteen minutes for one.
 *
 * It shares the same in-flight coalescing as the automatic path, so calling it while a
 * data route is already refreshing joins that attempt instead of spending the secret twice.
 */
export async function rotateSession(): Promise<boolean> {
  return (await refreshAccessToken()) !== null;
}

/** True when this browser has something worth trying. Used by the route guard. */
export async function hasSessionCookies(): Promise<boolean> {
  return (await getAccessCookie()) !== null || (await getRefreshCookie()) !== null;
}
