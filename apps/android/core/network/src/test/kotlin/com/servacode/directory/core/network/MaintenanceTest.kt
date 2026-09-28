package com.servacode.directory.core.network

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.api.ApiEnvironment
import com.servacode.directory.core.network.api.GeneratedClient
import com.servacode.directory.core.network.api.GeneratedMaintenanceProbe
import com.servacode.directory.core.observability.NoOpObservability
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class MaintenanceTest {
    private val server = MockWebServer()
    private val state = MaintenanceState()
    private lateinit var http: OkHttpClient

    @Before fun start() {
        server.start()
        http = OkHttpClient.Builder().addInterceptor(MaintenanceInterceptor(state)).build()
    }

    @After fun stop() = server.close()

    private fun respond(code: Int, body: String = "", retryAfter: String? = null) {
        val builder = MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body)
        if (retryAfter != null) builder.addHeader("Retry-After", retryAfter)
        server.enqueue(builder.build())
    }

    private fun get(): String = http.newCall(Request.Builder().url(server.url("/api/v1/public/provinces/")).build())
        .execute().use { it.body.string() }

    private val maintenanceBody =
        """{"code":"MAINTENANCE","message":"صيانة مجدولة","details":{"retryAfterSeconds":120},"requestId":"r-1"}"""

    @Test fun `a maintenance envelope enters maintenance and leaves the body readable`() {
        respond(503, maintenanceBody, retryAfter = "60")

        val body = get()

        assertEquals(maintenanceBody, body)
        val active = state.status.value as MaintenanceStatus.Active
        assertEquals("صيانة مجدولة", active.message)
        // The envelope's own hint wins over the header.
        assertEquals(120L, active.retryAfterSeconds)
        assertEquals("r-1", active.requestId)
    }

    @Test fun `the Retry-After header is used when the envelope has no hint`() {
        respond(503, """{"code":"MAINTENANCE","message":"","details":{},"requestId":""}""", retryAfter = "45")

        get()

        val active = state.status.value as MaintenanceStatus.Active
        assertNull(active.message)
        assertEquals(45L, active.retryAfterSeconds)
    }

    @Test fun `a 503 that is not maintenance changes nothing`() {
        respond(503, "<html>Bad gateway</html>")
        get()
        respond(503, """{"code":"SERVER_ERROR","message":"x","details":{},"requestId":"r"}""")
        get()

        assertEquals(MaintenanceStatus.Normal, state.status.value)
    }

    @Test fun `a successful answer ends maintenance, an error does not`() {
        respond(503, maintenanceBody)
        get()
        respond(404, """{"code":"NOT_FOUND","message":"","details":{},"requestId":""}""")
        get()
        assertTrue(state.active)

        respond(200, """{"items":[]}""")
        get()

        assertEquals(MaintenanceStatus.Normal, state.status.value)
    }

    private fun probe(): GeneratedMaintenanceProbe {
        val environment = ApiEnvironment(server.url("/").toString(), allowCleartext = true)
        return GeneratedMaintenanceProbe(environment, http, GeneratedClient(environment, http), state)
    }

    @Test fun `the adapters still see a typed server error`() = runTest {
        val client = GeneratedClient(ApiEnvironment(server.url("/").toString(), allowCleartext = true), http)
        respond(503, maintenanceBody)

        val error = runCatching {
            com.servacode.directory.core.network.api.GeneratedPublicApi(client, client).provinces()
        }.exceptionOrNull() as AppException

        assertEquals(AppError.Kind.SERVER, error.error.kind)
        assertEquals(MaintenanceEnvelope.CODE, error.error.code)
        assertTrue(state.active)
    }

    @Test fun `the probe asks the platform status first and trusts its answer`() = runTest {
        state.enter(MaintenanceStatus.Active(message = null, retryAfterSeconds = null))
        respond(200, """{"maintenance":true,"messageAr":"نعود بعد ساعة","retryAfterSeconds":3600}""")

        assertTrue(runCatching { probe().probe() }.isFailure)
        // A 200 from the status endpoint does not end maintenance by itself: its body does.
        val active = state.status.value as MaintenanceStatus.Active
        assertEquals("نعود بعد ساعة", active.message)
        assertEquals(3600L, active.retryAfterSeconds)
        assertEquals("/api/v1/platform/status/", server.takeRequest().url.encodedPath)

        respond(200, """{"maintenance":false,"messageAr":null,"retryAfterSeconds":null}""")
        probe().probe()
        assertEquals(MaintenanceStatus.Normal, state.status.value)
    }

    @Test fun `a backend without the status endpoint is probed through the provinces`() = runTest {
        state.enter(MaintenanceStatus.Active(message = null, retryAfterSeconds = null))
        respond(404, """{"code":"NOT_FOUND","message":"","details":{},"requestId":""}""")
        respond(200, """{"items":[]}""")

        probe().probe()

        assertEquals(MaintenanceStatus.Normal, state.status.value)
        assertEquals("/api/v1/platform/status/", server.takeRequest().url.encodedPath)
        assertEquals("/api/v1/public/provinces/", server.takeRequest().url.encodedPath)
    }

    @Test fun `a maintenance answer from the status endpoint keeps the screen up`() = runTest {
        respond(503, maintenanceBody, retryAfter = "30")

        val error = runCatching { probe().probe() }.exceptionOrNull() as AppException

        assertEquals(MaintenanceEnvelope.CODE, error.error.code)
        assertTrue(state.active)
    }

    @Test fun `Retry-After accepts seconds and HTTP dates and ignores garbage`() {
        val now = ZonedDateTime.of(2026, 9, 28, 10, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(30L, MaintenanceEnvelope.retryAfterSeconds("30", now))
        assertEquals(90L, MaintenanceEnvelope.retryAfterSeconds("Mon, 28 Sep 2026 10:01:30 GMT", now))
        assertEquals(0L, MaintenanceEnvelope.retryAfterSeconds("Mon, 28 Sep 2026 09:00:00 GMT", now))
        assertNull(MaintenanceEnvelope.retryAfterSeconds("soon", now))
        assertNull(MaintenanceEnvelope.retryAfterSeconds(null, now))
    }

    @Test fun `a malformed envelope is not maintenance and does not throw`() {
        assertNull(MaintenanceEnvelope.parse("{", null))
        assertNull(MaintenanceEnvelope.parse("[]", null))
        assertNull(MaintenanceEnvelope.parse("""{"code":42}""", null))
        assertNull(MaintenanceEnvelope.parse(null, "30"))
    }

    @Test fun `backoff doubles, is capped, and never undercuts the backend's hint`() {
        val policy = MaintenanceRetryPolicy(baseMillis = 5_000, maxMillis = 300_000, maxHintMillis = 1_800_000)
        assertEquals(5_000L, policy.delayMillis(0, null))
        assertEquals(10_000L, policy.delayMillis(1, null))
        assertEquals(300_000L, policy.delayMillis(30, null))
        assertEquals(120_000L, policy.delayMillis(0, 120))
        assertEquals(1_800_000L, policy.delayMillis(0, 86_400))
    }

    @Test fun `auto retry waits, probes, and stops once the backend answers`() = runTest {
        state.enter(MaintenanceStatus.Active(message = null, retryAfterSeconds = 20))
        var probes = 0
        val coordinator = MaintenanceCoordinator(
            state,
            MaintenanceProbe {
                probes++
                if (probes < 2) throw AppException(AppError(AppError.Kind.SERVER, code = MaintenanceEnvelope.CODE))
            },
            NoOpObservability,
        )

        val job = launch { coordinator.autoRetry() }
        runCurrent()
        advanceTimeBy(19_999)
        assertEquals(0, probes)
        advanceTimeBy(2)
        assertEquals(1, probes)
        assertTrue(state.active)
        // Second wait: max(10 s backoff, 20 s hint).
        advanceTimeBy(20_001)
        assertEquals(2, probes)
        assertFalse(state.active)
        assertTrue(job.isCompleted)
    }

    @Test fun `retry now reports failure without throwing`() = runTest {
        state.enter(MaintenanceStatus.Active(message = null, retryAfterSeconds = null))
        val coordinator = MaintenanceCoordinator(
            state,
            MaintenanceProbe { throw java.io.IOException("offline") },
            NoOpObservability,
        )

        assertFalse(coordinator.retryNow())
        assertTrue(state.active)
    }
}
