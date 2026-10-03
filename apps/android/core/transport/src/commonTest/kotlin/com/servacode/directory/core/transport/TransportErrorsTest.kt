package com.servacode.directory.core.transport

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppErrorMessage
import com.servacode.directory.core.model.AppErrorMessages
import com.servacode.directory.core.network.api.ApiNotConfiguredException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The port of Android's `ApiErrorMapperTest`, against [TransportErrors]. */
class TransportErrorsTest {
    private suspend fun failed(status: Int, body: String, requestIdHeader: String? = null): HttpResponse {
        val headers = buildList {
            add(HttpHeaders.ContentType to listOf("application/json"))
            if (requestIdHeader != null) add(TransportClients.REQUEST_ID_HEADER to listOf(requestIdHeader))
        }
        val engine = MockEngine { respond(body, HttpStatusCode.fromValue(status), headersOf(*headers.toTypedArray())) }
        return HttpClient(engine).get("https://api.example.test/api/v1/x/")
    }

    @Test fun `reads the envelope field errors and request id`() = runTest {
        val error = TransportErrors.fromResponse(
            failed(
                400,
                """{"code":"VALIDATION_ERROR","message":"Invalid input.",""" +
                    """"details":{"cursor":["The cursor is not valid."]},"requestId":"req-1"}""",
            ),
        )

        assertEquals(AppError.Kind.VALIDATION, error.kind)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(listOf("The cursor is not valid."), error.fieldErrors["cursor"])
        assertEquals("req-1", error.requestId)
        assertEquals(400, error.status)
    }

    @Test fun `maps every status to its kind`() {
        val expected = mapOf(
            401 to AppError.Kind.UNAUTHENTICATED,
            403 to AppError.Kind.FORBIDDEN,
            404 to AppError.Kind.NOT_FOUND,
            409 to AppError.Kind.CONFLICT,
            429 to AppError.Kind.RATE_LIMITED,
            422 to AppError.Kind.VALIDATION,
            500 to AppError.Kind.SERVER,
            503 to AppError.Kind.SERVER,
        )
        expected.forEach { (status, kind) -> assertEquals(kind, TransportErrors.kindFor(status), "$status") }
    }

    @Test fun `a body that is not an envelope keeps the status and takes the request id from the header`() = runTest {
        val error = TransportErrors.fromResponse(failed(502, "<html>Bad gateway</html>", requestIdHeader = "req-2"))

        assertEquals(AppError.Kind.SERVER, error.kind)
        assertNull(error.code)
        assertEquals("req-2", error.requestId)
        assertEquals(502, error.status)
    }

    @Test fun `an envelope whose details are not field errors keeps its code and drops the details`() = runTest {
        val error = TransportErrors.fromResponse(
            failed(
                503,
                """{"code":"MAINTENANCE","message":"صيانة","details":{"retryAfterSeconds":120},"requestId":"r-1"}""",
            ),
        )

        assertEquals("MAINTENANCE", error.code)
        assertTrue(error.fieldErrors.isEmpty())
        assertEquals("r-1", error.requestId)
    }

    @Test fun `transport failures are offline and contract mismatches are unexpected`() {
        assertEquals(AppError.Kind.OFFLINE, TransportErrors.fromThrowable(IOException("reset")).kind)
        assertEquals(AppError.Kind.OFFLINE, TransportErrors.fromThrowable(SocketTimeoutException("timeout")).kind)
        assertEquals(AppError.Kind.OFFLINE, TransportErrors.fromThrowable(ConnectTimeoutException("timeout")).kind)
        assertEquals(AppError.Kind.UNEXPECTED, TransportErrors.fromThrowable(SerializationException("x")).kind)
    }

    @Test fun `a build without an address fails typed and not with a crash`() {
        val error = TransportErrors.fromThrowable(ApiNotConfiguredException("API base URL is not configured."))

        assertEquals(AppError.Kind.UNEXPECTED, error.kind)
        assertEquals(TransportErrors.CLIENT_NOT_CONFIGURED, error.code)
    }

    @Test fun `text for the user comes from the code and never from the backend message`() {
        val error = AppError(AppError.Kind.CONFLICT, code = "DUTY_NOT_SUPPORTED", message = "Duty is not supported.")

        assertEquals(AppErrorMessage.DUTY_NOT_SUPPORTED, AppErrorMessages.of(error))
        assertEquals(
            AppErrorMessages.byKind(AppError.Kind.SERVER),
            AppErrorMessages.of(AppError(AppError.Kind.SERVER, code = "SOMETHING_NEW")),
        )
    }
}
