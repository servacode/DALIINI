package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.BusinessHourInput
import com.servacode.directory.api.models.BusinessHoursList
import com.servacode.directory.api.models.TemporaryClosure
import com.servacode.directory.api.models.TemporaryClosureInput
import com.servacode.directory.api.models.TemporaryClosureList

interface AvailabilityApi {
    /**
     * PUT api/v1/owner/facilities/{facility_id}/hours/
     * Replace the weekly opening hours of a facility
     * The whole week is replaced in one call. Overnight spans are supported and same-day overlaps are rejected. Only categories that declare the hours capability accept this.
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
    @PUT("api/v1/owner/facilities/{facility_id}/hours/")
    suspend fun ownerFacilityHoursReplace(@Path("facility_id") facilityId: java.util.UUID, @Body businessHourInput: kotlin.collections.List<BusinessHourInput>): Response<BusinessHoursList>

    /**
     * DELETE api/v1/owner/facilities/{facility_id}/temporary-closures/{closure_id}/
     * Cancel a temporary closure
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param closureId 
     * @param facilityId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/facilities/{facility_id}/temporary-closures/{closure_id}/")
    suspend fun ownerFacilityTemporaryClosureCancel(@Path("closure_id") closureId: java.util.UUID, @Path("facility_id") facilityId: java.util.UUID): Response<Unit>

    /**
     * POST api/v1/owner/facilities/{facility_id}/temporary-closures/
     * Open a temporary closure window
     * A temporary closure overrides both regular hours and duty.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param temporaryClosureInput 
     * @return [TemporaryClosure]
     */
    @POST("api/v1/owner/facilities/{facility_id}/temporary-closures/")
    suspend fun ownerFacilityTemporaryClosureCreate(@Path("facility_id") facilityId: java.util.UUID, @Body temporaryClosureInput: TemporaryClosureInput): Response<TemporaryClosure>

    /**
     * GET api/v1/owner/facilities/{facility_id}/temporary-closures/
     * List temporary closures of a facility
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [TemporaryClosureList]
     */
    @GET("api/v1/owner/facilities/{facility_id}/temporary-closures/")
    suspend fun ownerFacilityTemporaryClosuresList(@Path("facility_id") facilityId: java.util.UUID): Response<TemporaryClosureList>

}
