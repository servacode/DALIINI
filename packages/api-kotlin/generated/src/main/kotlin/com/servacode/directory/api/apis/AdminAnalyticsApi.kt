package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAnalytics
import com.servacode.directory.api.models.AdminAnalyticsSeries
import com.servacode.directory.api.models.AdminStaffPerformance
import com.servacode.directory.api.models.ApiError

interface AdminAnalyticsApi {
    /**
     * GET api/v1/admin/analytics/
     * Operational KPIs
     * Period-bound KPIs (approval median and the four event counts) cover &#x60;from&#x60; to &#x60;to&#x60;, by default the last 30 days, and &#x60;previous&#x60; holds the same KPIs for the equally long period just before, for comparison. The remaining fields are current totals.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param from ISO date or datetime; default 30 days before &#x60;to&#x60;. (optional)
     * @param to ISO date or datetime; a bare date includes that whole day. (optional)
     * @return [AdminAnalytics]
     */
    @GET("api/v1/admin/analytics/")
    suspend fun adminAnalyticsRetrieve(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<AdminAnalytics>

    /**
     * GET api/v1/admin/analytics/series/
     * The period&#39;s numbers, one Damascus day at a time
     * Every day from &#x60;from&#x60; to &#x60;to&#x60; is present, a quiet day as zeros. The event series are the same four the period totals count; &#x60;newUsers&#x60; are accounts created, &#x60;approvals&#x60; applications approved and &#x60;reports&#x60; problem reports received.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param from ISO date or datetime; default 30 days before &#x60;to&#x60;. (optional)
     * @param to ISO date or datetime; a bare date includes that whole day. (optional)
     * @return [AdminAnalyticsSeries]
     */
    @GET("api/v1/admin/analytics/series/")
    suspend fun adminAnalyticsSeriesRetrieve(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<AdminAnalyticsSeries>

    /**
     * GET api/v1/admin/analytics/staff/
     * Reviewer performance in a period
     * Per reviewer, over applications decided in [&#x60;from&#x60;, &#x60;to&#x60;) (default last 30 days): decisions, approvals, rejections and the median submit-to-decision hours; plus problem reports they resolved or dismissed in the period.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param from  (optional)
     * @param to  (optional)
     * @return [AdminStaffPerformance]
     */
    @GET("api/v1/admin/analytics/staff/")
    suspend fun adminAnalyticsStaffRetrieve(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<AdminStaffPerformance>

}
