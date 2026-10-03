package com.servacode.directory.core.transport

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.network.MaintenanceEnvelope
import com.servacode.directory.core.network.MaintenanceState
import com.servacode.directory.core.network.api.ApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.save
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The two Ktor clients every adapter here is built on, configured once (DECISION-091): the
 * same rules `:core:network`'s OkHttp clients follow on Android.
 *
 *  - **Every request** carries an `X-Request-ID`, keeping one the caller already set, so a
 *    support report can be matched to the backend's trace.
 *  - **A 503 carrying the maintenance envelope** puts [MaintenanceState] into maintenance; any
 *    other successful answer, except the status endpoint's own, takes it out.
 *  - **The signed-in client** carries the access token in memory, and answers a 401 by asking
 *    the [SessionCoordinator] for a fresh one, once, and sending the request again with it. The
 *    token reported as failed is the one the request carried, so requests that fail together
 *    on one expired token spend the refresh secret once.
 *
 * [session] is a function because the coordinator's refresh gateway is itself built on the
 * anonymous client: the two are made in that order and joined here.
 */
class TransportClients(
    environment: ApiEnvironment,
    engine: HttpClientEngine,
    /** The app the requests come from, for the calls that record it. */
    val platform: ClientPlatform,
    private val accessTokens: AccessTokenStore,
    private val maintenance: MaintenanceState,
    private val session: () -> SessionCoordinator,
) {
    /** The backend's address, checked once; a build without one fails each call, not start-up. */
    val baseUrl: String by lazy { environment.requireConfiguredBaseUrl().trimEnd('/') }

    /** Discovery, sign-in, registration, recovery, refresh: no user token. */
    val anonymous: HttpClient = build(engine, signedIn = false)

    /** Requests made as the signed-in user, with one refresh on 401. */
    val authorized: HttpClient = build(engine, signedIn = true)

    private fun build(engine: HttpClientEngine, signedIn: Boolean): HttpClient {
        val client = HttpClient(engine) {
            // The generated operations read a non-2xx answer themselves; nothing throws on one.
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                // Uploads of up to 10 MiB over a slow mobile link.
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
        }
        client.plugin(HttpSend).intercept { request ->
            if (request.headers[REQUEST_ID_HEADER] == null) request.headers.append(REQUEST_ID_HEADER, newRequestId())
            if (signedIn) accessTokens.get()?.let { request.bearer(it) }
            var call = observeMaintenance(execute(request))
            if (signedIn && call.response.status == HttpStatusCode.Unauthorized) {
                val failed = request.headers[HttpHeaders.Authorization]?.removePrefix(BEARER)
                val refreshed = session().refreshAfterUnauthorized(failed)
                if (refreshed != null) {
                    request.bearer(refreshed)
                    call = observeMaintenance(execute(request))
                }
            }
            call
        }
        return client
    }

    private suspend fun observeMaintenance(call: HttpClientCall): HttpClientCall {
        val response = call.response
        if (response.status == HttpStatusCode.ServiceUnavailable) {
            // Read once and kept, so the adapter can still read the envelope for its error.
            val saved = call.save()
            val body = runCatching { saved.response.bodyAsText() }.getOrNull()
            MaintenanceEnvelope.parse(
                body = body,
                retryAfterHeader = saved.response.headers[HttpHeaders.RetryAfter],
                requestIdHeader = saved.response.headers[REQUEST_ID_HEADER],
            )?.let(maintenance::enter)
            return saved
        }
        if (response.status.isSuccess() && maintenance.active &&
            !response.call.request.url.encodedPath.endsWith(PLATFORM_STATUS_SUFFIX)
        ) {
            // The status endpoint answers 200 during maintenance too; its body decides, and the
            // probe that asked reads it.
            maintenance.clear()
        }
        return call
    }

    private fun HttpRequestBuilder.bearer(token: String) {
        headers.remove(HttpHeaders.Authorization)
        headers.append(HttpHeaders.Authorization, BEARER + token)
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        private const val BEARER = "Bearer "
        private const val PLATFORM_STATUS_SUFFIX = "/platform/status/"
        private const val CONNECT_TIMEOUT_MILLIS = 15_000L
        private const val SOCKET_TIMEOUT_MILLIS = 60_000L

        /**
         * `explicitNulls = false` and `encodeDefaults = false`: generated request classes default
         * every optional field to null, and a PATCH naming one field must not send every other
         * field as null, which the backend reads as "clear this field". The same choice the
         * Android client makes.
         */
        val json: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        }

        @OptIn(ExperimentalUuidApi::class)
        private fun newRequestId(): String = Uuid.random().toString()
    }
}
