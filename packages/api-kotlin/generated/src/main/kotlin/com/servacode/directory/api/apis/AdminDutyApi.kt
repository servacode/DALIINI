package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminDutyRoster
import com.servacode.directory.api.models.AdminDutyShift
import com.servacode.directory.api.models.AdminDutyShiftCreateRequest
import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.PatchedAdminDutyShiftUpdateRequest

interface AdminDutyApi {
    /**
     * GET api/v1/admin/duty/
     * The duty roster of a province (or city), day by day
     * Days are Damascus calendar days from &#x60;from&#x60; to &#x60;to&#x60; inclusive, by default today and the next 13 days, at most 62. Each day lists the shifts of ACTIVE duty pharmacies overlapping it, and &#x60;gap&#x60; is true when there is none: the rule behind the DUTY_GAP alert. With &#x60;cityId&#x60;, shifts and gaps are those of that city&#39;s pharmacies.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId 
     * @param cityId  (optional)
     * @param from YYYY-MM-DD (optional)
     * @param to YYYY-MM-DD (optional)
     * @return [AdminDutyRoster]
     */
    @GET("api/v1/admin/duty/")
    suspend fun adminDutyRosterRetrieve(@Query("provinceId") provinceId: kotlin.String, @Query("cityId") cityId: kotlin.String? = null, @Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<AdminDutyRoster>

    /**
     * POST api/v1/admin/duty/
     * Put a duty shift on a pharmacy&#39;s roster
     * Same rules as the owner endpoint: the category must support duty (409 DUTY_NOT_SUPPORTED), the shift must not overlap another of the same pharmacy or be invalid (409 DUTY_OVERLAP_OR_INVALID) and must not fall in a temporary closure (409 DUTY_DURING_CLOSURE). Recorded with &#x60;createdBy&#x60; ADMIN, audited, and the pharmacy&#39;s owners are notified. Sending a shift identical to an existing one returns it with 200 and changes nothing.
     * Responses:
     *  - 201: 
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param adminDutyShiftCreateRequest 
     * @return [AdminDutyShift]
     */
    @POST("api/v1/admin/duty/")
    suspend fun adminDutyShiftCreate(@Body adminDutyShiftCreateRequest: AdminDutyShiftCreateRequest): Response<AdminDutyShift>

    /**
     * DELETE api/v1/admin/duty/{shift_id}/
     * Cancel a duty shift
     * Audited; the pharmacy&#39;s owners are notified.
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param shiftId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/duty/{shift_id}/")
    suspend fun adminDutyShiftDelete(@Path("shift_id") shiftId: java.util.UUID): Response<Unit>

    /**
     * PATCH api/v1/admin/duty/{shift_id}/
     * Move a duty shift
     * Same validation as creating one. &#x60;createdBy&#x60; keeps who created the shift. Audited; the pharmacy&#39;s owners are notified when the times change.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param shiftId 
     * @param patchedAdminDutyShiftUpdateRequest  (optional)
     * @return [AdminDutyShift]
     */
    @PATCH("api/v1/admin/duty/{shift_id}/")
    suspend fun adminDutyShiftUpdate(@Path("shift_id") shiftId: java.util.UUID, @Body patchedAdminDutyShiftUpdateRequest: PatchedAdminDutyShiftUpdateRequest? = null): Response<AdminDutyShift>

}
