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
 * Which sentence an error deserves, named without saying it.
 *
 * The backend's own message is never shown: it may be English, and it is written for developers.
 * What is shown is one of these, chosen from the code the backend sent, or from the kind of
 * failure where the code is one this app has never heard of.
 *
 * The words themselves are in the design system's `strings.xml`, and `appErrorText` reads them.
 * They are not here because this module is compiled without the Android framework — it is the
 * part of the app a test can run in a second — and because a sentence that depends on the
 * reader's language is not a constant. What is here is the decision, which is the same in every
 * language and which the harness tests.
 */
enum class AppErrorMessage {
    AUTHENTICATION_FAILED,
    AUTHENTICATION_REQUIRED,
    PERMISSION_DENIED,
    NOT_FOUND,
    THROTTLED,
    VALIDATION_ERROR,
    PROVINCE_REQUIRED,
    PHONE_ALREADY_REGISTERED,
    DUTY_NOT_SUPPORTED,
    DUTY_OVERLAP_OR_INVALID,
    DUTY_DURING_CLOSURE,
    HOURS_NOT_SUPPORTED,
    INVALID_HOURS,
    PHOTOS_NOT_SUPPORTED,
    TEMPORARY_CLOSURE_NOT_SUPPORTED,
    EVIDENCE_MAX_FILES,
    EVIDENCE_LOCKED_DURING_REVIEW,
    LAST_OWNER_PROTECTED,
    MAINTENANCE,

    /** The kinds that have no code of their own worth naming. */
    OFFLINE,
    SESSION_EXPIRED,
    CONFLICT,
    SERVER,
    UNEXPECTED,
}

object AppErrorMessages {
    // Codes the backend emits today: `core/exceptions.py` and the ConflictError and
    // DomainError call sites.
    private val byCode = mapOf(
        "AUTHENTICATION_FAILED" to AppErrorMessage.AUTHENTICATION_FAILED,
        "AUTHENTICATION_REQUIRED" to AppErrorMessage.AUTHENTICATION_REQUIRED,
        "PERMISSION_DENIED" to AppErrorMessage.PERMISSION_DENIED,
        "NOT_FOUND" to AppErrorMessage.NOT_FOUND,
        "THROTTLED" to AppErrorMessage.THROTTLED,
        "VALIDATION_ERROR" to AppErrorMessage.VALIDATION_ERROR,
        "PROVINCE_REQUIRED" to AppErrorMessage.PROVINCE_REQUIRED,
        "PHONE_ALREADY_REGISTERED" to AppErrorMessage.PHONE_ALREADY_REGISTERED,
        "DUTY_NOT_SUPPORTED" to AppErrorMessage.DUTY_NOT_SUPPORTED,
        "DUTY_OVERLAP_OR_INVALID" to AppErrorMessage.DUTY_OVERLAP_OR_INVALID,
        "DUTY_DURING_CLOSURE" to AppErrorMessage.DUTY_DURING_CLOSURE,
        "HOURS_NOT_SUPPORTED" to AppErrorMessage.HOURS_NOT_SUPPORTED,
        "INVALID_HOURS" to AppErrorMessage.INVALID_HOURS,
        "PHOTOS_NOT_SUPPORTED" to AppErrorMessage.PHOTOS_NOT_SUPPORTED,
        "TEMPORARY_CLOSURE_NOT_SUPPORTED" to AppErrorMessage.TEMPORARY_CLOSURE_NOT_SUPPORTED,
        "EVIDENCE_MAX_FILES" to AppErrorMessage.EVIDENCE_MAX_FILES,
        "EVIDENCE_LOCKED_DURING_REVIEW" to AppErrorMessage.EVIDENCE_LOCKED_DURING_REVIEW,
        "LAST_OWNER_PROTECTED" to AppErrorMessage.LAST_OWNER_PROTECTED,
        "MAINTENANCE" to AppErrorMessage.MAINTENANCE,
    )

    fun of(error: AppError): AppErrorMessage = error.code?.let(byCode::get) ?: byKind(error.kind)

    /**
     * What a kind of failure says on its own.
     *
     * Several kinds share a message with a code — a forbidden request and `PERMISSION_DENIED` are
     * the same thing said twice — and they answer with the same member rather than with a second
     * sentence that has to be translated twice and kept in step.
     */
    fun byKind(kind: AppError.Kind): AppErrorMessage = when (kind) {
        AppError.Kind.OFFLINE -> AppErrorMessage.OFFLINE
        AppError.Kind.UNAUTHENTICATED -> AppErrorMessage.SESSION_EXPIRED
        AppError.Kind.FORBIDDEN -> AppErrorMessage.PERMISSION_DENIED
        AppError.Kind.NOT_FOUND -> AppErrorMessage.NOT_FOUND
        AppError.Kind.CONFLICT -> AppErrorMessage.CONFLICT
        AppError.Kind.VALIDATION -> AppErrorMessage.VALIDATION_ERROR
        AppError.Kind.RATE_LIMITED -> AppErrorMessage.THROTTLED
        AppError.Kind.SERVER -> AppErrorMessage.SERVER
        AppError.Kind.UNEXPECTED -> AppErrorMessage.UNEXPECTED
    }
}
