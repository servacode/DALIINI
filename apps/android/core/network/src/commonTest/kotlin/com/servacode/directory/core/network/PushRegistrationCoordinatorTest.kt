package com.servacode.directory.core.network

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionTokens
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PushRegistrationCoordinatorTest {
    private val calls = mutableListOf<String>()
    private val boundary = object : PushRegistrationBoundary {
        override suspend fun registerAndroidToken(token: String) { calls += "register:$token" }
        override suspend fun deactivateAndroidToken(token: String) { calls += "unregister:$token" }
    }
    private val session = SessionCoordinator(
        object : AccessTokenStore {
            private var value: String? = null
            override fun get() = value
            override fun set(value: String?) { this.value = value }
        },
        object : RefreshTokenVault {
            private var value: String? = null
            override fun read() = value
            override fun write(value: String) { this.value = value }
            override fun clear() { value = null }
        },
        object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens = error("unused")
        },
    )
    private val coordinator = PushRegistrationCoordinator(boundary, session)

    @Test fun `a token that arrives signed out is registered when a session starts`() = runTest {
        coordinator.onTokenAvailable("  token-1  ")
        assertTrue(calls.isEmpty())

        session.establish(SessionTokens("a", "r", "s"))
        coordinator.onSignedIn()

        assertEquals(listOf("register:token-1"), calls)
    }

    @Test fun `a rotated token is registered at once while signed in`() = runTest {
        session.establish(SessionTokens("a", "r", "s"))

        coordinator.onTokenAvailable("token-2")

        assertEquals(listOf("register:token-2"), calls)
    }

    @Test fun `an empty token is refused before it reaches the backend`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            coordinator.onTokenAvailable("   ")
        }
        assertTrue(calls.isEmpty())
    }
}
