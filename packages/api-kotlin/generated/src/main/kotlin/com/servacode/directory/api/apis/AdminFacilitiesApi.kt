package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminDecisionRequest
import com.servacode.directory.api.models.AdminFacility
import com.servacode.directory.api.models.AdminFacilityCreate
import com.servacode.directory.api.models.AdminFacilityDetail
import com.servacode.directory.api.models.AdminFacilityList
import com.servacode.directory.api.models.AdminFacilityMap
import com.servacode.directory.api.models.AdminTimeline
import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.BusinessHourInput
import com.servacode.directory.api.models.BusinessHoursList
import com.servacode.directory.api.models.OwnerFacilityImage
import com.servacode.directory.api.models.OwnerFacilityImageList
import com.servacode.directory.api.models.PatchedAdminFacilityWrite

import okhttp3.MultipartBody

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
     * In cursor pages. Every filter is optional and combines with the rest. Each row carries &#x60;qualityScore&#x60; (0-100) and &#x60;qualityIssues&#x60;, computed in the same query.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param category Category id. (optional)
     * @param city City id. (optional)
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param id One facility by id: what a link written before the console had cards resolves to. (optional)
     * @param issue Keep facilities that have this quality issue. (optional)
     * @param limit Page size, maximum 200, default 50. (optional)
     * @param ordering Sort order; the default is &#x60;-updatedAt&#x60; (most recently changed). (optional)
     * @param province Province id. (optional)
     * @param q Free text matched against the Arabic and English facility names. (optional)
     * @param status Facility status, for example ACTIVE or SUSPENDED. (optional)
     * @return [AdminFacilityList]
     */
    @GET("api/v1/admin/facilities/")
    suspend fun adminFacilitiesList(@Query("category") category: kotlin.String? = null, @Query("city") city: kotlin.String? = null, @Query("cursor") cursor: kotlin.String? = null, @Query("id") id: kotlin.String? = null, @Query("issue") issue: IssueAdminFacilitiesList? = null, @Query("limit") limit: kotlin.Int? = null, @Query("ordering") ordering: OrderingAdminFacilitiesList? = null, @Query("province") province: kotlin.String? = null, @Query("q") q: kotlin.String? = null, @Query("status") status: kotlin.String? = null): Response<AdminFacilityList>

    /**
     * GET api/v1/admin/facilities/map/
     * Located facilities as map points, with the same filters as the list
     * Every located facility the filters select, as points (DECISION-075).  The same filters as the list, so \&quot;the map of what I am looking at\&quot; is one click. Only what a pin needs travels: the name, the state and the coordinates. A facility without a location is counted rather than dropped silently, so the operator can go and fix it.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param category Category id. (optional)
     * @param city City id. (optional)
     * @param issue One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. (optional)
     * @param province Province id. (optional)
     * @param q Free text matched against the facility names. (optional)
     * @param status Facility status. (optional)
     * @return [AdminFacilityMap]
     */
    @GET("api/v1/admin/facilities/map/")
    suspend fun adminFacilitiesMap(@Query("category") category: kotlin.String? = null, @Query("city") city: kotlin.String? = null, @Query("issue") issue: kotlin.String? = null, @Query("province") province: kotlin.String? = null, @Query("q") q: kotlin.String? = null, @Query("status") status: kotlin.String? = null): Response<AdminFacilityMap>

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
     * POST api/v1/admin/facilities/
     * Add a facility to the directory
     * Listed by the directory itself, with no owner; an owner can claim it later. ACTIVE (the default) publishes it at once and counts as verified. The same validation as an owner&#39;s edit applies. Requires &#x60;admin.facilities.edit&#x60;; audited.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminFacilityCreate 
     * @return [AdminFacilityDetail]
     */
    @POST("api/v1/admin/facilities/")
    suspend fun adminFacilityCreate(@Body adminFacilityCreate: AdminFacilityCreate): Response<AdminFacilityDetail>

    /**
     * GET api/v1/admin/facilities/{facility_id}/hours/
     * A facility&#39;s weekly opening hours
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [BusinessHoursList]
     */
    @GET("api/v1/admin/facilities/{facility_id}/hours/")
    suspend fun adminFacilityHoursList(@Path("facility_id") facilityId: java.util.UUID): Response<BusinessHoursList>

    /**
     * PUT api/v1/admin/facilities/{facility_id}/hours/
     * Replace a facility&#39;s weekly opening hours
     * The whole week in one call, as the owner&#39;s own route: overnight spans are allowed and same-day overlaps refused. Requires &#x60;admin.facilities.edit&#x60;.
     * Responses:
     *  - 200: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param businessHourInput 
     * @return [BusinessHoursList]
     */
    @PUT("api/v1/admin/facilities/{facility_id}/hours/")
    suspend fun adminFacilityHoursReplace(@Path("facility_id") facilityId: java.util.UUID, @Body businessHourInput: kotlin.collections.List<BusinessHourInput>): Response<BusinessHoursList>

    /**
     * POST api/v1/admin/facilities/{facility_id}/images/
     * Add a public photo to a facility
     * Multipart, through the same pipeline as the owner&#39;s upload: the file is decoded, bounded, re-encoded to JPEG, stripped and stored under a random key. Requires &#x60;admin.facilities.edit&#x60;.
     * Responses:
     *  - 201: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param file 
     * @return [OwnerFacilityImage]
     */
    @Multipart
    @POST("api/v1/admin/facilities/{facility_id}/images/")
    suspend fun adminFacilityImageCreate(@Path("facility_id") facilityId: java.util.UUID, @Part file: MultipartBody.Part): Response<OwnerFacilityImage>

    /**
     * DELETE api/v1/admin/facilities/{facility_id}/images/{image_id}/
     * Remove a public photo from a facility
     * Requires &#x60;admin.facilities.edit&#x60;. The stored file goes once the row does.
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param imageId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/facilities/{facility_id}/images/{image_id}/")
    suspend fun adminFacilityImageDelete(@Path("facility_id") facilityId: java.util.UUID, @Path("image_id") imageId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/admin/facilities/{facility_id}/images/
     * A facility&#39;s public photos
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [OwnerFacilityImageList]
     */
    @GET("api/v1/admin/facilities/{facility_id}/images/")
    suspend fun adminFacilityImagesList(@Path("facility_id") facilityId: java.util.UUID): Response<OwnerFacilityImageList>

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
     * Everything the console shows and edits, with the quality score.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [AdminFacilityDetail]
     */
    @GET("api/v1/admin/facilities/{facility_id}/")
    suspend fun adminFacilityRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<AdminFacilityDetail>

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

    /**
     * PATCH api/v1/admin/facilities/{facility_id}/
     * Correct a facility&#39;s details
     * Only the fields sent change. The status is left as it is: an operator&#39;s correction does not send a live facility back for re-verification. Moving it to another province clears its city unless one is sent; another category clears its specialties and services unless they are sent. Requires &#x60;admin.facilities.edit&#x60;; audited with both snapshots, and the owners are notified.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param patchedAdminFacilityWrite  (optional)
     * @return [AdminFacilityDetail]
     */
    @PATCH("api/v1/admin/facilities/{facility_id}/")
    suspend fun adminFacilityUpdate(@Path("facility_id") facilityId: java.util.UUID, @Body patchedAdminFacilityWrite: PatchedAdminFacilityWrite? = null): Response<AdminFacilityDetail>

}
