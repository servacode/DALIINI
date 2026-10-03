package com.servacode.directory.core.transport

import com.servacode.directory.core.model.AppException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The iPhone app is recorded as itself (DECISION-091): a session, a push token and the release
 * it is told to update to all carry the platform the transport was built for.
 */
class ClientPlatformTest {
    private val backend = FakeBackend()
    private val wiring = Wiring(backend, platform = ClientPlatform.IOS)

    @Test fun `a sign-in from the iPhone is recorded as IOS`() = runTest {
        backend.enqueue(400, """{"code":"VALIDATION_ERROR","message":"x","details":{},"requestId":"r"}""")

        assertFailsWith<AppException> { KtorAuthApi(wiring.clients, "iPhone").login("+963900000001", "secret") }

        val body = backend.request(0).text
        assertTrue(""""platform":"IOS"""" in body, body)
        assertTrue(""""deviceName":"iPhone"""" in body, body)
    }

    @Test fun `the iPhone's push token is registered as IOS`() = runTest {
        backend.enqueue(204)

        KtorPushRegistration(wiring.clients).registerAndroidToken("apns-token-1")

        assertEquals("""{"platform":"IOS","token":"apns-token-1"}""", backend.request(0).text)
    }

    @Test fun `the iPhone asks for the iPhone's release`() = runTest {
        backend.enqueue(404, """{"code":"NOT_FOUND","message":"x","details":{},"requestId":"r"}""")

        assertFailsWith<AppException> { KtorPublicApi(wiring.clients).appRelease() }

        assertEquals("IOS", backend.request(0).url.parameters["platform"])
    }
}
