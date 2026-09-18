package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.DutyShift
import com.servacode.directory.api.models.DutyShiftList
import com.servacode.directory.api.models.PatchedDutyShift

interface DutyApi {
    /**
     * POST api/v1/owner/facilities/{facility_id}/duty/
     * Schedule a duty shift
     * Overlapping shifts for the same facility are refused by a PostgreSQL exclusion constraint, not only by application code. Only categories that declare the duty capability accept this.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param dutyShift 
     * @return [DutyShift]
     */
    @POST("api/v1/owner/facilities/{facility_id}/duty/")
    suspend fun ownerFacilityDutyCreate(@Path("facility_id") facilityId: java.util.UUID, @Body dutyShift: DutyShift): Response<DutyShift>

    /**
     * DELETE api/v1/owner/facilities/{facility_id}/duty/{shift_id}/
     * Remove a duty shift
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @param shiftId 
     * @return [Unit]
     */
    @DELETE("api/v1/owner/facilities/{facility_id}/duty/{shift_id}/")
    suspend fun ownerFacilityDutyDelete(@Path("facility_id") facilityId: java.util.UUID, @Path("shift_id") shiftId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/owner/facilities/{facility_id}/duty/
     * List duty shifts of a facility
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [DutyShiftList]
     */
    @GET("api/v1/owner/facilities/{facility_id}/duty/")
    suspend fun ownerFacilityDutyList(@Path("facility_id") facilityId: java.util.UUID): Response<DutyShiftList>

    /**
     * PATCH api/v1/owner/facilities/{facility_id}/duty/{shift_id}/
     * Adjust a duty shift
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param facilityId 
     * @param shiftId 
     * @param patchedDutyShift  (optional)
     * @return [DutyShift]
     */
    @PATCH("api/v1/owner/facilities/{facility_id}/duty/{shift_id}/")
    suspend fun ownerFacilityDutyUpdate(@Path("facility_id") facilityId: java.util.UUID, @Path("shift_id") shiftId: java.util.UUID, @Body patchedDutyShift: PatchedDutyShift? = null): Response<DutyShift>

}
