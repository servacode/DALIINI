package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.AnalyticsApi
import com.servacode.directory.api.models.AnalyticsEventRequest
import com.servacode.directory.core.analytics.AnalyticsTransport
import kotlinx.serialization.json.JsonPrimitive

/**
 * Posts one product event to the backend's own analytics endpoint.
 *
 * It uses the anonymous client, not the authorized one. A measurement must not be attached to an
 * account: the server gets a pseudonymous id in a header and nothing else, and sending the access
 * token with it would turn "how many people opened the map" into "which person opened the map".
 *
 * It never throws. The contract its caller relies on is that a failure comes back as a status or
 * a null, so a dead socket is a value here rather than an exception travelling up into a screen.
 */
class GeneratedAnalyticsTransport(
    private val anonymous: GeneratedClient,
) : AnalyticsTransport {

    private val api: AnalyticsApi by lazy { anonymous.create() }

    override suspend fun send(
        name: String,
        properties: Map<String, String>,
        anonymousId: String,
    ): Int? = runCatching {
        api.analyticsEventCreate(
            analyticsEventRequest = AnalyticsEventRequest(
                name = name,
                properties = properties.mapValues { (_, value) -> JsonPrimitive(value) },
            ),
            xAnonymousId = anonymousId,
        ).code()
    }.getOrNull()
}
