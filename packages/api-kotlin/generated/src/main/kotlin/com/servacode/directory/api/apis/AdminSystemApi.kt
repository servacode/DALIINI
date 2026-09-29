package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAlertList
import com.servacode.directory.api.models.AdminDashboard
import com.servacode.directory.api.models.AdminMe
import com.servacode.directory.api.models.AdminSearchResult
import com.servacode.directory.api.models.AdminSystemStatus
import com.servacode.directory.api.models.AdminTasks
import com.servacode.directory.api.models.ApiError

interface AdminSystemApi {
    /**
     * GET api/v1/admin/alerts/
     * Smart alerts: problems worth acting on now
     * DUTY_GAP: per province offering a duty category, the Damascus days of the next 14 with no duty shift of any ACTIVE pharmacy (critical when the first gap is today or tomorrow). STALE_FACILITY: ACTIVE facilities with no change, owner confirmation or approval for 90 days. REPORTED_FACILITY: 3 or more open reports (critical from 5). ZERO_RESULT_SEARCH: searches without results in the last 7 days, grouped by province and category because search text is never recorded. REVIEW_OVERDUE: submitted applications past the SLA (critical past twice it). MAINTENANCE_ON.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminAlertList]
     */
    @GET("api/v1/admin/alerts/")
    suspend fun adminAlertsList(): Response<AdminAlertList>

    /**
     * GET api/v1/admin/dashboard/
     * Operational counters for the review desk
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminDashboard]
     */
    @GET("api/v1/admin/dashboard/")
    suspend fun adminDashboardRetrieve(): Response<AdminDashboard>

    /**
     * GET api/v1/admin/me/
     * The current operator and the permissions they hold
     * Drives navigation visibility and action gating in the Admin. A UI gate is not authorization: every endpoint re-checks, and a permission revoked mid-session surfaces as a 403 on the next call.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminMe]
     */
    @GET("api/v1/admin/me/")
    suspend fun adminMeRetrieve(): Response<AdminMe>

    /**
     * GET api/v1/admin/search/
     * Search facilities, users and applications at once
     * Up to 5 hits per group. FACILITY (admin.facilities.read): Arabic or English name, or phone digits. USER (admin.users.read, or admin.facilities.read with the phone masked to its last 4 digits): name or phone digits. APPLICATION (admin.reviews.read): facility name. A group the caller may not read is left out, not returned empty.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param q At least 2 chars.
     * @return [AdminSearchResult]
     */
    @GET("api/v1/admin/search/")
    suspend fun adminSearchRetrieve(@Query("q") q: kotlin.String): Response<AdminSearchResult>

    /**
     * GET api/v1/admin/system/status/
     * Runtime and configuration status
     * Reports only whether each dependency is configured. No secret, connection string or credential is returned.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminSystemStatus]
     */
    @GET("api/v1/admin/system/status/")
    suspend fun adminSystemStatusRetrieve(): Response<AdminSystemStatus>

    /**
     * GET api/v1/admin/tasks/
     * The operator&#39;s queue: what is waiting, oldest first
     * Submitted applications split into INITIAL and REVERIFICATION, open problem reports grouped by facility (facilities with 2 or more open reports first) and facilities waiting in REVERIFICATION_REQUIRED. Each bucket has its count, how many are past the SLA (platform setting &#x60;review.slaHours&#x60;, default 48) and up to 10 oldest items with their age in hours and an &#x60;overdue&#x60; flag.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminTasks]
     */
    @GET("api/v1/admin/tasks/")
    suspend fun adminTasksRetrieve(): Response<AdminTasks>

}
