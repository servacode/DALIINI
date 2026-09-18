/**
 * The one place an error code becomes something a person reads.
 *
 * Every failure in this application arrives as the contract envelope — `{code, message,
 * details, requestId}` — because `core/exceptions.py` renders all of them and the BFF
 * forwards that shape unchanged. So the mapping needs to exist exactly once, keyed by
 * `code`, rather than as a `switch` in each screen that drifts from the others.
 *
 * `message` from the backend is already written for an operator and is already safe: the
 * handler never puts a stack trace, SQL, a token or an exception class in it. It is
 * preferred wherever it exists. This table covers the transport codes, where a single
 * phrase belongs to the whole application rather than to whichever endpoint happened to
 * refuse, and the cases where the BFF itself is the one refusing.
 *
 * Nothing here ever surfaces an API URL, a cookie, a header or a request body.
 */

export type ApiErrorBody = Readonly<{
  code: string;
  message: string;
  details: Readonly<Record<string, readonly string[]>>;
  requestId: string;
}>;

const TRANSPORT_MESSAGES: Readonly<Record<string, string>> = {
  VALIDATION_ERROR: "تحقق من الحقول المميزة وأعد المحاولة.",
  AUTHENTICATION_REQUIRED: "انتهت الجلسة. سجّل الدخول من جديد.",
  AUTHENTICATION_FAILED: "بيانات الدخول غير صحيحة.",
  PERMISSION_DENIED: "لا تملك الصلاحية لهذا الإجراء.",
  NOT_FOUND: "العنصر المطلوب غير موجود أو تم حذفه.",
  METHOD_NOT_ALLOWED: "هذا الإجراء غير متاح على هذا العنصر.",
  THROTTLED: "عدد المحاولات كبير. انتظر قليلاً ثم أعد المحاولة.",
  INTERNAL_ERROR: "حدث خطأ مؤقت. حاول مرة أخرى.",
  UPSTREAM_UNAVAILABLE: "تعذر الوصول إلى الخدمة. حاول مرة أخرى.",
  ORIGIN_REJECTED: "تم رفض الطلب. أعد تحميل الصفحة وحاول مجدداً.",
  NETWORK_UNAVAILABLE: "تعذر الاتصال. تحقق من الشبكة وحاول مرة أخرى.",
};

const FALLBACK = "حدث خطأ مؤقت. حاول مرة أخرى.";

/** True when this failure means the session is gone and the operator must sign in again. */
export function isSessionExpired(error: ApiErrorBody | null): boolean {
  return error?.code === "AUTHENTICATION_REQUIRED";
}

/** True when the backend refused on permission grounds rather than on input. */
export function isPermissionDenied(error: ApiErrorBody | null): boolean {
  return error?.code === "PERMISSION_DENIED";
}

/** True when the request lost a race with someone else's change. */
export function isConflict(error: ApiErrorBody | null, status?: number): boolean {
  return status === 409;
}

/**
 * The sentence to show.
 *
 * A transport code gets the application-wide phrase, because "you do not have permission"
 * should read the same everywhere. A domain code — `LAST_OWNER_PROTECTED`,
 * `DUTY_OVERLAP_OR_INVALID` — gets the backend's own message, which is the only place that
 * knows what the rule was.
 */
export function messageFor(error: ApiErrorBody | null): string {
  if (!error) return FALLBACK;
  const transport = TRANSPORT_MESSAGES[error.code];
  if (transport) return transport;
  return error.message?.trim() || FALLBACK;
}

/** Field-scoped messages for a form, keyed by the field path the backend used. */
export function fieldErrorsFor(error: ApiErrorBody | null): Record<string, string> {
  if (!error?.details) return {};
  const out: Record<string, string> = {};
  for (const [field, messages] of Object.entries(error.details)) {
    if (Array.isArray(messages) && messages.length > 0) out[field] = messages.join(" ");
  }
  return out;
}

/**
 * The correlation id, shown beside a failure so a support report can quote it.
 *
 * It identifies a request in the server log and carries nothing about the request itself.
 */
export function requestIdFor(error: ApiErrorBody | null): string | null {
  const value = error?.requestId?.trim();
  return value ? value : null;
}
