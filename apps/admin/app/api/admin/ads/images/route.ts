import { callWithSession } from "../../../../../lib/auth/session";
import { checkSameOrigin } from "../../../../../lib/http/csrf";
import { fail, ok, refuseOrigin } from "../../../../../lib/http/responses";
import {
  BODY_TOO_LARGE,
  MAX_UPLOAD_BODY_BYTES,
  NO_FILE,
  arabicUploadFailure,
  checkAdImage,
  readBounded,
} from "../../../../../lib/http/upload";

/**
 * Upload one advertisement image and answer with its key.
 *
 * The only BFF route that takes a file, so the only one whose body is multipart rather than
 * JSON. Everything else holds as for a JSON write: `POST` only, the same-origin check
 * (Sec-Fetch-Site and Origin, fail-closed), the operator's session carried server-side, and
 * failures in the usual envelope. The body is read with a hard ceiling, the part is checked
 * (present, not empty, at most 2 MB, JPEG/PNG/WebP by its bytes), and only then forwarded to
 * Django, which decodes, re-encodes and stores it and re-checks `admin.ads.manage`.
 *
 * The answer is `{imageKey, url, width, height}`; the page keeps the key for the save and
 * shows the image from the public media origin the CSP already allows.
 */

export async function POST(request: Request): Promise<Response> {
  const rejection = checkSameOrigin(request, { body: "multipart" });
  if (rejection) return refuseOrigin(rejection, "/api/admin/ads/images");

  const raw = await readBounded(request, MAX_UPLOAD_BODY_BYTES);
  if (raw === null) return fail(BODY_TOO_LARGE.status, BODY_TOO_LARGE.body);

  let form: FormData;
  try {
    form = await new Response(raw, {
      headers: { "Content-Type": request.headers.get("content-type") ?? "" },
    }).formData();
  } catch {
    return fail(NO_FILE.status, NO_FILE.body);
  }

  const file = form.get("file");
  const refused = await checkAdImage(file);
  if (refused) return fail(refused.status, refused.body);

  const outcome = await callWithSession((apis) =>
    apis.ads.adminAdImageUpload({ file: file as File }),
  );
  if (!outcome.ok) return fail(outcome.status, arabicUploadFailure(outcome.body));
  return ok(outcome.data, 201);
}
