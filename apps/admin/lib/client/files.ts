"use client";

import type { ApiErrorBody } from "../errors/messages";
import type { Result } from "./api";

/**
 * The two BFF calls that move a file rather than JSON: uploading an advertisement image
 * and downloading a CSV export.
 *
 * Kept beside `api.ts` rather than inside it because their bodies are different, but the
 * contract is the same: same-origin routes only, cookies travel on their own, and every
 * failure comes back as the `{code, message, details, requestId}` envelope.
 */

const NETWORK_FAILURE: ApiErrorBody = {
  code: "NETWORK_UNAVAILABLE",
  message: "تعذر الاتصال.",
  details: {},
  requestId: "",
};

async function failure(response: Response): Promise<ApiErrorBody> {
  try {
    const body: unknown = await response.json();
    if (body && typeof body === "object" && typeof (body as ApiErrorBody).code === "string") {
      return body as ApiErrorBody;
    }
  } catch {
    // Not the envelope; fall through.
  }
  return NETWORK_FAILURE;
}

export type AdImage = Readonly<{ imageKey: string; url: string; width: number; height: number }>;

/**
 * Upload one image as multipart. The browser sets the boundary itself, and `Origin` with
 * it, which is what the route's same-origin check reads.
 */
export async function uploadAdImage(file: File): Promise<Result<AdImage>> {
  const form = new FormData();
  form.append("file", file);
  try {
    const response = await fetch("/api/admin/ads/images", {
      method: "POST",
      credentials: "same-origin",
      body: form,
    });
    if (response.ok) return { ok: true, data: (await response.json()) as AdImage };
    return { ok: false, status: response.status, error: await failure(response) };
  } catch {
    return { ok: false, status: 0, error: NETWORK_FAILURE };
  }
}

/** The name the server chose, from `Content-Disposition`, or a plain fallback. */
function fileNameFrom(response: Response, fallback: string): string {
  const header = response.headers.get("content-disposition") ?? "";
  const match = /filename="([^"]+)"/.exec(header);
  return match?.[1] ?? fallback;
}

/**
 * Fetch a CSV export and hand it to the browser as a download.
 *
 * Fetched rather than linked so a refusal (no permission, an expired session) is shown as a
 * sentence on the page instead of being saved as a file named after the export.
 */
export async function downloadExport(
  name: "facilities" | "reports" | "audit",
  params: Record<string, string | undefined> = {},
): Promise<Result<null>> {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    const trimmed = value?.trim();
    if (trimmed) query.set(key, trimmed);
  }
  const suffix = query.toString();
  try {
    const response = await fetch(`/api/admin/export/${name}${suffix ? `?${suffix}` : ""}`, {
      credentials: "same-origin",
    });
    if (!response.ok) {
      return { ok: false, status: response.status, error: await failure(response) };
    }
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileNameFrom(response, `daliini-${name}.csv`);
    document.body.append(link);
    link.click();
    link.remove();
    // Give the browser a moment to start the download before the blob goes away.
    window.setTimeout(() => URL.revokeObjectURL(url), 30_000);
    return { ok: true, data: null };
  } catch {
    return { ok: false, status: 0, error: NETWORK_FAILURE };
  }
}
