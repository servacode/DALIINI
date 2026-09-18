package com.servacode.directory.core.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class SessionCoordinatorTest {
    private val material = RefreshMaterial(sessionId = "session-1", refreshToken = "refresh-1").encode()

    @Test fun refreshIsCoordinatedAcrossConcurrent401s() = runTest {
        val access = FakeAccess("old")
        val vault = FakeVault(material)
        val calls = AtomicInteger()
        val gateway = object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens {
                calls.incrementAndGet()
                assertEquals("refresh-1", refreshToken)
                return SessionTokens("new", "refresh-2", "session-1")
            }
        }
        val coordinator = SessionCoordinator(access, vault, gateway)

        List(8) { async { coordinator.refreshAfterUnauthorized("old") } }.awaitAll()

        assertEquals(1, calls.get())
        assertEquals("new", access.get())
        assertEquals(RefreshMaterial("session-1", "refresh-2"), RefreshMaterial.decode(vault.read()!!))
    }

    @Test fun aRejectedRefreshEndsTheSession() = runTest {
        val access = FakeAccess("old")
        val vault = FakeVault(material)
        val gateway = object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens = throw RefreshRejectedException()
        }
        val coordinator = SessionCoordinator(access, vault, gateway)
        assertEquals(SessionState.SIGNED_IN, coordinator.state.value)

        assertNull(coordinator.refreshAfterUnauthorized("old"))

        assertNull(access.get())
        assertNull(vault.read())
        assertEquals(SessionState.SIGNED_OUT, coordinator.state.value)
    }

    @Test fun aTransientFailureKeepsTheSession() = runTest {
        val vault = FakeVault(material)
        val gateway = object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens = throw IOException("offline")
        }
        val coordinator = SessionCoordinator(FakeAccess("old"), vault, gateway)

        assertNull(coordinator.refreshAfterUnauthorized("old"))

        assertEquals(material, vault.read())
        assertEquals(SessionState.SIGNED_IN, coordinator.state.value)
    }

    @Test fun establishStoresTheSessionIdWithTheSecretAndNeverTheAccessToken() {
        val access = FakeAccess(null)
        val vault = FakeVault(null)
        val coordinator = SessionCoordinator(access, vault, object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens = error("unused")
        })
        assertEquals(SessionState.SIGNED_OUT, coordinator.state.value)

        coordinator.establish(SessionTokens("access", "refresh", "session-9"))

        assertEquals("session-9", coordinator.sessionId())
        assertEquals("access", access.get())
        assertEquals(false, vault.read()!!.contains("access"))
        assertEquals(SessionState.SIGNED_IN, coordinator.state.value)
    }

    @Test fun unreadableMaterialIsNoSession() {
        assertNull(RefreshMaterial.decode("refresh-only"))
        assertNull(RefreshMaterial.decode("v2\nsession\nsecret"))
        assertNull(RefreshMaterial.decode("v1\n\nsecret"))
    }
}

private class FakeAccess(value: String?) : AccessTokenStore {
    private var value = value
    override fun get() = value
    override fun set(value: String?) { this.value = value }
}

private class FakeVault(value: String?) : RefreshTokenVault {
    private var value = value
    override fun read() = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}
