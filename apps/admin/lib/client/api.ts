"use client";

import type { ApiErrorBody } from "../errors/messages";

/**
 * The browser's only way to reach data.
 *
 * It calls same-origin BFF routes by operation name. It never sees the API origin, never
 * builds a backend URL, and never holds a token: the session travels as `HttpOnly` cookies
 * the page cannot read, and the CSP would block a direct call to Django anyway.
 */

export type Result<T> =
  | Readonly<{ ok: true; data: T }>
  | Readonly<{ ok: false; status: number; error: ApiErrorBody }>;

const NETWORK_FAILURE: ApiErrorBody = {
  code: "NETWORK_UNAVAILABLE",
  message: "تعذر الاتصال.",
  details: {},
  requestId: "",
};

async function parse<T>(response: Response): Promise<Result<T>> {
  let body: unknown = null;
  try {
    body = await response.json();
  } catch {
    body = null;
  }
  if (response.ok) return { ok: true, data: body as T };
  const error =
    body && typeof body === "object" && typeof (body as ApiErrorBody).code === "string"
      ? (body as ApiErrorBody)
      : NETWORK_FAILURE;
  return { ok: false, status: response.status, error };
}

/** Run a read operation. Empty parameters are dropped rather than sent blank. */
export async function read<T>(
  operation: string,
  params: Record<string, string | undefined> = {},
): Promise<Result<T>> {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    const trimmed = value?.trim();
    if (trimmed) query.set(key, trimmed);
  }
  const suffix = query.toString();
  try {
    const response = await fetch(
      `/api/admin/${operation}${suffix ? `?${suffix}` : ""}`,
      { credentials: "same-origin", headers: { Accept: "application/json" } },
    );
    return await parse<T>(response);
  } catch {
    return { ok: false, status: 0, error: NETWORK_FAILURE };
  }
}

/** Run a write operation. Always `POST`, always JSON, so the origin check applies. */
export async function write<T>(
  operation: string,
  body: Record<string, unknown> = {},
): Promise<Result<T>> {
  try {
    const response = await fetch(`/api/admin/${operation}`, {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    return await parse<T>(response);
  } catch {
    return { ok: false, status: 0, error: NETWORK_FAILURE };
  }
}

/** End the session. Used by the shell's sign-out control. */
export async function logout(): Promise<void> {
  try {
    await fetch("/api/session/logout", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: "{}",
    });
  } catch {
    // The cookies are gone or unreachable either way; the caller redirects regardless.
  }
}
