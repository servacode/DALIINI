import "server-only";
import { NextResponse } from "next/server";

import type { ApiErrorBody } from "../api/client";
import type { OriginRejection } from "./csrf";

/**
 * Every BFF response shape, in one place.
 *
 * Failures leave here in the same `{code, message, details, requestId}` envelope Django
 * uses, so the browser has one shape to handle whether the refusal came from the BFF or
 * from the API behind it.
 *
 * Nothing else travels. No stack trace, no upstream URL, no header, no token, no request
 * body echo, and no internal exception text — `readApiError` already replaces an
 * unrecognised upstream body with a generic failure before it can reach here.
 */

export function ok<T>(data: T, status = 200): NextResponse {
  return NextResponse.json(data, { status, headers: { "Cache-Control": "no-store" } });
}

export function fail(status: number, body: ApiErrorBody): NextResponse {
  return NextResponse.json(body, { status, headers: { "Cache-Control": "no-store" } });
}

export function badRequest(
  code: string,
  message: string,
  details: Record<string, readonly string[]> = {},
): NextResponse {
  return fail(400, { code, message, details, requestId: "" });
}

/**
 * Turn an origin rejection into a response, logging the reason server-side only.
 *
 * The caller is told that the request was refused, not which of the three checks refused
 * it. Naming the failing check would help someone tune a cross-site attempt until it passes.
 */
export function refuseOrigin(rejection: OriginRejection, path: string): NextResponse {
  console.warn(`[admin-bff] refused ${path}: ${rejection.reason}`);
  return fail(403, {
    code: rejection.code,
    message: "تم رفض الطلب.",
    details: {},
    requestId: "",
  });
}

export const SESSION_EXPIRED: ApiErrorBody = {
  code: "AUTHENTICATION_REQUIRED",
  message: "انتهت الجلسة. سجّل الدخول من جديد.",
  details: {},
  requestId: "",
};

/** Read a JSON body, or null when it is absent or malformed. */
export async function readJson(request: Request): Promise<Record<string, unknown> | null> {
  try {
    const parsed: unknown = await request.json();
    if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
      return parsed as Record<string, unknown>;
    }
  } catch {
    // Treated as absent below.
  }
  return null;
}
