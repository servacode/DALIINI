package com.servacode.directory.feature.auth

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.network.AuthApiBoundary
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class AuthRepositoryTest {
    private val access = MemoryAccess()
    private val vault = MemoryVault()
    private val api = RecordingAuthApi()
    private val session = SessionCoordinator(access, vault, object : RefreshGateway {
        override suspend fun rotate(refreshToken: String): SessionTokens = error("unused")
    })
    private val repository = AuthRepository(api, session)

    @Test fun `signing in starts a session with the id needed to end it`() = runTest {
        repository.login(" +963900000001 ", "password").getOrThrow()

        assertEquals(SessionState.SIGNED_IN, repository.state.value)
        assertEquals("session-1", session.sessionId())
        assertEquals("access-1", access.get())
        assertEquals(listOf("login:+963900000001"), api.calls)
    }

    @Test fun `a refused sign-in leaves no session`() = runTest {
        api.loginFails = true

        val error = repository.login("+963900000001", "wrong").exceptionOrNull() as AppException

        assertEquals("AUTHENTICATION_FAILED", error.error.code)
        assertEquals(SessionState.SIGNED_OUT, repository.state.value)
        assertNull(vault.read())
    }

    @Test fun `signing out revokes this session and clears the device`() = runTest {
        repository.login("+963900000001", "password")

        repository.logout()

        assertEquals("logout:session-1", api.calls.last())
        assertNull(access.get())
        assertNull(vault.read())
        assertEquals(SessionState.SIGNED_OUT, repository.state.value)
    }

    @Test fun `signing out offline still clears the device`() = runTest {
        repository.login("+963900000001", "password")
        api.logoutFails = true

        repository.logout()

        assertNull(access.get())
        assertNull(vault.read())
        assertEquals(SessionState.SIGNED_OUT, repository.state.value)
    }

    @Test fun `registration is start then verify then complete and only completion signs in`() = runTest {
        // The number goes first and alone: nothing about the person is sent until the code has
        // been proved, so the name travels with the password rather than with the request.
        val challenge = repository.startRegistration("+963900000009", "raqqa").getOrThrow()
        repository.verifyRegistration(challenge.id, " 123456 ").getOrThrow()
        assertEquals(SessionState.SIGNED_OUT, repository.state.value)

        repository.completeRegistration(challenge.id, " مالك ", "StrongPass123!").getOrThrow()

        assertEquals(SessionState.SIGNED_IN, repository.state.value)
        assertTrue("start:+963900000009:raqqa" in api.calls)
        assertTrue("complete:challenge-1:مالك" in api.calls)
    }
}

internal class RecordingAuthApi : AuthApiBoundary {
    val calls = mutableListOf<String>()
    var loginFails = false
    var logoutFails = false
    private val tokens = SessionTokens("access-1", "refresh-1", "session-1")

    override suspend fun login(phone: String, password: String): SessionTokens {
        calls += "login:$phone"
        if (loginFails) throw AppException(AppError(AppError.Kind.UNAUTHENTICATED, code = "AUTHENTICATION_FAILED"))
        return tokens
    }

    override suspend fun registerStart(phone: String, provinceId: String): AuthChallenge {
        calls += "start:$phone:$provinceId"
        return AuthChallenge("challenge-1", 0)
    }

    override suspend fun registerVerify(challengeId: String, code: String) {
        calls += "verify:$challengeId:$code"
    }

    override suspend fun registerComplete(
        challengeId: String,
        displayName: String,
        password: String,
    ): SessionTokens {
        calls += "complete:$challengeId:$displayName"
        return tokens
    }

    override suspend fun recoveryStart(phone: String): AuthChallenge = AuthChallenge("challenge-2", 0)
    override suspend fun recoveryVerify(challengeId: String, code: String) = Unit
    override suspend fun recoveryReset(challengeId: String, password: String) = Unit

    override suspend fun logout(sessionId: String) {
        calls += "logout:$sessionId"
        if (logoutFails) throw AppException(AppError(AppError.Kind.OFFLINE))
    }

    override suspend fun sessions(): List<AccountSession> = emptyList()
    override suspend fun revokeSession(sessionId: String) = Unit
}

internal class MemoryAccess : AccessTokenStore {
    private var value: String? = null
    override fun get() = value
    override fun set(value: String?) { this.value = value }
}

internal class MemoryVault : RefreshTokenVault {
    private var value: String? = null
    override fun read() = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}
