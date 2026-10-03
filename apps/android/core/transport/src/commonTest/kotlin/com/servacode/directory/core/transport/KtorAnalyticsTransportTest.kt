package com.servacode.directory.core.transport

import com.servacode.directory.core.network.api.ApiEnvironment
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Android's `GeneratedAnalyticsTransport`, ported: an anonymous POST that never throws. */
class KtorAnalyticsTransportTest {
    private val backend = FakeBackend()
    private val wiring = Wiring(backend)
    private val transport = KtorAnalyticsTransport(wiring.clients)

    @Test fun `an event goes out anonymously with its pseudonymous id and comes back as a status`() = runTest {
        // Signed in: the token must still not travel with a measurement.
        wiring.accessTokens.set("token-1")
        backend.enqueue(202, """{"accepted":true,"id":"55555555-5555-4555-8555-555555555555"}""")

        val status = transport.send("map_opened", mapOf("source" to "home"), anonymousId = "anon-1")

        assertEquals(202, status)
        val request = backend.request(0)
        assertEquals("POST", request.method)
        assertEquals("/api/v1/analytics/events/", request.url.encodedPath)
        assertEquals("anon-1", request.headers["X-Anonymous-Id"])
        assertNull(request.headers["Authorization"])
        assertEquals("""{"name":"map_opened","properties":{"source":"home"}}""", request.text)
    }

    @Test fun `a refusal is a status and not an exception`() = runTest {
        backend.enqueue(400, """{"code":"VALIDATION_ERROR","message":"x","details":{},"requestId":"r"}""")

        assertEquals(400, transport.send("x", emptyMap(), anonymousId = "anon-1"))
    }

    @Test fun `no answer is null`() = runTest {
        backend.failure = IOException("offline")

        assertNull(transport.send("x", emptyMap(), anonymousId = "anon-1"))
    }

    @Test fun `an accepted answer that does not decode is no answer as with Retrofit`() = runTest {
        backend.enqueue(202, """{"unexpected":true}""")

        assertNull(transport.send("x", emptyMap(), anonymousId = "anon-1"))
    }

    @Test fun `a build without an address is null`() = runTest {
        val unconfigured = KtorAnalyticsTransport(Wiring(backend, environment = ApiEnvironment("")).clients)

        assertNull(unconfigured.send("x", emptyMap(), anonymousId = "anon-1"))
    }
}
