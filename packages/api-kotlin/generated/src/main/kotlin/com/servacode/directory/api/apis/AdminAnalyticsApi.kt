package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAnalytics
import com.servacode.directory.api.models.DetailError

interface AdminAnalyticsApi {
    /**
     * GET api/v1/admin/analytics/
     * Operational KPIs
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminAnalytics]
     */
    @GET("api/v1/admin/analytics/")
    suspend fun adminAnalyticsRetrieve(): Response<AdminAnalytics>

}
