package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminFacilityReport
import com.servacode.directory.api.models.AdminFacilityReportList
import com.servacode.directory.api.models.AdminReportDecisionRequest
import com.servacode.directory.api.models.ApiError

interface AdminReportsApi {
    /**
     * POST api/v1/admin/reports/{report_id}/dismiss/
     * Dismiss a report
     * Only OPEN reports can be decided; the decision is audited.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param reportId 
     * @param adminReportDecisionRequest  (optional)
     * @return [AdminFacilityReport]
     */
    @POST("api/v1/admin/reports/{report_id}/dismiss/")
    suspend fun adminReportDismiss(@Path("report_id") reportId: java.util.UUID, @Body adminReportDecisionRequest: AdminReportDecisionRequest? = null): Response<AdminFacilityReport>

    /**
     * POST api/v1/admin/reports/{report_id}/resolve/
     * Mark a report resolved
     * Only OPEN reports can be decided; the decision is audited.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param reportId 
     * @param adminReportDecisionRequest  (optional)
     * @return [AdminFacilityReport]
     */
    @POST("api/v1/admin/reports/{report_id}/resolve/")
    suspend fun adminReportResolve(@Path("report_id") reportId: java.util.UUID, @Body adminReportDecisionRequest: AdminReportDecisionRequest? = null): Response<AdminFacilityReport>

    /**
     * GET api/v1/admin/reports/
     * List facility problem reports
     * Newest first, capped at 250 rows.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param facility Facility id. (optional)
     * @param status OPEN, RESOLVED or DISMISSED. (optional)
     * @return [AdminFacilityReportList]
     */
    @GET("api/v1/admin/reports/")
    suspend fun adminReportsList(@Query("facility") facility: kotlin.String? = null, @Query("status") status: kotlin.String? = null): Response<AdminFacilityReportList>

}
