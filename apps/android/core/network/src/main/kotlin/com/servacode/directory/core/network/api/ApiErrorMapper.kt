package com.servacode.directory.core.network.api

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

/**
 * The one place a transport failure becomes an [AppError].
 *
 * A failed response is read as the backend envelope `{code, message, details, requestId}`.
 * When the body is not an envelope — a proxy's HTML page, an empty 502 — the status alone
 * decides the kind and the request id comes from the `X-Request-ID` header.
 */
object ApiErrorMapper {
    // The envelope has no contextual fields, so a plain decoder reads it; this keeps the
    // mapper independent of when the generated serializer gets configured.
    private val envelopeJson = Json { ignoreUnknownKeys = true }

    fun fromResponse(response: Response<*>): AppError {
        val status = response.code()
        val envelope = runCatching { response.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { body ->
                runCatching {
                    envelopeJson.decodeFromString(ApiError.serializer(), body)
                }.getOrNull()
            }
        return AppError(
            kind = kindFor(status),
            code = envelope?.code,
            message = envelope?.message,
            fieldErrors = envelope?.details.orEmpty(),
            requestId = envelope?.requestId?.takeIf { it.isNotBlank() }
                ?: response.headers()[REQUEST_ID_HEADER],
            status = status,
        )
    }

    fun fromThrowable(error: Throwable): AppError = when (error) {
        is AppException -> error.error
        is ApiNotConfiguredException -> AppError(AppError.Kind.UNEXPECTED, code = CLIENT_NOT_CONFIGURED)
        // OkHttp reports every transport failure — no route, DNS, TLS, timeout — as IOException.
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

    const val REQUEST_ID_HEADER = "X-Request-ID"

    /** The build carries no usable backend address. Not a backend code. */
    const val CLIENT_NOT_CONFIGURED = "CLIENT_NOT_CONFIGURED"
}

/**
 * Runs one generated operation and returns its body, or throws [AppException].
 *
 * Every adapter call goes through here, so no screen ever sees a Retrofit `Response`, an
 * `IOException` or a serialization error.
 */
internal suspend fun <T : Any> call(operation: suspend () -> Response<T>): T {
    val response = send(operation)
    return response.body() ?: throw AppException(AppError(AppError.Kind.UNEXPECTED, status = response.code()))
}

/** As [call], for operations answered with 204 and no body. */
internal suspend fun callForNoContent(operation: suspend () -> Response<Unit>) {
    send(operation)
}

private suspend fun <T> send(operation: suspend () -> Response<T>): Response<T> {
    val response = try {
        operation()
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw AppException(ApiErrorMapper.fromThrowable(failure))
    }
    if (!response.isSuccessful) throw AppException(ApiErrorMapper.fromResponse(response))
    return response
}
