package com.servacode.directory.core.transport

import com.servacode.directory.core.auth.MemoryAccessTokenStore
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.MaintenanceState
import com.servacode.directory.core.network.api.ApiEnvironment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The transport on the iPhone's real engine, against a server that is not there. */
class DarwinEngineTest {
    @Test
    fun `a server that cannot be reached is offline`() = runBlocking {
        val accessTokens = MemoryAccessTokenStore()
        lateinit var session: SessionCoordinator
        // Port 9 on the loopback: nothing listens there, so the connection is refused at once.
        val clients = TransportClients(
            ApiEnvironment("http://127.0.0.1:9/api/v1/", allowCleartext = true),
            darwinEngine(),
            ClientPlatform.IOS,
            accessTokens,
            MaintenanceState(),
        ) { session }
        session = SessionCoordinator(accessTokens, MemoryVault(), KtorRefreshGateway(clients))

        val failure = assertFailsWith<AppException> { KtorPublicApi(clients).provinces() }

        assertEquals(AppError.Kind.OFFLINE, failure.error.kind)
    }
}
