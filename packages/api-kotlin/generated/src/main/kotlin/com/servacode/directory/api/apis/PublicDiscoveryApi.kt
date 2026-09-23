package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.FacilityCursorPage
import com.servacode.directory.api.models.MapMarkerList
import com.servacode.directory.api.models.PublicFacilityDetail
import com.servacode.directory.api.models.PublicHome

interface PublicDiscoveryApi {
    /**
     * GET api/v1/public/facilities/
     * List publicly visible facilities in a province, optionally in one category
     * Ordered nearest-first when coordinates are supplied, otherwise by Arabic name. Availability is computed by the backend; openNow and dutyNow filter on that computed state rather than on a stored flag.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId Province to scope the query to.
     * @param bbox Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
     * @param categoryId Optional category to list. Absent means the whole province. (optional)
     * @param cityId Optional city filter. (optional)
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param dutyNow Pass true to keep only facilities currently on duty. (optional)
     * @param latitude Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
     * @param limit Page size, maximum 100, default 30. (optional)
     * @param longitude Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
     * @param neighborhoodId Optional neighbourhood filter. (optional)
     * @param openNow Pass true to keep only facilities currently open. (optional)
     * @param search Free-text term matched against facility text. (optional)
     * @param serviceId Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
     * @param specialtyId Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)
     * @return [FacilityCursorPage]
     */
    @GET("api/v1/public/facilities/")
    suspend fun publicFacilitiesList(@Query("provinceId") provinceId: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("cursor") cursor: kotlin.String? = null, @Query("dutyNow") dutyNow: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("openNow") openNow: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.String? = null, @Query("specialtyId") specialtyId: kotlin.String? = null): Response<FacilityCursorPage>

    /**
     * GET api/v1/public/facilities/{facility_id}/
     * Retrieve one publicly visible facility
     * Returns the public projection only. Verification evidence, reviewer notes, memberships, internal policy fields and raw storage keys are never included.
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param facilityId 
     * @return [PublicFacilityDetail]
     */
    @GET("api/v1/public/facilities/{facility_id}/")
    suspend fun publicFacilityRetrieve(@Path("facility_id") facilityId: java.util.UUID): Response<PublicFacilityDetail>

    /**
     * GET api/v1/public/home/
     * Retrieve the home composition for a province
     * Bundles advertisements, the active category grid and three facility strips so the first screen needs one round trip. serverTime is authoritative.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId Province to scope the query to.
     * @param bbox Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
     * @param categoryId Optional category filter. (optional)
     * @param cityId Optional city filter. (optional)
     * @param latitude Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
     * @param longitude Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
     * @param neighborhoodId Optional neighbourhood filter. (optional)
     * @param search Free-text term matched against facility text. (optional)
     * @param serviceId Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
     * @param specialtyId Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)
     * @return [PublicHome]
     */
    @GET("api/v1/public/home/")
    suspend fun publicHomeRetrieve(@Query("provinceId") provinceId: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.String? = null, @Query("specialtyId") specialtyId: kotlin.String? = null): Response<PublicHome>

    /**
     * GET api/v1/public/map/facilities/
     * List compact map markers inside a viewport
     * Capped at 500 markers. Facilities without coordinates are omitted. openNow and dutyNow filter on the availability the backend computes, exactly as the list endpoint does, so a map and a list asked the same question answer the same.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId Province to scope the query to.
     * @param bbox Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
     * @param categoryId Optional category filter. (optional)
     * @param cityId Optional city filter. (optional)
     * @param dutyNow Pass true to keep only facilities currently on duty. (optional)
     * @param latitude Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
     * @param longitude Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
     * @param neighborhoodId Optional neighbourhood filter. (optional)
     * @param openNow Pass true to keep only facilities currently open. (optional)
     * @param search Free-text term matched against facility text. (optional)
     * @param serviceId Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
     * @param specialtyId Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)
     * @return [MapMarkerList]
     */
    @GET("api/v1/public/map/facilities/")
    suspend fun publicMapFacilitiesList(@Query("provinceId") provinceId: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("dutyNow") dutyNow: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("openNow") openNow: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.String? = null, @Query("specialtyId") specialtyId: kotlin.String? = null): Response<MapMarkerList>

    /**
     * GET api/v1/public/search/
     * Search facilities within a province
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId Province to scope the query to.
     * @param q Search term, at least two characters.
     * @param bbox Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
     * @param categoryId Optional category filter. (optional)
     * @param cityId Optional city filter. (optional)
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param latitude Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
     * @param limit Page size, maximum 100, default 30. (optional)
     * @param longitude Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
     * @param neighborhoodId Optional neighbourhood filter. (optional)
     * @param search Free-text term matched against facility text. (optional)
     * @param serviceId Optional service-tag filter; only meaningful when the category declares serviceFilter. (optional)
     * @param specialtyId Optional specialty filter; only meaningful when the category declares specialtyFilter. (optional)
     * @return [FacilityCursorPage]
     */
    @GET("api/v1/public/search/")
    suspend fun publicSearchList(@Query("provinceId") provinceId: kotlin.String, @Query("q") q: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("cursor") cursor: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.String? = null, @Query("specialtyId") specialtyId: kotlin.String? = null): Response<FacilityCursorPage>

}
