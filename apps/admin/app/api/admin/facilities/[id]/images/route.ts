import { callWithSession } from "../../../../../../lib/auth/session";
import { checkSameOrigin } from "../../../../../../lib/http/csrf";
import { fail, ok, refuseOrigin } from "../../../../../../lib/http/responses";
import {
  BODY_TOO_LARGE,
  MAX_UPLOAD_BODY_BYTES,
  NO_FILE,
  arabicUploadFailure,
  checkAdImage,
  readBounded,
} from "../../../../../../lib/http/upload";

type Context = { params: Promise<{ id: string }> };

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/**
 * Add one public photo to a facility (DECISION-115).
 *
 * The advertisement route's twin: `POST` only, the same-origin check, the body read with a hard
 * ceiling, the part checked (present, not empty, at most 2 MB, JPEG/PNG/WebP by its bytes), and
 * only then forwarded to Django, which decodes, re-encodes and stores it and re-checks
 * `admin.facilities.edit`. The facility id is checked for shape before it reaches the client.
 */
export async function POST(request: Request, context: Context): Promise<Response> {
  const { id } = await context.params;
  const rejection = checkSameOrigin(request, { body: "multipart" });
  if (rejection) return refuseOrigin(rejection, "/api/admin/facilities/[id]/images");
  if (!UUID.test(id)) {
    return fail(404, { code: "NOT_FOUND", message: "المنشأة غير موجودة.", details: {}, requestId: "" });
  }

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
    apis.facilities.adminFacilityImageCreate({ facilityId: id, file: file as File }),
  );
  if (!outcome.ok) return fail(outcome.status, arabicUploadFailure(outcome.body));
  return ok(outcome.data, 201);
}
