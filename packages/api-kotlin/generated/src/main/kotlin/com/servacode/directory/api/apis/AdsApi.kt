package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.PublicAdvertisementList

interface AdsApi {
    /**
     * GET api/v1/public/ads/
     * List advertisements currently in flight
     * First-party advertisements only, filtered server-side by schedule, enabled state and targeting scope. Capped at 50 items.
     * Responses:
     *  - 200: 
     *
     * @param categoryId Restrict to advertisements targeting this category. (optional)
     * @param provinceId Restrict to advertisements targeting this province. (optional)
     * @return [PublicAdvertisementList]
     */
    @GET("api/v1/public/ads/")
    suspend fun publicAdsList(@Query("categoryId") categoryId: kotlin.String? = null, @Query("provinceId") provinceId: kotlin.String? = null): Response<PublicAdvertisementList>

}
