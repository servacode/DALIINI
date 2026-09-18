package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAuditList
import com.servacode.directory.api.models.ApiError

interface AdminAuditApi {
    /**
     * GET api/v1/admin/audit/
     * Search the audit trail
     * Capped at 250 rows. Snapshots and metadata are stored redacted. Every filter is optional and combines with the rest.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param action Substring matched against the action code, case-insensitive. (optional)
     * @param actor Actor user id. (optional)
     * @param requestId Exact request correlation id, as returned in an error body. (optional)
     * @param resource Substring matched against the target type, or an exact target id. (optional)
     * @return [AdminAuditList]
     */
    @GET("api/v1/admin/audit/")
    suspend fun adminAuditList(@Query("action") action: kotlin.String? = null, @Query("actor") actor: kotlin.String? = null, @Query("requestId") requestId: kotlin.String? = null, @Query("resource") resource: kotlin.String? = null): Response<AdminAuditList>

}
