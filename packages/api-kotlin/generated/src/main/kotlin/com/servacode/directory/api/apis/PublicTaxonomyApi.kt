package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.PublicCategoryList
import com.servacode.directory.api.models.PublicCategoryTags
import com.servacode.directory.api.models.PublicCityList
import com.servacode.directory.api.models.PublicLocationResolve
import com.servacode.directory.api.models.PublicProvinceList

interface PublicTaxonomyApi {
    /**
     * GET api/v1/public/categories/{category_id}/tags/
     * List the specialties and services a category offers
     * The choices behind the specialty and service filters: active items only, in the operators&#39; order. Specialties are the category&#39;s own plus those shared by its specialization. Whether to offer each filter is still decided by the category&#39;s &#x60;specialtyFilter&#x60; and &#x60;serviceFilter&#x60; capabilities. A category that is not public in any province is 404. Cacheable for five minutes (&#x60;Cache-Control: public, max-age&#x3D;300&#x60;).
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @return [PublicCategoryTags]
     */
    @GET("api/v1/public/categories/{category_id}/tags/")
    suspend fun publicCategoryTagsRetrieve(@Path("category_id") categoryId: java.util.UUID): Response<PublicCategoryTags>

    /**
     * GET api/v1/public/locations/resolve/
     * Resolve a coordinate to a province, city and neighbourhood
     * Point-in-polygon against the seeded city and neighbourhood boundaries, then the nearest active province centre within 200 km. No external geocoder is called and the coordinate is not stored.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param latitude 
     * @param longitude 
     * @return [PublicLocationResolve]
     */
    @GET("api/v1/public/locations/resolve/")
    suspend fun publicLocationResolve(@Query("latitude") latitude: kotlin.Double, @Query("longitude") longitude: kotlin.Double): Response<PublicLocationResolve>

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
