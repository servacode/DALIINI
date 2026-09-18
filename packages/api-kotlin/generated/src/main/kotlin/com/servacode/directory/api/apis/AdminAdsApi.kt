package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAdvertisementList
import com.servacode.directory.api.models.AdminAdvertisementRequest
import com.servacode.directory.api.models.AdminId
import com.servacode.directory.api.models.ApiError

interface AdminAdsApi {
    /**
     * POST api/v1/admin/ads/
     * Create an advertisement
     * Requires the manage permission, which is re-checked inside the handler. Action payloads are validated per action type.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminAdvertisementRequest 
     * @return [AdminId]
     */
    @POST("api/v1/admin/ads/")
    suspend fun adminAdCreate(@Body adminAdvertisementRequest: AdminAdvertisementRequest): Response<AdminId>

    /**
     * DELETE api/v1/admin/ads/{advertisement_id}/
     * Delete an advertisement
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param advertisementId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/ads/{advertisement_id}/")
    suspend fun adminAdDelete(@Path("advertisement_id") advertisementId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/admin/ads/
     * List advertisements
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminAdvertisementList]
     */
    @GET("api/v1/admin/ads/")
    suspend fun adminAdsList(): Response<AdminAdvertisementList>

}
