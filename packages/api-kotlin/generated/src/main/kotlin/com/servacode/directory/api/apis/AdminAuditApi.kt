package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAuditList
import com.servacode.directory.api.models.DetailError

interface AdminAuditApi {
    /**
     * GET api/v1/admin/audit/
     * Search the audit trail
     * Capped at 250 rows. Snapshots and metadata are stored redacted.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminAuditList]
     */
    @GET("api/v1/admin/audit/")
    suspend fun adminAuditList(): Response<AdminAuditList>

}
