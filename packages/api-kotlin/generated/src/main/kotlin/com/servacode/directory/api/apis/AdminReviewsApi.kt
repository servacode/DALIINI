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
import com.servacode.directory.api.models.AdminRejectionTemplate
import com.servacode.directory.api.models.AdminRejectionTemplateList
import com.servacode.directory.api.models.AdminRejectionTemplateRequest
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
     * POST api/v1/admin/rejection-templates/
     * Create a rejection template
     * Requires admin.reviews.decide, re-checked inside the handler.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminRejectionTemplateRequest 
     * @return [AdminRejectionTemplate]
     */
    @POST("api/v1/admin/rejection-templates/")
    suspend fun adminRejectionTemplateCreate(@Body adminRejectionTemplateRequest: AdminRejectionTemplateRequest): Response<AdminRejectionTemplate>

    /**
     * DELETE api/v1/admin/rejection-templates/{template_id}/
     * Delete a rejection template
     * Past rejections keep their text; a template is only a starting point.
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param templateId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/rejection-templates/{template_id}/")
    suspend fun adminRejectionTemplateDelete(@Path("template_id") templateId: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/admin/rejection-templates/{template_id}/
     * Edit, reorder or retire a rejection template
     * Omitted fields keep their value.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param templateId 
     * @param adminRejectionTemplateRequest 
     * @return [AdminRejectionTemplate]
     */
    @PUT("api/v1/admin/rejection-templates/{template_id}/")
    suspend fun adminRejectionTemplateUpdate(@Path("template_id") templateId: java.util.UUID, @Body adminRejectionTemplateRequest: AdminRejectionTemplateRequest): Response<AdminRejectionTemplate>

    /**
     * GET api/v1/admin/rejection-templates/
     * List rejection templates
     * Ordered by &#x60;sortOrder&#x60;. &#x60;active&#x3D;true&#x60; keeps only the active ones.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param active  (optional)
     * @return [AdminRejectionTemplateList]
     */
    @GET("api/v1/admin/rejection-templates/")
    suspend fun adminRejectionTemplatesList(@Query("active") active: kotlin.Boolean? = null): Response<AdminRejectionTemplateList>

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
     * Capped at 200 rows. Every filter is optional and combines with the rest.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param category Category id of the facility the application belongs to. (optional)
     * @param evidence &#x60;complete&#x60; or &#x60;incomplete&#x60;: whether every required document is uploaded. (optional)
     * @param from Submitted on or after this day (YYYY-MM-DD, Damascus) or this ISO datetime. (optional)
     * @param kind Application kind, for example REGISTRATION or REVERIFICATION. (optional)
     * @param province Province id of the facility the application belongs to. (optional)
     * @param status Application status, for example SUBMITTED or APPROVED. (optional)
     * @param to Submitted on or before this day (YYYY-MM-DD, Damascus) or before this datetime. (optional)
     * @return [AdminApplicationList]
     */
    @GET("api/v1/admin/applications/")
    suspend fun adminReviewsList(@Query("category") category: kotlin.String? = null, @Query("evidence") evidence: kotlin.String? = null, @Query("from") from: kotlin.String? = null, @Query("kind") kind: kotlin.String? = null, @Query("province") province: kotlin.String? = null, @Query("status") status: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<AdminApplicationList>

}
