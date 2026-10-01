package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The sentence a refusal is shown as.
 *
 * The backend's own message is not shown, so every refusal a reader can do something about needs
 * a message of its own here. A code with none falls back to the one for its kind, which is honest
 * but says nothing: "check what you typed" to someone whose typing was fine is worse than saying
 * nothing, because it sends them back to re-read what they wrote.
 *
 * The words are in the design system's resources; what is asserted here is which of them an error
 * asks for, so improving a wording is not a test failure.
 */
class AppErrorMessagesTest {
    private fun message(code: String?, kind: AppError.Kind) =
        AppErrorMessages.of(AppError(kind, code))

    @Test fun `a number that already has an account is told so, and told what to do`() {
        val message = message("PHONE_ALREADY_REGISTERED", AppError.Kind.CONFLICT)

        assertEquals(AppErrorMessage.PHONE_ALREADY_REGISTERED, message)
        assertNotEquals(AppErrorMessages.byKind(AppError.Kind.CONFLICT), message)
        assertNotEquals(AppErrorMessages.byKind(AppError.Kind.VALIDATION), message)
    }

    @Test fun `a code with no message of its own falls back to its kind`() {
        assertEquals(
            AppErrorMessages.byKind(AppError.Kind.SERVER),
            message("SOMETHING_THE_APP_HAS_NEVER_HEARD_OF", AppError.Kind.SERVER),
        )
        assertEquals(
            AppErrorMessages.byKind(AppError.Kind.OFFLINE),
            message(null, AppError.Kind.OFFLINE),
        )
    }

    @Test fun `every code the backend sends has a message of its own`() {
        // A code that falls through to its kind is a refusal the reader is told nothing useful
        // about, which is the failure this whole table exists to prevent.
        listOf(
            "AUTHENTICATION_FAILED",
            "AUTHENTICATION_REQUIRED",
            "PERMISSION_DENIED",
            "NOT_FOUND",
            "THROTTLED",
            "VALIDATION_ERROR",
            "PROVINCE_REQUIRED",
            "PHONE_ALREADY_REGISTERED",
            "DUTY_NOT_SUPPORTED",
            "DUTY_OVERLAP_OR_INVALID",
            "HOURS_NOT_SUPPORTED",
            "INVALID_HOURS",
            "PHOTOS_NOT_SUPPORTED",
            "TEMPORARY_CLOSURE_NOT_SUPPORTED",
            "EVIDENCE_MAX_FILES",
            "EVIDENCE_LOCKED_DURING_REVIEW",
            "LAST_OWNER_PROTECTED",
        ).forEach { code ->
            assertEquals(code, AppErrorMessage.valueOf(code), message(code, AppError.Kind.SERVER))
        }
    }
}
