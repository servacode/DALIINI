package com.servacode.directory.core.transport

import com.servacode.directory.api.multiplatform.apis.AnalyticsApi
import com.servacode.directory.api.multiplatform.models.AnalyticsEventRequest
import com.servacode.directory.core.analytics.AnalyticsTransport
import kotlinx.serialization.json.JsonPrimitive

/**
 * Posts one product event to the backend's own analytics endpoint: the port of Android's
 * `GeneratedAnalyticsTransport`.
 *
 * It uses the anonymous client, not the authorized one. A measurement must not be attached to an
 * account: the server gets a pseudonymous id in a header and nothing else, and sending the access
 * token with it would turn "how many people opened the map" into "which person opened the map".
 *
 * It never throws. The contract its caller relies on is that a failure comes back as a status or
 * a null, so a dead socket is a value here rather than an exception travelling up into a screen.
 */
class KtorAnalyticsTransport(clients: TransportClients) : AnalyticsTransport {
    private val api by lazy { AnalyticsApi(clients.baseUrl, clients.anonymous) }

    override suspend fun send(
        name: String,
        properties: Map<String, String>,
        anonymousId: String,
    ): Int? = runCatching {
        val response = api.analyticsEventCreate(
            analyticsEventRequest = AnalyticsEventRequest(
                name = name,
                properties = properties.mapValues { (_, value) -> JsonPrimitive(value) },
            ),
            xAnonymousId = anonymousId,
        )
        // Retrofit decoded a 2xx body other than 204 and 205 before handing back the status, and
        // an answer that did not decode came back as no answer; the body is read for the same.
        if (response.success && response.status != NO_CONTENT && response.status != RESET_CONTENT) {
            response.body()
        }
        response.status
    }.getOrNull()

    private companion object {
        const val NO_CONTENT = 204
        const val RESET_CONTENT = 205
    }
}
