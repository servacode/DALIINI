package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError

interface AdminExportsApi {

    /**
    * enum for parameter format
    */
    enum class FormatAdminExportAuditCsv(val value: kotlin.String) {
        @SerialName(value = "csv") csv("csv"),
        @SerialName(value = "json") json("json")
    }

    /**
     * GET api/v1/admin/exports/audit.csv
     * Export the audit trail as CSV
     * Same filters as the audit search, newest first, without its 250 cap. Snapshots are left out; metadata is included as recorded (already redacted).
     * Responses:
     *  - 200: text/csv; charset=utf-8, starting with a UTF-8 byte-order mark, sent as an attachment. At most 50000 data rows.
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param action Substring of the action code. (optional)
     * @param actor Actor user id. (optional)
     * @param format  (optional)
     * @param from ISO date or datetime. (optional)
     * @param requestId Exact request correlation id. (optional)
     * @param resource Substring of the target type, or an exact target id. (optional)
     * @param to ISO date or datetime; a bare date includes that whole day. (optional)
     * @return [kotlin.String]
     */
    @GET("api/v1/admin/exports/audit.csv")
    suspend fun adminExportAuditCsv(@Query("action") action: kotlin.String? = null, @Query("actor") actor: kotlin.String? = null, @Query("format") format: FormatAdminExportAuditCsv? = null, @Query("from") from: kotlin.String? = null, @Query("requestId") requestId: kotlin.String? = null, @Query("resource") resource: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<kotlin.String>


    /**
    * enum for parameter format
    */
    enum class FormatAdminExportFacilitiesCsv(val value: kotlin.String) {
        @SerialName(value = "csv") csv("csv"),
        @SerialName(value = "json") json("json")
    }

    /**
     * GET api/v1/admin/exports/facilities.csv
     * Export the facility list as CSV
     * Same filters and ordering as the facility list, without its 250 cap.
     * Responses:
     *  - 200: text/csv; charset=utf-8, starting with a UTF-8 byte-order mark, sent as an attachment. At most 50000 data rows.
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param category Category id. (optional)
     * @param format  (optional)
     * @param issue One of NO_PHOTOS, NO_HOURS, NO_LOCATION, NO_PHONE, STALE, OPEN_REPORTS, NOT_VERIFIED_RECENTLY. (optional)
     * @param ordering qualityScore, -qualityScore, updatedAt or -updatedAt. (optional)
     * @param province Province id. (optional)
     * @param q Free text matched against the facility names. (optional)
     * @param status Facility status. (optional)
     * @return [kotlin.String]
     */
    @GET("api/v1/admin/exports/facilities.csv")
    suspend fun adminExportFacilitiesCsv(@Query("category") category: kotlin.String? = null, @Query("format") format: FormatAdminExportFacilitiesCsv? = null, @Query("issue") issue: kotlin.String? = null, @Query("ordering") ordering: kotlin.String? = null, @Query("province") province: kotlin.String? = null, @Query("q") q: kotlin.String? = null, @Query("status") status: kotlin.String? = null): Response<kotlin.String>


    /**
    * enum for parameter format
    */
    enum class FormatAdminExportReportsCsv(val value: kotlin.String) {
        @SerialName(value = "csv") csv("csv"),
        @SerialName(value = "json") json("json")
    }

    /**
     * GET api/v1/admin/exports/reports.csv
     * Export problem reports as CSV
     * Same filters as the report list, newest first, without its 250 cap.
     * Responses:
     *  - 200: text/csv; charset=utf-8, starting with a UTF-8 byte-order mark, sent as an attachment. At most 50000 data rows.
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param facility Facility id. (optional)
     * @param format  (optional)
     * @param status OPEN, RESOLVED or DISMISSED. (optional)
     * @return [kotlin.String]
     */
    @GET("api/v1/admin/exports/reports.csv")
    suspend fun adminExportReportsCsv(@Query("facility") facility: kotlin.String? = null, @Query("format") format: FormatAdminExportReportsCsv? = null, @Query("status") status: kotlin.String? = null): Response<kotlin.String>

}
