import { callWithSession } from "../../../../../lib/auth/session";
import { checkSameOrigin } from "../../../../../lib/http/csrf";
import { fail, ok, refuseOrigin } from "../../../../../lib/http/responses";
import { readBounded } from "../../../../../lib/http/upload";

/** Rosters are small tables; a megabyte is thousands of rows, past the backend's own limit. */
const MAX_ROSTER_BYTES = 1024 * 1024;

function refused(status: number, message: string, code = "VALIDATION_ERROR"): Response {
  return fail(status, { code, message, details: { file: [message] }, requestId: "" });
}

/**
 * Upload a duty roster (CSV or XLSX) to preview it, or to apply it.
 *
 * Holds the same line as the other multipart route: `POST` only, the same-origin check, a hard
 * ceiling on what is read, and the session carried server-side. Django reads and checks every
 * row and re-checks `admin.duty.manage`; this only refuses what is plainly not a roster.
 */
export async function POST(request: Request): Promise<Response> {
  const rejection = checkSameOrigin(request, { body: "multipart" });
  if (rejection) return refuseOrigin(rejection, "/api/admin/duty/import");

  const raw = await readBounded(request, MAX_ROSTER_BYTES + 64 * 1024);
  if (raw === null) return refused(413, "الملف أكبر من ١ ميغابايت. قسّمه إلى ملفات أصغر.", "FILE_TOO_LARGE");

  let form: FormData;
  try {
    form = await new Response(raw, {
      headers: { "Content-Type": request.headers.get("content-type") ?? "" },
    }).formData();
  } catch {
    return refused(400, "اختر ملف الجدول لرفعه.");
  }

  const file = form.get("file");
  if (!(file instanceof File) || file.size === 0) return refused(400, "اختر ملف الجدول لرفعه.");
  if (!/\.(csv|xlsx|xlsm)$/i.test(file.name)) {
    return refused(400, "الجدول يجب أن يكون بصيغة CSV أو XLSX.");
  }
  const provinceId = String(form.get("provinceId") ?? "");
  const apply = form.get("apply") === "true";

  const outcome = await callWithSession((apis) =>
    apis.duty.adminDutyImport({ file, provinceId, apply }),
  );
  if (!outcome.ok) return fail(outcome.status, outcome.body);
  return ok(outcome.data);
}
