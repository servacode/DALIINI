package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import okhttp3.ResponseBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminApplication
import com.servacode.directory.api.models.AdminApplicationDetail
import com.servacode.directory.api.models.AdminApplicationList
import com.servacode.directory.api.models.AdminDecisionRequest
import com.servacode.directory.api.models.ApiError

interface AdminReviewsApi {
    /**
     * GET api/v1/admin/evidence/{evidence_id}/content/
     * Stream one piece of private verification evidence
     * 
     * Responses:
     *  - 200: Evidence bytes. Served with Cache-Control private, no-store and X-Content-Type-Options nosniff, and every access is audited.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param evidenceId 
     * @return [ResponseBody]
     */
    @GET("api/v1/admin/evidence/{evidence_id}/content/")
    suspend fun adminEvidenceContentRetrieve(@Path("evidence_id") evidenceId: java.util.UUID): Response<ResponseBody>

    /**
     * POST api/v1/admin/applications/{application_id}/approve/
     * Approve an application
     * Runs in one transaction: the application and the facility lifecycle are locked, the current requirements are re-checked, the change is audited and the realtime event is emitted only after commit.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param applicationId 
     * @param adminDecisionRequest  (optional)
     * @return [AdminApplication]
     */
    @POST("api/v1/admin/applications/{application_id}/approve/")
    suspend fun adminReviewApprove(@Path("application_id") applicationId: java.util.UUID, @Body adminDecisionRequest: AdminDecisionRequest? = null): Response<AdminApplication>

    /**
     * POST api/v1/admin/applications/{application_id}/reject/
     * Reject an application
     * A reason is recorded in the audit trail; nothing is silently deleted.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param applicationId 
     * @param adminDecisionRequest  (optional)
     * @return [AdminApplication]
     */
    @POST("api/v1/admin/applications/{application_id}/reject/")
    suspend fun adminReviewReject(@Path("application_id") applicationId: java.util.UUID, @Body adminDecisionRequest: AdminDecisionRequest? = null): Response<AdminApplication>

    /**
     * GET api/v1/admin/applications/{application_id}/
     * Retrieve one application with its review context
     * Evidence is referenced by identifier only; content is fetched separately through the audited evidence endpoint.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param applicationId 
     * @return [AdminApplicationDetail]
     */
    @GET("api/v1/admin/applications/{application_id}/")
    suspend fun adminReviewRetrieve(@Path("application_id") applicationId: java.util.UUID): Response<AdminApplicationDetail>

    /**
     * GET api/v1/admin/applications/
     * List facility applications awaiting or past review
     * Capped at 200 rows.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminApplicationList]
     */
    @GET("api/v1/admin/applications/")
    suspend fun adminReviewsList(): Response<AdminApplicationList>

}
