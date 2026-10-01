import "server-only";
import {
  AccountApi,
  AdminAdsApi,
  AdminAnalyticsApi,
  AdminAuditApi,
  AdminFacilitiesApi,
  AdminProvincesApi,
  AdminReportsApi,
  AdminReviewsApi,
  AdminSettingsApi,
  AdminSystemApi,
  AdminTaxonomyApi,
  AdminUsersApi,
  AdminVerificationApi,
  AuthApi,
  Configuration,
  ResponseError,
  // Operations screens
  AdminContentApi,
  AdminDutyApi,
  AdminExportsApi,
  AdminNotificationsApi,
} from "@servacode/api-typescript";

/**
 * The only transport layer in this application.
 *
 * Every call to Django goes through a generated API class configured here. Nothing
 * hand-writes a request DTO, a response interface or a URL, because the generated client is
 * regenerated from `openapi/schema.yaml` and anything written beside it goes stale silently.
 *
 * This module is `server-only`. The browser talks to same-origin BFF routes under `/api/`,
 * never to Django: the CSP in `next.config.ts` sets `connect-src 'self'`, so a direct call
 * from a page would be blocked by the browser before it left.
 */

function apiOrigin(): string {
  const value = process.env.ADMIN_API_ORIGIN;
  if (!value) throw new Error("ADMIN_API_ORIGIN is required");
  return value.replace(/\/+$/, "");
}

/** Called from `instrumentation.ts` so a missing origin stops the boot, not the first login. */
export function assertApiOriginConfigured(): void {
  const origin = apiOrigin();
  try {
    new URL(origin);
  } catch {
    throw new Error(`ADMIN_API_ORIGIN is not a URL: ${origin}`);
  }
}

function configuration(accessToken?: string): Configuration {
  return new Configuration({
    // Origin only. The generated operations already carry the full `/api/v1/...` path,
    // because the contract's own paths do; appending the prefix here would double it.
    basePath: apiOrigin(),
    // The generated client asks for the token by scheme name and sets the Authorization
    // header itself; see `bearerAccessToken` in the contract.
    ...(accessToken ? { accessToken: async () => accessToken } : {}),
    fetchApi: (input, init) => fetch(input, { ...init, cache: "no-store" }),
  });
}

/** Unauthenticated surface: login and refresh, which is where a session comes from. */
export function authApi(): AuthApi {
  return new AuthApi(configuration());
}

/** Authenticated surface, one bearer token per caller. */
export function adminApis(accessToken: string) {
  const config = configuration(accessToken);
  return {
    account: new AccountApi(config),
    ads: new AdminAdsApi(config),
    analytics: new AdminAnalyticsApi(config),
    audit: new AdminAuditApi(config),
    auth: new AuthApi(config),
    facilities: new AdminFacilitiesApi(config),
    provinces: new AdminProvincesApi(config),
    reports: new AdminReportsApi(config),
    reviews: new AdminReviewsApi(config),
    settings: new AdminSettingsApi(config),
    system: new AdminSystemApi(config),
    taxonomy: new AdminTaxonomyApi(config),
    users: new AdminUsersApi(config),
    verification: new AdminVerificationApi(config),
    // Operations screens
    content: new AdminContentApi(config),
    duty: new AdminDutyApi(config),
    exports: new AdminExportsApi(config),
    notifications: new AdminNotificationsApi(config),
  };
}

export type AdminApis = ReturnType<typeof adminApis>;

/** The unified error envelope every Django failure carries. See `core/exceptions.py`. */
export type ApiErrorBody = Readonly<{
  code: string;
  message: string;
  details: Readonly<Record<string, readonly string[]>>;
  requestId: string;
}>;

const UNKNOWN_FAILURE: ApiErrorBody = {
  code: "UPSTREAM_UNAVAILABLE",
  message: "تعذر الوصول إلى الخدمة. حاول مرة أخرى.",
  details: {},
  requestId: "",
};

/**
 * Turn a thrown generated-client error into the envelope, or return null if it is not one.
 *
 * The generated client throws `ResponseError` carrying the untouched `Response`, so the
 * body is read here rather than reconstructed. A body that is not the envelope — a proxy
 * error page, say — becomes a generic failure rather than being forwarded to the browser,
 * because whatever it contains was not written for an operator to read.
 */
export async function readApiError(error: unknown): Promise<{
  status: number;
  body: ApiErrorBody;
} | null> {
  if (!(error instanceof ResponseError)) return null;
  const status = error.response.status;
  try {
    const parsed: unknown = await error.response.clone().json();
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      typeof (parsed as ApiErrorBody).code === "string" &&
      typeof (parsed as ApiErrorBody).message === "string"
    ) {
      const body = parsed as ApiErrorBody;
      return {
        status,
        body: {
          code: body.code,
          message: body.message,
          details: body.details ?? {},
          requestId: body.requestId ?? "",
        },
      };
    }
  } catch {
    // Fall through to the generic failure below.
  }
  return { status, body: UNKNOWN_FAILURE };
}

/**
 * Turn any thrown error into a failure the browser may see.
 *
 * `readApiError` only recognises an answer from Django. A connection refused, a DNS
 * failure or a socket reset throws `FetchError` instead, and letting that escape produces
 * a framework error page carrying a stack trace and the upstream URL — which is exactly
 * what must never reach an operator. Anything unrecognised becomes a bare 502 here, with
 * the detail written to the server log instead.
 */
export async function toFailure(
  error: unknown,
  context: string,
): Promise<{ status: number; body: ApiErrorBody }> {
  const recognised = await readApiError(error);
  if (recognised) return recognised;
  console.error(`[admin-bff] ${context} could not reach the API:`, error);
  return { status: 502, body: UNKNOWN_FAILURE };
}

export { ResponseError };
