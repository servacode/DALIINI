package com.servacode.directory.core.model

/**
 * The one error shape the app works with above the network layer.
 *
 * It is built from the backend envelope `{code, message, details, requestId}` or from the
 * transport failure, never from a raw exception, so a screen can decide what to show without
 * knowing how the request was made. `requestId` is what an operator asks for when the user
 * reports a problem.
 */
data class AppError(
    val kind: Kind,
    val code: String? = null,
    val message: String? = null,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val requestId: String? = null,
    val status: Int? = null,
) {
    enum class Kind {
        /** No response: no network, DNS failure, timeout, connection reset. */
        OFFLINE,
        /** The session is missing, expired or revoked. */
        UNAUTHENTICATED,
        FORBIDDEN,
        NOT_FOUND,
        /** The request was well formed and refused by a business rule. */
        CONFLICT,
        VALIDATION,
        RATE_LIMITED,
        SERVER,
        /** A response the app could not read: a contract mismatch, not a user mistake. */
        UNEXPECTED,
    }
}

class AppException(val error: AppError) : Exception(error.code ?: error.kind.name)

/** The app error behind a failed [Result], or an UNEXPECTED one for anything else. */
fun Throwable.toAppError(): AppError =
    (this as? AppException)?.error ?: AppError(AppError.Kind.UNEXPECTED)

/**
 * Arabic text for an error, chosen by its code and kind.
 *
 * The backend's own message is not shown: it may be English, and it is written for
 * developers. Codes the app has no phrase for fall back to the phrase for their kind.
 */
object AppErrorText {
    // Codes the backend emits today: `core/exceptions.py` and the ConflictError and
    // DomainError call sites.
    private val byCode = mapOf(
        "AUTHENTICATION_FAILED" to "بيانات الدخول غير صحيحة أو انتهت الجلسة.",
        "AUTHENTICATION_REQUIRED" to "سجّل الدخول للمتابعة.",
        "PERMISSION_DENIED" to "لا تملك الصلاحية لهذا الإجراء.",
        "NOT_FOUND" to "العنصر المطلوب غير موجود.",
        "THROTTLED" to "محاولات كثيرة. حاول بعد قليل.",
        "VALIDATION_ERROR" to "تحقق من البيانات المدخلة.",
        "PROVINCE_REQUIRED" to "اختر المحافظة أولًا.",
        "PHONE_ALREADY_REGISTERED" to
            "هذا الرقم له حساب بالفعل. سجّل الدخول أو استعد كلمة المرور.",
        "DUTY_NOT_SUPPORTED" to "هذا القسم لا يدعم المناوبة.",
        "DUTY_OVERLAP_OR_INVALID" to "وقت المناوبة يتداخل مع مناوبة أخرى أو غير صالح.",
        "HOURS_NOT_SUPPORTED" to "هذا القسم لا يدعم ساعات العمل.",
        "INVALID_HOURS" to "ساعات العمل غير صالحة.",
        "PHOTOS_NOT_SUPPORTED" to "هذا القسم لا يدعم الصور.",
        "TEMPORARY_CLOSURE_NOT_SUPPORTED" to "هذا القسم لا يدعم الإغلاق المؤقت.",
        "EVIDENCE_MAX_FILES" to "تجاوزت العدد المسموح من الملفات لهذا المتطلب.",
        "EVIDENCE_LOCKED_DURING_REVIEW" to "لا يمكن تعديل المستندات أثناء المراجعة.",
        "LAST_OWNER_PROTECTED" to "لا يمكن إزالة المالك الوحيد للمنشأة.",
    )

    fun of(error: AppError): String = error.code?.let(byCode::get) ?: byKind(error.kind)

    fun byKind(kind: AppError.Kind): String = when (kind) {
        AppError.Kind.OFFLINE -> "لا يوجد اتصال بالإنترنت."
        AppError.Kind.UNAUTHENTICATED -> "انتهت الجلسة. سجّل الدخول من جديد."
        AppError.Kind.FORBIDDEN -> "لا تملك الصلاحية لهذا الإجراء."
        AppError.Kind.NOT_FOUND -> "العنصر المطلوب غير موجود."
        AppError.Kind.CONFLICT -> "لا يمكن تنفيذ هذا الإجراء الآن."
        AppError.Kind.VALIDATION -> "تحقق من البيانات المدخلة."
        AppError.Kind.RATE_LIMITED -> "محاولات كثيرة. حاول بعد قليل."
        AppError.Kind.SERVER -> "حدث خطأ في الخادم. حاول لاحقًا."
        AppError.Kind.UNEXPECTED -> "حدث خطأ غير متوقع."
    }
}
