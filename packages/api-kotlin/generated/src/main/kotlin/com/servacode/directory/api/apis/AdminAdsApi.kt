package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdStats
import com.servacode.directory.api.models.AdminAdImage
import com.servacode.directory.api.models.AdminAdvertisementList
import com.servacode.directory.api.models.AdminAdvertisementRequest
import com.servacode.directory.api.models.AdminAdvertisementUpdateRequest
import com.servacode.directory.api.models.AdminId
import com.servacode.directory.api.models.ApiError

import okhttp3.MultipartBody

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
     * POST api/v1/admin/ads/images/
     * Upload an advertisement image
     * multipart/form-data with &#x60;file&#x60;. JPEG, PNG or WebP only, at most 2 MB, each side 100 to 4096 px. The image is re-encoded to JPEG (metadata stripped) and stored in public media under a random key. Pass the returned &#x60;imageKey&#x60; when creating or updating the advertisement.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param file JPEG, PNG or WebP, at most 2 MB, 100-4096 px a side.
     * @return [AdminAdImage]
     */
    @Multipart
    @POST("api/v1/admin/ads/images/")
    suspend fun adminAdImageUpload(@Part file: MultipartBody.Part): Response<AdminAdImage>

    /**
     * GET api/v1/admin/ads/stats/
     * Impressions and clicks of each advertisement over a period
     * Counted from the apps&#39; &#x60;ad_impression&#x60; and &#x60;ad_click&#x60; events, by Damascus day. The default is the last 30 days; at most a year. Every advertisement is listed, those never shown with zeros.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param from  (optional)
     * @param to  (optional)
     * @return [AdStats]
     */
    @GET("api/v1/admin/ads/stats/")
    suspend fun adminAdStatsRetrieve(@Query("from") from: kotlin.String? = null, @Query("to") to: kotlin.String? = null): Response<AdStats>

    /**
     * PUT api/v1/admin/ads/{advertisement_id}/
     * Edit an advertisement, its schedule or its activation
     * Omitted fields keep their current value. Schedule, targeting and action payload are validated together, so an end before its start or a global advertisement carrying a target is refused.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param advertisementId 
     * @param adminAdvertisementUpdateRequest  (optional)
     * @return [AdminId]
     */
    @PUT("api/v1/admin/ads/{advertisement_id}/")
    suspend fun adminAdUpdate(@Path("advertisement_id") advertisementId: java.util.UUID, @Body adminAdvertisementUpdateRequest: AdminAdvertisementUpdateRequest? = null): Response<AdminId>

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
