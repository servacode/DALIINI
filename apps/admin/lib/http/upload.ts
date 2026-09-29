import "server-only";
import type { ApiErrorBody } from "../api/client";

/**
 * The advertisement image upload, checked before it leaves this process.
 *
 * Django is the authority — it decodes the image, checks its sides, re-encodes it to JPEG
 * and strips its metadata — and nothing here replaces that. What these checks buy is an
 * early, Arabic answer for the obvious mistakes (no file, a PDF, a 9 MB photograph) without
 * streaming the bytes onward, and a hard ceiling on what the BFF will hold in memory for
 * one request.
 */

/** The backend's own limit (`content_services.media.MAX_AD_IMAGE_BYTES`). */
export const MAX_AD_IMAGE_BYTES = 2 * 1024 * 1024;

/** A maximal file plus room for the multipart boundary and part headers around it. */
export const MAX_UPLOAD_BODY_BYTES = MAX_AD_IMAGE_BYTES + 64 * 1024;

export const UPLOAD_MESSAGES = {
  missing: "اختر صورة لرفعها.",
  empty: "الملف فارغ. اختر صورة أخرى.",
  tooLarge: "حجم الصورة أكبر من ٢ ميغابايت. صغّرها ثم أعد المحاولة.",
  format: "الصورة يجب أن تكون بصيغة JPEG أو PNG أو WebP.",
  invalid: "تعذّرت قراءة الملف كصورة. اختر صورة أخرى.",
  dimensions: "طول كل ضلع في الصورة يجب أن يكون بين ١٠٠ و٤٠٩٦ بكسل.",
} as const;

export type UploadRefusal = Readonly<{ status: number; body: ApiErrorBody }>;

function refusal(status: number, message: string, code = "VALIDATION_ERROR"): UploadRefusal {
  return { status, body: { code, message, details: { file: [message] }, requestId: "" } };
}

export const BODY_TOO_LARGE = refusal(413, UPLOAD_MESSAGES.tooLarge, "FILE_TOO_LARGE");
export const NO_FILE = refusal(400, UPLOAD_MESSAGES.missing);

function ascii(bytes: Uint8Array, from: number, to: number): string {
  return String.fromCharCode(...bytes.subarray(from, to));
}

/**
 * The format by the file's first bytes.
 *
 * The declared type is only the browser's guess from the extension, so it is not what is
 * checked: a renamed PDF is refused here, and a JPEG saved as `.png` is accepted, because
 * the backend reads the bytes too.
 */
export function sniffImage(head: Uint8Array): "jpeg" | "png" | "webp" | null {
  if (head.length >= 3 && head[0] === 0xff && head[1] === 0xd8 && head[2] === 0xff) return "jpeg";
  const png = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
  if (head.length >= png.length && png.every((byte, index) => head[index] === byte)) return "png";
  if (head.length >= 12 && ascii(head, 0, 4) === "RIFF" && ascii(head, 8, 12) === "WEBP") {
    return "webp";
  }
  return null;
}

/** Null when the part may go on to Django, or the refusal to answer with. */
export async function checkAdImage(entry: FormDataEntryValue | null): Promise<UploadRefusal | null> {
  if (entry === null || typeof entry === "string") return NO_FILE;
  if (entry.size === 0) return refusal(400, UPLOAD_MESSAGES.empty);
  if (entry.size > MAX_AD_IMAGE_BYTES) return BODY_TOO_LARGE;
  const head = new Uint8Array(await entry.slice(0, 16).arrayBuffer());
  if (!sniffImage(head)) return refusal(400, UPLOAD_MESSAGES.format);
  return null;
}

/**
 * Django refuses a file in English (the wording of its image checks), inside `details.file`.
 * The operator reads Arabic, so the refusal is restated here; any other failure passes
 * through untouched.
 */
export function arabicUploadFailure(body: ApiErrorBody): ApiErrorBody {
  const reasons = body.details?.file ?? [];
  if (body.code !== "VALIDATION_ERROR" || reasons.length === 0) return body;
  const text = reasons.join(" ").toLowerCase();
  const message = text.includes("2 mb")
    ? UPLOAD_MESSAGES.tooLarge
    : text.includes("pixels")
      ? UPLOAD_MESSAGES.dimensions
      : text.includes("jpeg")
        ? UPLOAD_MESSAGES.format
        : text.includes("no file")
          ? UPLOAD_MESSAGES.missing
          : text.includes("empty")
            ? UPLOAD_MESSAGES.empty
            : UPLOAD_MESSAGES.invalid;
  return { ...body, message, details: { file: [message] } };
}

/**
 * Read at most `limit` bytes of the request body, or null when there is more.
 *
 * A declared `Content-Length` over the limit is refused without reading anything; a body
 * sent without one (chunked, or through a proxy that dropped it) is counted as it arrives
 * and abandoned at the limit, so either way the process never holds more than `limit`.
 */
export async function readBounded(
  request: Request,
  limit: number,
): Promise<Uint8Array<ArrayBuffer> | null> {
  const declared = Number(request.headers.get("content-length") ?? "");
  if (Number.isFinite(declared) && declared > limit) return null;
  if (!request.body) return new Uint8Array(0);

  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > limit) {
      await reader.cancel();
      return null;
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return bytes;
}
