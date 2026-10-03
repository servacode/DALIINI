package com.servacode.directory.feature.auth

import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.testing.runMainTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

/** Signing in and recovering an account, shared since DECISION-098, on both platforms. */
class AuthViewModelsTest {
    private val api = RecordingAuthApi()
    private val repository = AuthRepository(
        api,
        SessionCoordinator(MemoryAccess(), MemoryVault(), object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens = error("unused")
        }),
    )

    @Test fun `a sign-in the backend accepts is signed in`() = runMainTest {
        val model = LoginViewModel(repository)

        model.submit("+963900000001", "password")
        assertTrue(model.state.value.busy)
        advanceUntilIdle()

        assertTrue(model.state.value.signedIn)
        assertNull(model.state.value.failure)
    }

    @Test fun `a refused sign-in keeps the backend's code for the form`() = runMainTest {
        api.loginFails = true
        val model = LoginViewModel(repository)

        model.submit("+963900000001", "wrong")
        advanceUntilIdle()

        assertFalse(model.state.value.signedIn)
        assertEquals("AUTHENTICATION_FAILED", model.state.value.failure?.code)
    }

    @Test fun `recovery goes from the phone to the code to a new password`() = runMainTest {
        val model = RecoveryViewModel(repository)

        model.start("+963900000001")
        advanceUntilIdle()
        assertEquals(ChallengeStep.CODE, model.state.value.step)
        model.verify("123456")
        advanceUntilIdle()
        assertEquals(ChallengeStep.PASSWORD, model.state.value.step)
        model.reset("a new password")
        advanceUntilIdle()

        assertEquals(ChallengeStep.DONE, model.state.value.step)
    }
}
