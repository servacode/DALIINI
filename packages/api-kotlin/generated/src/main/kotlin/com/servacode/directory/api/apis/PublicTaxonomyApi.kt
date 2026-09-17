package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.DetailError
import com.servacode.directory.api.models.PublicCategoryList
import com.servacode.directory.api.models.PublicCityList
import com.servacode.directory.api.models.PublicProvinceList

interface PublicTaxonomyApi {
    /**
     * GET api/v1/public/provinces/{province_id}/categories/
     * List categories publicly enabled for a province
     * A category is listed only when the province is active, the group and the category are active, and the per-province public switch is on. Clients drive their UI from the returned capability flags, never from the category name.
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId 
     * @return [PublicCategoryList]
     */
    @GET("api/v1/public/provinces/{province_id}/categories/")
    suspend fun publicProvinceCategoriesList(@Path("province_id") provinceId: java.util.UUID): Response<PublicCategoryList>

    /**
     * GET api/v1/public/provinces/{province_id}/cities/
     * List active cities in a province
     * 
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId 
     * @return [PublicCityList]
     */
    @GET("api/v1/public/provinces/{province_id}/cities/")
    suspend fun publicProvinceCitiesList(@Path("province_id") provinceId: java.util.UUID): Response<PublicCityList>

    /**
     * GET api/v1/public/provinces/
     * List active provinces
     * Every province is seeded, but only active ones are publicly visible. Ordered by sort order then Arabic name.
     * Responses:
     *  - 200: 
     *
     * @return [PublicProvinceList]
     */
    @GET("api/v1/public/provinces/")
    suspend fun publicProvincesList(): Response<PublicProvinceList>

}
