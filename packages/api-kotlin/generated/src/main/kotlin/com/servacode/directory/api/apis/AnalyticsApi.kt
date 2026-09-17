package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AnalyticsEventAccepted
import com.servacode.directory.api.models.AnalyticsEventRequest

interface AnalyticsApi {
    /**
     * POST api/v1/analytics/events/
     * Record a product analytics event
     * The event name must exist in the central registry and its properties are checked against the keys declared for that event. Forbidden keys such as raw coordinates, phone numbers and tokens are rejected rather than stored.
     * Responses:
     *  - 202: 
     *  - 400: Request validation failed.
     *
     * @param analyticsEventRequest 
     * @param xAnonymousId Client-generated pseudonymous id for unauthenticated callers. (optional)
     * @return [AnalyticsEventAccepted]
     */
    @POST("api/v1/analytics/events/")
    suspend fun analyticsEventCreate(@Body analyticsEventRequest: AnalyticsEventRequest, @Header("X-Anonymous-Id") xAnonymousId: kotlin.String? = null): Response<AnalyticsEventAccepted>

}
