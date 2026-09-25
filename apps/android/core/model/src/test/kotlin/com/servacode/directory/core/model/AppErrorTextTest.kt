package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The sentence a refusal is shown as.
 *
 * The backend's own message is not shown, so every refusal a reader can do something about
 * needs a phrase here. A code with no phrase falls back to the phrase for its kind, which is
 * honest but says nothing: "تحقق من البيانات المدخلة" to someone whose typing was fine is
 * worse than saying nothing, because it sends them back to re-read what they wrote.
 */
class AppErrorTextTest {
    private fun text(code: String?, kind: AppError.Kind) = AppErrorText.of(AppError(kind, code))

    @Test fun `a number that already has an account is told so, and told what to do`() {
        val message = text("PHONE_ALREADY_REGISTERED", AppError.Kind.CONFLICT)

        assertEquals(
            "هذا الرقم له حساب بالفعل. سجّل الدخول أو استعد كلمة المرور.",
            message,
        )
        assertNotEquals(AppErrorText.byKind(AppError.Kind.CONFLICT), message)
        assertNotEquals(AppErrorText.byKind(AppError.Kind.VALIDATION), message)
    }

    @Test fun `a code with no phrase of its own falls back to its kind`() {
        assertEquals(
            AppErrorText.byKind(AppError.Kind.SERVER),
            text("SOMETHING_THE_APP_HAS_NEVER_HEARD_OF", AppError.Kind.SERVER),
        )
        assertEquals(AppErrorText.byKind(AppError.Kind.OFFLINE), text(null, AppError.Kind.OFFLINE))
    }
}
