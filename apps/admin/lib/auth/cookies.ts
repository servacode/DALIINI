import "server-only";
import { cookies } from "next/headers";

/**
 * Session cookies for the Admin BFF.
 *
 * INT-012 was here. The refresh cookie was named `__Host-directory_admin_refresh` and set
 * `secure` only when `NODE_ENV === "production"`. A browser rejects a `__Host-` cookie that
 * arrives without `Secure`, so the cookie was silently dropped everywhere else and the
 * development login flow could not work at all.
 *
 * The prefix and the `Secure` attribute are not two decisions, they are one. `__Host-`
 * *requires* `Secure`, `Path=/` and no `Domain`, so a name carrying that prefix is only
 * ever paired with a secure cookie. A plain http development origin gets the same name
 * without the prefix instead of a prefixed cookie the browser will refuse.
 *
 * Nothing here is readable by JavaScript. The refresh secret and the access token are both
 * `HttpOnly` and never reach `localStorage`, `sessionStorage`, IndexedDB or a script-visible
 * cookie; the browser only ever talks to the same-origin BFF, which does the bearer call.
 */

const REFRESH_BASE = "directory_admin_refresh";
const ACCESS_BASE = "directory_admin_access";

/**
 * Whether cookies can carry `Secure`, decided by the origin this Admin is served from.
 *
 * `NODE_ENV` is the wrong signal and was the original bug: a production build served over
 * http and a development build served over https both exist, and a build flag knows about
 * neither. `ADMIN_PUBLIC_ORIGIN` is a fact about the deployment — the URL operators type —
 * so it cannot disagree with reality without the deployment already being misconfigured.
 *
 * Fail-closed: `http` is accepted only on loopback, where there is no network to intercept.
 * An `http` origin on any other host is refused outright rather than quietly downgraded,
 * because that combination means session cookies would cross a network in the clear.
 */
export function secureCookies(): boolean {
  const raw = process.env.ADMIN_PUBLIC_ORIGIN;
  if (!raw) {
    throw new Error(
      "ADMIN_PUBLIC_ORIGIN is required; session cookie attributes are derived from it",
    );
  }
  let origin: URL;
  try {
    origin = new URL(raw);
  } catch {
    throw new Error(`ADMIN_PUBLIC_ORIGIN is not a URL: ${raw}`);
  }
  if (origin.protocol === "https:") return true;
  const loopback = new Set(["localhost", "127.0.0.1", "[::1]", "::1"]);
  if (!loopback.has(origin.hostname)) {
    throw new Error(
      "ADMIN_PUBLIC_ORIGIN must use https outside loopback; session cookies would " +
        "otherwise cross the network without Secure",
    );
  }
  return false;
}

/** `__Host-<base>` where cookies are secure, and the bare `<base>` where they cannot be. */
export function cookieName(base: string): string {
  return secureCookies() ? `__Host-${base}` : base;
}

export const refreshCookieName = () => cookieName(REFRESH_BASE);
export const accessCookieName = () => cookieName(ACCESS_BASE);

type Attributes = {
  httpOnly: true;
  secure: boolean;
  sameSite: "strict";
  path: "/";
  maxAge?: number;
};

/** `__Host-` also requires `Path=/` and no `Domain`; both hold in every branch below. */
function attributes(maxAgeSeconds?: number): Attributes {
  return {
    httpOnly: true,
    secure: secureCookies(),
    sameSite: "strict",
    path: "/",
    ...(maxAgeSeconds === undefined ? {} : { maxAge: maxAgeSeconds }),
  };
}

export type RefreshMaterial = Readonly<{
  refreshToken: string;
  sessionId: string;
}>;

export async function setRefreshCookie(
  material: RefreshMaterial,
  maxAgeSeconds: number,
): Promise<void> {
  const store = await cookies();
  // The session id travels with the refresh secret because `authLogout` needs it, and
  // keeping them in one cookie means they cannot fall out of step with each other.
  const value = Buffer.from(JSON.stringify(material), "utf8").toString("base64url");
  store.set(refreshCookieName(), value, attributes(Math.max(1, Math.floor(maxAgeSeconds))));
}

export async function getRefreshCookie(): Promise<RefreshMaterial | null> {
  const store = await cookies();
  const raw = store.get(refreshCookieName())?.value;
  if (!raw) return null;
  try {
    const parsed: unknown = JSON.parse(Buffer.from(raw, "base64url").toString("utf8"));
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      typeof (parsed as RefreshMaterial).refreshToken === "string" &&
      typeof (parsed as RefreshMaterial).sessionId === "string"
    ) {
      return parsed as RefreshMaterial;
    }
  } catch {
    // A cookie we cannot parse is a cookie we do not trust. Treated as absent so the
    // caller sends the operator back to login rather than retrying against nonsense.
  }
  return null;
}

export async function setAccessCookie(token: string, maxAgeSeconds: number): Promise<void> {
  const store = await cookies();
  store.set(accessCookieName(), token, attributes(Math.max(1, Math.floor(maxAgeSeconds))));
}

export async function getAccessCookie(): Promise<string | null> {
  const store = await cookies();
  return store.get(accessCookieName())?.value ?? null;
}

/** Clear both cookies under every name this build could have written. */
export async function clearSessionCookies(): Promise<void> {
  const store = await cookies();
  for (const base of [REFRESH_BASE, ACCESS_BASE]) {
    store.delete(cookieName(base));
  }
}
