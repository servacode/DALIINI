package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminSettingList
import com.servacode.directory.api.models.AdminSettingWriteRequest
import com.servacode.directory.api.models.AdminSettingWritten
import com.servacode.directory.api.models.DetailError

interface AdminSettingsApi {
    /**
     * PUT api/v1/admin/settings/
     * Create or update a typed platform setting
     * Requires the manage permission, which is re-checked inside the handler.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminSettingWriteRequest 
     * @return [AdminSettingWritten]
     */
    @PUT("api/v1/admin/settings/")
    suspend fun adminSettingWrite(@Body adminSettingWriteRequest: AdminSettingWriteRequest): Response<AdminSettingWritten>

    /**
     * GET api/v1/admin/settings/
     * List typed platform settings
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminSettingList]
     */
    @GET("api/v1/admin/settings/")
    suspend fun adminSettingsList(): Response<AdminSettingList>

}
