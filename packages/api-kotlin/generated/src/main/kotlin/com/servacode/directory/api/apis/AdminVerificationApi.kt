package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminId
import com.servacode.directory.api.models.AdminVerificationRequirement
import com.servacode.directory.api.models.AdminVerificationRequirementList
import com.servacode.directory.api.models.AdminVerificationRequirementRequest
import com.servacode.directory.api.models.AdminVerificationRequirementUpdateRequest
import com.servacode.directory.api.models.ApiError

interface AdminVerificationApi {
    /**
     * POST api/v1/admin/verification-requirements/
     * Create a verification requirement
     * Requires the manage permission, which is re-checked inside the handler.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminVerificationRequirementRequest 
     * @return [AdminId]
     */
    @POST("api/v1/admin/verification-requirements/")
    suspend fun adminVerificationRequirementCreate(@Body adminVerificationRequirementRequest: AdminVerificationRequirementRequest): Response<AdminId>

    /**
     * PUT api/v1/admin/verification-requirements/{requirement_id}/
     * Edit a verification requirement, or retire it
     * The owning category cannot change: evidence already submitted points at a (facility, requirement) pair. Retirement is &#x60;active &#x3D; false&#x60;; there is no delete, because evidence references the row.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param requirementId 
     * @param adminVerificationRequirementUpdateRequest  (optional)
     * @return [AdminVerificationRequirement]
     */
    @PUT("api/v1/admin/verification-requirements/{requirement_id}/")
    suspend fun adminVerificationRequirementUpdate(@Path("requirement_id") requirementId: kotlin.Int, @Body adminVerificationRequirementUpdateRequest: AdminVerificationRequirementUpdateRequest? = null): Response<AdminVerificationRequirement>

    /**
     * GET api/v1/admin/verification-requirements/
     * List verification requirements
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminVerificationRequirementList]
     */
    @GET("api/v1/admin/verification-requirements/")
    suspend fun adminVerificationRequirementsList(): Response<AdminVerificationRequirementList>

}
