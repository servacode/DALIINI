package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminProvinceList
import com.servacode.directory.api.models.AdminProvinceUpdateRequest
import com.servacode.directory.api.models.AdminProvinceUpdated
import com.servacode.directory.api.models.DetailError

interface AdminProvincesApi {
    /**
     * PUT api/v1/admin/provinces/{province_id}/
     * Activate a province or change its order
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId 
     * @param adminProvinceUpdateRequest  (optional)
     * @return [AdminProvinceUpdated]
     */
    @PUT("api/v1/admin/provinces/{province_id}/")
    suspend fun adminProvinceUpdate(@Path("province_id") provinceId: java.util.UUID, @Body adminProvinceUpdateRequest: AdminProvinceUpdateRequest? = null): Response<AdminProvinceUpdated>

    /**
     * GET api/v1/admin/provinces/
     * List every province
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminProvinceList]
     */
    @GET("api/v1/admin/provinces/")
    suspend fun adminProvincesList(): Response<AdminProvinceList>

}
