package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminDashboard
import com.servacode.directory.api.models.AdminSystemStatus
import com.servacode.directory.api.models.ApiError

interface AdminSystemApi {
    /**
     * GET api/v1/admin/dashboard/
     * Operational counters for the review desk
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminDashboard]
     */
    @GET("api/v1/admin/dashboard/")
    suspend fun adminDashboardRetrieve(): Response<AdminDashboard>

    /**
     * GET api/v1/admin/system/status/
     * Runtime and configuration status
     * Reports only whether each dependency is configured. No secret, connection string or credential is returned.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminSystemStatus]
     */
    @GET("api/v1/admin/system/status/")
    suspend fun adminSystemStatusRetrieve(): Response<AdminSystemStatus>

}
