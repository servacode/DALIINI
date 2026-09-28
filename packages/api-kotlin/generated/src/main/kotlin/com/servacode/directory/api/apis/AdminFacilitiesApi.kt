package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminDecisionRequest
import com.servacode.directory.api.models.AdminFacility
import com.servacode.directory.api.models.AdminFacilityList
import com.servacode.directory.api.models.AdminFacilityQuality
import com.servacode.directory.api.models.AdminTimeline
import com.servacode.directory.api.models.ApiError

interface AdminFacilitiesApi {

    /**
    * enum for parameter issue
    */
    enum class IssueAdminFacilitiesList(val value: kotlin.String) {
        @SerialName(value = "NOT_VERIFIED_RECENTLY") NOT_VERIFIED_RECENTLY("NOT_VERIFIED_RECENTLY"),
        @SerialName(value = "NO_HOURS") NO_HOURS("NO_HOURS"),
        @SerialName(value = "NO_LOCATION") NO_LOCATION("NO_LOCATION"),
        @SerialName(value = "NO_PHONE") NO_PHONE("NO_PHONE"),
        @SerialName(value = "NO_PHOTOS") NO_PHOTOS("NO_PHOTOS"),
        @SerialName(value = "OPEN_REPORTS") OPEN_REPORTS("OPEN_REPORTS"),
        @SerialName(value = "STALE") STALE("STALE")
    }


    /**
    * enum for parameter ordering
    */
    enum class OrderingAdminFacilitiesList(val value: kotlin.String) {
        @SerialName(value = "-qualityScore") MinusQualityScore("-qualityScore"),
        @SerialName(value = "-updatedAt") MinusUpdatedAt("-updatedAt"),
        @SerialName(value = "qualityScore") qualityScore("qualityScore"),
        @SerialName(value = "updatedAt") updatedAt("updatedAt")
    }

    /**
     * GET api/v1/admin/facilities/
     * List facilities for operations
     * Capped at 250 rows. Every filter is optional and combines with the rest. Each row carries &#x60;qualityScore&#x60; (0-100) and &#x60;qualityIssues&#x60;, computed in the same query.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param category Category id. (optional)
     * @param issue Keep facilities that have this quality issue. (optional)
     * @param ordering Sort order; the default is &#x60;-updatedAt&#x60; (most recently changed). (optional)
     * @param province Province id. (optional)
     * @param q Free text matched against the Arabic and English facility names. (optional)
     * @param status Facility status, for example ACTIVE or SUSPENDED. (optional)
     * @return [AdminFacilityList]
     */
    @GET("api/v1/admin/facilities/")
    suspend fun adminFacilitiesList(@Query("category") category: kotlin.String? = null, @Query("issue") issue: IssueAdminFacilitiesList? = null, @Query("ordering") ordering: OrderingAdminFacilitiesList? = null, @Query("province") province: kotlin.String? = null, @Query("q") q: kotlin.String? = null, @Query("status") status: kotlin.String? = null): Response<AdminFacilityList>

    /**
     * POST api/v1/admin/facilities/{facility_id}/close/
     * Close a facility
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param adminDecisionRequest  (optional)
     * @return [AdminFacility]
     */
    @POST("api/v1/admin/facilities/{facility_id}/close/")
    suspend fun adminFacilityClose(@Path("facility_id") facilityId: java.util.UUID, @Body adminDecisionRequest: AdminDecisionRequest? = null): Response<AdminFacility>

    /**
     * POST api/v1/admin/facilities/{facility_id}/reactivate/
     * Reactivate a suspended facility
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param adminDecisionRequest  (optional)
     * @return [AdminFacility]
     */
    @POST("api/v1/admin/facilities/{facility_id}/reactivate/")
    suspend fun adminFacilityReactivate(@Path("facility_id") facilityId: java.util.UUID, @Body adminDecisionRequest: AdminDecisionRequest? = null): Response<AdminFacility>

    /**
     * GET api/v1/admin/facilities/{facility_id}/
     * Retrieve one facility
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [AdminFacilityQuality]
     */
    @GET("api/v1/admin/facilities/{facility_id}/")
    suspend fun adminFacilityRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<AdminFacilityQuality>

    /**
     * POST api/v1/admin/facilities/{facility_id}/suspend/
     * Suspend a facility
     * A suspended facility leaves public discovery and cannot self-reactivate.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param adminDecisionRequest  (optional)
     * @return [AdminFacility]
     */
    @POST("api/v1/admin/facilities/{facility_id}/suspend/")
    suspend fun adminFacilitySuspend(@Path("facility_id") facilityId: java.util.UUID, @Body adminDecisionRequest: AdminDecisionRequest? = null): Response<AdminFacility>

    /**
     * GET api/v1/admin/facilities/{facility_id}/timeline/
     * Everything that happened to a facility, newest first
     * Merges applications (submitted, decided), problem reports (created, resolved or dismissed), audited changes to the facility and its applications, reports, images, evidence and duty shifts, and a summary of the next 14 days of duty. Up to 200 events.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [AdminTimeline]
     */
    @GET("api/v1/admin/facilities/{facility_id}/timeline/")
    suspend fun adminFacilityTimelineRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<AdminTimeline>

}
