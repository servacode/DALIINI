package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.models.PlatformStatus
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.MaintenanceEnvelope
import com.servacode.directory.core.network.MaintenanceStatus
import com.servacode.directory.core.network.api.ApiEnvironment
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The probe's half of Android's `MaintenanceTest`, against [KtorMaintenanceProbe]. */
class KtorMaintenanceProbeTest {
    private val backend = FakeBackend()
    private val wiring = Wiring(backend)
    private val state = wiring.maintenance
    private val probe = KtorMaintenanceProbe(wiring.clients, state)

    private val maintenanceBody =
        """{"code":"MAINTENANCE","message":"صيانة مجدولة","details":{"retryAfterSeconds":120},"requestId":"r-1"}"""

    private fun underMaintenance() = state.enter(MaintenanceStatus.Active(message = null, retryAfterSeconds = null))

    @Test fun `the adapters still see a typed server error`() = runTest {
        backend.enqueue(503, maintenanceBody)

        val error = assertFailsWith<AppException> { KtorPublicApi(wiring.clients).provinces() }

        assertEquals(AppError.Kind.SERVER, error.error.kind)
        assertEquals(MaintenanceEnvelope.CODE, error.error.code)
        assertTrue(state.active)
    }

    @Test fun `the probe asks the platform status first and trusts its answer`() = runTest {
        underMaintenance()
        backend.enqueue(200, """{"maintenance":true,"messageAr":"نعود بعد ساعة","retryAfterSeconds":3600}""")

        assertFailsWith<AppException> { probe.probe() }
        // A 200 from the status endpoint does not end maintenance by itself: its body does.
        val active = assertNotNull(state.status.value as? MaintenanceStatus.Active)
        assertEquals("نعود بعد ساعة", active.message)
        assertEquals(3600L, active.retryAfterSeconds)
        val request = backend.request(0)
        assertEquals("/api/v1/platform/status/", request.url.encodedPath)
        assertNull(request.headers["Authorization"])

        backend.enqueue(200, """{"maintenance":false,"messageAr":"","retryAfterSeconds":0}""")
        probe.probe()
        assertEquals(MaintenanceStatus.Normal, state.status.value)
    }

    @Test fun `a backend that answers anything but maintenance is up`() = runTest {
        underMaintenance()
        // A server from before the status endpoint: it answered, so it is not in maintenance.
        backend.enqueue(404, """{"code":"NOT_FOUND","message":"","details":{},"requestId":""}""")

        probe.probe()

        assertEquals(MaintenanceStatus.Normal, state.status.value)
    }

    @Test fun `a server error or no answer leaves the notice up`() = runTest {
        underMaintenance()
        backend.enqueue(500)

        assertFailsWith<AppException> { probe.probe() }
        assertTrue(state.active)

        backend.failure = IOException("offline")
        val offline = assertFailsWith<AppException> { probe.probe() }
        assertEquals(AppError.Kind.OFFLINE, offline.error.kind)
        assertTrue(state.active)
    }

    @Test fun `a zero retry hint is no hint`() {
        val active = assertNotNull(
            KtorMaintenanceProbe.active(PlatformStatus(maintenance = true, messageAr = " ", retryAfterSeconds = 0)),
        )
        assertNull(active.message)
        assertNull(active.retryAfterSeconds)
        val normal = PlatformStatus(maintenance = false, messageAr = "x", retryAfterSeconds = 5)
        assertNull(KtorMaintenanceProbe.active(normal))
    }

    @Test fun `a maintenance answer from the status endpoint keeps the screen up`() = runTest {
        backend.enqueue(503, maintenanceBody, headers = mapOf("Retry-After" to "30"))

        val error = assertFailsWith<AppException> { probe.probe() }

        assertEquals(MaintenanceEnvelope.CODE, error.error.code)
        assertTrue(state.active)
    }

    @Test fun `a build without an address leaves the notice up`() = runTest {
        val unconfigured = Wiring(backend, environment = ApiEnvironment(""))
        unconfigured.maintenance.enter(MaintenanceStatus.Active(message = null, retryAfterSeconds = null))

        val error = assertFailsWith<AppException> {
            KtorMaintenanceProbe(unconfigured.clients, unconfigured.maintenance).probe()
        }

        assertEquals(TransportErrors.CLIENT_NOT_CONFIGURED, error.error.code)
        assertTrue(unconfigured.maintenance.active)
    }
}
