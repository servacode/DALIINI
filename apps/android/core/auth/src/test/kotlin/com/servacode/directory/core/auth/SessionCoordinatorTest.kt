package com.servacode.directory.core.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SessionCoordinatorTest {
    @Test fun refreshIsCoordinatedAcrossConcurrent401s() = runTest {
        val access = FakeAccess("old")
        val vault = FakeVault("refresh-1")
        val calls = AtomicInteger()
        val gateway = object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens {
                calls.incrementAndGet()
                return SessionTokens("new", "refresh-2")
            }
        }
        val coordinator = SessionCoordinator(access, vault, gateway)
        List(8) { async { coordinator.refreshAfterUnauthorized("old") } }.awaitAll()
        assertEquals(1, calls.get())
        assertEquals("new", access.get())
        assertEquals("refresh-2", vault.read())
    }

    @Test fun failedRefreshClearsSession() = runTest {
        val access = FakeAccess("old")
        val vault = FakeVault("refresh")
        val gateway = object : RefreshGateway {
            override suspend fun rotate(refreshToken: String): SessionTokens = error("rejected")
        }
        val coordinator = SessionCoordinator(access, vault, gateway)
        assertNull(coordinator.refreshAfterUnauthorized("old"))
        assertNull(access.get())
        assertNull(vault.read())
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
