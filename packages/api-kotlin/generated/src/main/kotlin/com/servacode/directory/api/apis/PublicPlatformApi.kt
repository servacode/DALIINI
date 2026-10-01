package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.PlatformStatus

interface PublicPlatformApi {
    /**
     * GET api/v1/platform/status/
     * Platform availability (maintenance mode)
     * Always served, even during maintenance. While &#x60;maintenance&#x60; is true every other public and owner API answers 503 with code MAINTENANCE, details {retryAfterSeconds} and a Retry-After header.
     * Responses:
     *  - 200: 
     *
     * @return [PlatformStatus]
     */
    @GET("api/v1/platform/status/")
    suspend fun publicPlatformStatusRetrieve(): Response<PlatformStatus>

}
