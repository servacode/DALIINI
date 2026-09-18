package com.servacode.directory.connected

import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private const val CODE = "246810"

/** Registration and recovery, each start → code → complete, through the real endpoints. */
class RegistrationConnectedTest {
    @Test fun `a new account is registered in three steps and signs in on completion`() = runBlocking {
        val device = Device()
        val raqqa = device.public.provinces().first { it.nameEn == "Raqqa" }

        val challenge = device.auth.registerStart("مستخدم جديد", Accounts.REGISTRANT_PHONE, raqqa.id)
        setOtp(challenge.id, CODE)
        val wrong = runCatching { device.auth.registerVerify(challenge.id, "000000") }.exceptionOrNull()
        device.auth.registerVerify(challenge.id, CODE)
        assertEquals(SessionState.SIGNED_OUT, device.session.state.value)
        device.session.establish(device.auth.registerComplete(challenge.id, "NewAccount123!"))

        assertEquals(AppError.Kind.VALIDATION, (wrong as AppException).error.kind)
        assertEquals(SessionState.SIGNED_IN, device.session.state.value)
        val profile = device.public.profile()
        assertEquals(Accounts.REGISTRANT_PHONE, profile.phone)
        assertEquals(raqqa.id, profile.provinceId)
    }

    /**
     * On the owner account: a reset revokes every session of the account, and the citizen
     * session other suites share must survive this run.
     */
    @Test fun `a forgotten password is reset in three steps, open sessions end and the new one signs in`() =
        runBlocking {
            val before = Device().signIn(Accounts.OWNER_PHONE, Accounts.OWNER_PASSWORD)
            val device = Device()

            val challenge = device.auth.recoveryStart(Accounts.OWNER_PHONE)
            setOtp(challenge.id, CODE)
            device.auth.recoveryVerify(challenge.id, CODE)
            // The same password again, so the other suites' credentials stay valid.
            device.auth.recoveryReset(challenge.id, Accounts.OWNER_PASSWORD)

            val ended = runCatching { before.public.profile() }.exceptionOrNull() as AppException
            assertEquals(AppError.Kind.UNAUTHENTICATED, ended.error.kind)
            assertEquals(SessionState.SIGNED_OUT, before.session.state.value)
            val signedIn = device.signIn(Accounts.OWNER_PHONE, Accounts.OWNER_PASSWORD)
            assertEquals(Accounts.OWNER_PHONE, signedIn.public.profile().phone)
        }
}
