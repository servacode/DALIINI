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
import com.servacode.directory.api.models.ApiError

interface AdminFacilitiesApi {
    /**
     * GET api/v1/admin/facilities/
     * List facilities for operations
     * Capped at 250 rows.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminFacilityList]
     */
    @GET("api/v1/admin/facilities/")
    suspend fun adminFacilitiesList(): Response<AdminFacilityList>

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
     * @return [AdminFacility]
     */
    @GET("api/v1/admin/facilities/{facility_id}/")
    suspend fun adminFacilityRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<AdminFacility>

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

}
