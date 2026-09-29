package com.servacode.directory.core.network

import com.servacode.directory.core.observability.Observability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/** Whether the backend is in maintenance, as last reported by one of its answers. */
sealed interface MaintenanceStatus {
    data object Normal : MaintenanceStatus

    /**
     * The backend answered 503 with code `MAINTENANCE`.
     *
     * [message] is the backend's own (Arabic) text, or null to show the app's fallback.
     * [retryAfterSeconds] is the backend's hint, from `details.retryAfterSeconds` or the
     * `Retry-After` header, when it gave one.
     */
    data class Active(
        val message: String?,
        val retryAfterSeconds: Long?,
        val requestId: String? = null,
    ) : MaintenanceStatus
}

/**
 * The process-wide maintenance state.
 *
 * Written only by [MaintenanceInterceptor]: a maintenance answer enters it, any successful
 * answer leaves it. The app shell reads [status] and covers the navigation host while it is
 * [MaintenanceStatus.Active]; screens underneath keep their own state and simply retry.
 */
@Singleton
class MaintenanceState @Inject constructor() {
    private val _status = MutableStateFlow<MaintenanceStatus>(MaintenanceStatus.Normal)
    val status: StateFlow<MaintenanceStatus> = _status.asStateFlow()

    val active: Boolean get() = _status.value is MaintenanceStatus.Active

    fun enter(status: MaintenanceStatus.Active) {
        _status.value = status
    }

    fun clear() {
        _status.update { MaintenanceStatus.Normal }
    }
}

/**
 * Reads maintenance out of every response, on both the anonymous and the signed-in client.
 *
 * It never changes the response and never throws: the error envelope still reaches
 * `ApiErrorMapper`, so a screen that was loading fails its request normally and the shell
 * decides what to show. The body is peeked, not consumed.
 */
class MaintenanceInterceptor @Inject constructor(
    private val state: MaintenanceState,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HTTP_SERVICE_UNAVAILABLE) {
            detect(response)?.let(state::enter)
        } else if (response.isSuccessful && state.active && !isPlatformStatus(response)) {
            // The status endpoint answers 200 during maintenance too; its body decides, and the
            // probe that asked reads it.
            state.clear()
        }
        return response
    }

    private fun isPlatformStatus(response: Response): Boolean =
        response.request.url.encodedPath.endsWith(PLATFORM_STATUS_SUFFIX)

    private fun detect(response: Response): MaintenanceStatus.Active? {
        val body = runCatching { response.peekBody(MAX_ENVELOPE_BYTES).string() }.getOrNull()
        return MaintenanceEnvelope.parse(
            body = body,
            retryAfterHeader = response.header(RETRY_AFTER_HEADER),
            requestIdHeader = response.header(REQUEST_ID_HEADER),
        )
    }

    companion object {
        const val HTTP_SERVICE_UNAVAILABLE = 503
        const val RETRY_AFTER_HEADER = "Retry-After"
        private const val REQUEST_ID_HEADER = "X-Request-ID"
        private const val MAX_ENVELOPE_BYTES = 64L * 1024
        private const val PLATFORM_STATUS_SUFFIX = "/platform/status/"
    }
}

/** Reads the backend's maintenance envelope. Pure, so it is tested without a server. */
object MaintenanceEnvelope {
    /** The envelope `code` the backend uses for maintenance. */
    const val CODE = "MAINTENANCE"

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The maintenance status in a 503 body, or null when the 503 is something else — a proxy
     * page, an overloaded upstream, an envelope with another code.
     */
    fun parse(
        body: String?,
        retryAfterHeader: String?,
        requestIdHeader: String? = null,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): MaintenanceStatus.Active? {
        val envelope = body?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull() }
            ?: return null
        val code = envelope["code"].stringOrNull()
        if (code != CODE) return null
        val details = envelope["details"] as? JsonObject
        val fromDetails = runCatching { details?.get("retryAfterSeconds")?.jsonPrimitive?.longOrNull }
            .getOrNull()
        return MaintenanceStatus.Active(
            message = envelope["message"].stringOrNull()?.takeIf { it.isNotBlank() },
            retryAfterSeconds = (fromDetails ?: retryAfterSeconds(retryAfterHeader, now))
                ?.takeIf { it >= 0 },
            requestId = envelope["requestId"].stringOrNull()?.takeIf { it.isNotBlank() } ?: requestIdHeader,
        )
    }

    /** `Retry-After` is either delay-seconds or an HTTP-date (RFC 9110 §10.2.3). */
    fun retryAfterSeconds(header: String?, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val value = header?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        value.toLongOrNull()?.let { return it.coerceAtLeast(0) }
        return runCatching {
            val at = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
            Duration.between(now, at).seconds.coerceAtLeast(0)
        }.getOrNull()
    }

    private fun kotlinx.serialization.json.JsonElement?.stringOrNull(): String? =
        runCatching { this?.jsonPrimitive?.contentOrNull }.getOrNull()
}

/**
 * How long to wait before asking again: exponential from [baseMillis], capped at
 * [maxMillis], and never sooner than the backend asked for. The backend's hint is itself
 * capped, so a wrong header cannot park the app for hours.
 */
class MaintenanceRetryPolicy(
    private val baseMillis: Long = 5_000L,
    private val maxMillis: Long = 5 * 60_000L,
    private val maxHintMillis: Long = 30 * 60_000L,
) {
    fun delayMillis(attempt: Int, retryAfterSeconds: Long?): Long {
        require(attempt >= 0)
        val backoff = min(maxMillis, baseMillis * (1L shl attempt.coerceAtMost(20)))
        val hint = retryAfterSeconds?.let { min(it * 1_000L, maxHintMillis) } ?: 0L
        return maxOf(backoff, hint)
    }
}

/** A cheap, non-exempt request whose answer tells whether maintenance is over. */
fun interface MaintenanceProbe {
    suspend fun probe()
}

/**
 * Retries while the backend is in maintenance.
 *
 * [retryNow] is the screen's button. [autoRetry] runs for as long as the shell shows the
 * maintenance screen and stops by itself once a probe succeeds. A probe that fails for any
 * reason — still in maintenance, offline — only schedules the next one: nothing here throws
 * back into the UI, and nothing restarts the app.
 */
@Singleton
class MaintenanceCoordinator @Inject constructor(
    private val state: MaintenanceState,
    private val probe: MaintenanceProbe,
    private val observability: Observability,
) {
    private val policy = MaintenanceRetryPolicy()

    /** Returns true when the backend answered normally. */
    suspend fun retryNow(): Boolean {
        try {
            probe.probe()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The interceptor has already recorded a maintenance answer; anything else,
            // such as no network, leaves the state as it was.
            return false
        }
        // A successful answer has cleared the state in the interceptor; clear it here too
        // in case the probe was answered from a path without it.
        state.clear()
        return true
    }

    suspend fun autoRetry() {
        var attempt = 0
        while (true) {
            val current = state.status.value as? MaintenanceStatus.Active ?: return
            delay(policy.delayMillis(attempt, current.retryAfterSeconds))
            if (!state.active) return
            if (retryNow()) return
            attempt++
            if (attempt == LOG_AFTER_ATTEMPTS) {
                observability.recordError(MaintenanceEnvelope.CODE, current.requestId)
            }
        }
    }

    private companion object {
        const val LOG_AFTER_ATTEMPTS = 5
    }
}
