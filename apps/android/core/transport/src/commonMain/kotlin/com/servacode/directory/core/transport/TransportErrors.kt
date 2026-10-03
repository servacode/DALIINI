package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.infrastructure.HttpResponse
import com.servacode.directory.api.multiplatform.models.ApiError
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.network.api.ApiNotConfiguredException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The one place a transport failure becomes an [AppError], as `ApiErrorMapper` is on Android
 * (DECISION-091), with the same answers: a failed response is read as the backend envelope
 * `{code, message, details, requestId}`; when the body is not one — a proxy's page, an empty
 * 502 — the status alone decides the kind and the request id comes from the header.
 */
object TransportErrors {
    private val envelopeJson = Json { ignoreUnknownKeys = true }

    suspend fun fromResponse(response: io.ktor.client.statement.HttpResponse): AppError {
        val status = response.status.value
        val envelope = runCatching { response.bodyAsText() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { body ->
                runCatching { envelopeJson.decodeFromString(ApiError.serializer(), body) }.getOrNull()
                    ?: lenientEnvelope(body)
            }
        return AppError(
            kind = kindFor(status),
            code = envelope?.code,
            message = envelope?.message,
            fieldErrors = envelope?.details.orEmpty(),
            requestId = envelope?.requestId?.takeIf { it.isNotBlank() }
                ?: response.headers[TransportClients.REQUEST_ID_HEADER],
            status = status,
        )
    }

    /**
     * The envelope when its `details` is not the field-error map the contract declares —
     * maintenance sends `{"retryAfterSeconds": n}` there. Code, message and request id are
     * still the backend's; the details are dropped rather than misread as field errors.
     */
    private fun lenientEnvelope(body: String): ApiError? = runCatching {
        val root = envelopeJson.parseToJsonElement(body) as? JsonObject ?: return null
        fun text(key: String) = (root[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ApiError(
            code = text("code") ?: return null,
            message = text("message").orEmpty(),
            details = emptyMap(),
            requestId = text("requestId").orEmpty(),
        )
    }.getOrNull()

    fun fromThrowable(error: Throwable): AppError = when (error) {
        is AppException -> error.error
        is ApiNotConfiguredException -> AppError(AppError.Kind.UNEXPECTED, code = CLIENT_NOT_CONFIGURED)
        // Ktor reports every transport failure — no route, DNS, TLS, a timeout — as an
        // IOException, on the JVM and on Darwin alike.
        is IOException -> AppError(AppError.Kind.OFFLINE)
        // The response arrived but does not match the contract the client was generated from.
        is SerializationException, is IllegalArgumentException -> AppError(AppError.Kind.UNEXPECTED)
        else -> AppError(AppError.Kind.UNEXPECTED)
    }

    fun kindFor(status: Int): AppError.Kind = when {
        status == 401 -> AppError.Kind.UNAUTHENTICATED
        status == 403 -> AppError.Kind.FORBIDDEN
        status == 404 -> AppError.Kind.NOT_FOUND
        status == 409 -> AppError.Kind.CONFLICT
        status == 429 -> AppError.Kind.RATE_LIMITED
        status in 400..499 -> AppError.Kind.VALIDATION
        status >= 500 -> AppError.Kind.SERVER
        else -> AppError.Kind.UNEXPECTED
    }

    /** The build carries no usable backend address. Not a backend code. */
    const val CLIENT_NOT_CONFIGURED = "CLIENT_NOT_CONFIGURED"
}

/**
 * Runs one generated operation and returns its body, or throws [AppException]. Every adapter
 * call goes through here, so no screen ever sees a Ktor response, an IOException or a
 * serialization error.
 */
internal suspend fun <T : Any> call(operation: suspend () -> HttpResponse<T>): T {
    val response = send(operation)
    return try {
        response.body()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        // A 2xx whose body cannot be read: unexpected, with the status kept, as Android keeps it.
        throw AppException(TransportErrors.fromThrowable(failure).copy(status = response.status))
    }
}

/** As [call], for operations answered with 204 and no body. */
internal suspend fun callForNoContent(operation: suspend () -> HttpResponse<Unit>) {
    send(operation)
}

private suspend fun <T : Any> send(operation: suspend () -> HttpResponse<T>): HttpResponse<T> {
    val response = try {
        operation()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw AppException(TransportErrors.fromThrowable(failure))
    }
    if (!response.success) throw AppException(TransportErrors.fromResponse(response.response))
    return response
}
