package com.servacode.directory.core.network.api

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppErrorText
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ApiErrorMapperTest {
    private val json = "application/json".toMediaType()

    private fun failed(status: Int, body: String, requestIdHeader: String? = null): Response<Unit> {
        val raw = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.example.test/api/v1/x/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("status")
            .apply { if (requestIdHeader != null) header("X-Request-ID", requestIdHeader) }
            .build()
        return Response.error(body.toResponseBody(json), raw)
    }

    @Test fun `reads the envelope, field errors and request id`() {
        val error = ApiErrorMapper.fromResponse(
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
        expected.forEach { (status, kind) -> assertEquals("$status", kind, ApiErrorMapper.kindFor(status)) }
    }

    @Test fun `a body that is not an envelope keeps the status and takes the request id from the header`() {
        val error = ApiErrorMapper.fromResponse(failed(502, "<html>Bad gateway</html>", requestIdHeader = "req-2"))

        assertEquals(AppError.Kind.SERVER, error.kind)
        assertNull(error.code)
        assertEquals("req-2", error.requestId)
    }

    @Test fun `transport failures are offline and contract mismatches are unexpected`() {
        assertEquals(AppError.Kind.OFFLINE, ApiErrorMapper.fromThrowable(IOException("reset")).kind)
        assertEquals(AppError.Kind.OFFLINE, ApiErrorMapper.fromThrowable(SocketTimeoutException()).kind)
        assertEquals(AppError.Kind.UNEXPECTED, ApiErrorMapper.fromThrowable(SerializationException("x")).kind)
    }

    @Test fun `a build without an address fails typed, not with a crash`() {
        val error = ApiErrorMapper.fromThrowable(ApiNotConfiguredException("API base URL is not configured."))

        assertEquals(AppError.Kind.UNEXPECTED, error.kind)
        assertEquals(ApiErrorMapper.CLIENT_NOT_CONFIGURED, error.code)
    }

    @Test fun `text for the user comes from the code, never from the backend message`() {
        val error = AppError(AppError.Kind.CONFLICT, code = "DUTY_NOT_SUPPORTED", message = "Duty is not supported.")

        assertEquals("هذا القسم لا يدعم المناوبة.", AppErrorText.of(error))
        assertEquals(
            AppErrorText.byKind(AppError.Kind.SERVER),
            AppErrorText.of(AppError(AppError.Kind.SERVER, code = "SOMETHING_NEW")),
        )
    }
}
