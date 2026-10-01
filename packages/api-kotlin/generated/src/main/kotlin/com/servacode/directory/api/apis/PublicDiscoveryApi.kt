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
import com.servacode.directory.api.models.PublicDutyRoster
import com.servacode.directory.api.models.PublicFacilityDetail
import com.servacode.directory.api.models.PublicHome

interface PublicDiscoveryApi {
    /**
     * GET api/v1/public/duty/
     * Pharmacies on duty on a given day (or up to 7 days)
     * Days are Damascus calendar days starting at &#x60;date&#x60; (default today). A pharmacy is listed on every day one of its duty shifts overlaps. Same visibility as the public duty-now listing. Cacheable for one minute.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId 
     * @param categoryId  (optional)
     * @param cityId  (optional)
     * @param date YYYY-MM-DD (optional)
     * @param days 1 to 7. (optional)
     * @return [PublicDutyRoster]
     */
    @GET("api/v1/public/duty/")
    suspend fun publicDutyByDateList(@Query("provinceId") provinceId: kotlin.String, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("date") date: kotlin.String? = null, @Query("days") days: kotlin.Int? = null): Response<PublicDutyRoster>

    /**
     * GET api/v1/public/facilities/
     * List publicly visible facilities in a province, optionally in one category
     * Ordered nearest-first when coordinates are supplied, otherwise by Arabic name. Availability is computed by the backend. The filters combine: openNow and dutyToday together mean facilities that are both, which is a different question from either alone.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId Province to scope the query to.
     * @param bbox Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
     * @param categoryId Optional category to list. Absent means the whole province. (optional)
     * @param cityId Optional city filter. (optional)
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param dutyNow Pass true to keep only facilities whose duty shift is running. (optional)
     * @param dutyToday Pass true to keep only facilities on today&#39;s duty roster. (optional)
     * @param latitude Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
     * @param limit Page size, maximum 100, default 30. (optional)
     * @param longitude Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
     * @param neighborhoodId Optional neighbourhood filter. (optional)
     * @param openNow Pass true to keep only facilities open at this moment. (optional)
     * @param search Free-text term matched against facility text. (optional)
     * @param serviceId The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
     * @param serviceTagId Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @param sort nearest orders by distance and needs coordinates; name orders by Arabic name. Omitted keeps the historical behaviour: nearest whenever coordinates are supplied, name otherwise. Distances are returned whenever coordinates are supplied, whichever ordering is asked for. (optional)
     * @param specialtyId Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @return [FacilityCursorPage]
     */
    @GET("api/v1/public/facilities/")
    suspend fun publicFacilitiesList(@Query("provinceId") provinceId: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("cursor") cursor: kotlin.String? = null, @Query("dutyNow") dutyNow: kotlin.String? = null, @Query("dutyToday") dutyToday: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("openNow") openNow: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.Int? = null, @Query("serviceTagId") serviceTagId: kotlin.Int? = null, @Query("sort") sort: kotlin.String? = null, @Query("specialtyId") specialtyId: kotlin.Int? = null): Response<FacilityCursorPage>

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
     * @param serviceId The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
     * @param serviceTagId Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @param specialtyId Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @return [PublicHome]
     */
    @GET("api/v1/public/home/")
    suspend fun publicHomeRetrieve(@Query("provinceId") provinceId: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.Int? = null, @Query("serviceTagId") serviceTagId: kotlin.Int? = null, @Query("specialtyId") specialtyId: kotlin.Int? = null): Response<PublicHome>

    /**
     * GET api/v1/public/map/facilities/
     * List compact map markers inside a viewport
     * Capped at 500 markers. Facilities without coordinates are omitted. The filters behave exactly as they do on the list endpoint and combine the same way, so a map and a list asked the same question answer the same.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId Province to scope the query to.
     * @param bbox Viewport as west,south,east,north in WGS84 decimal degrees. (optional)
     * @param categoryId Optional category filter. (optional)
     * @param cityId Optional city filter. (optional)
     * @param dutyNow Pass true to keep only facilities whose duty shift is running. (optional)
     * @param dutyToday Pass true to keep only facilities on today&#39;s duty roster. (optional)
     * @param latitude Caller latitude in WGS84 decimal degrees. Must be sent with longitude. (optional)
     * @param longitude Caller longitude in WGS84 decimal degrees. Must be sent with latitude. (optional)
     * @param neighborhoodId Optional neighbourhood filter. (optional)
     * @param openNow Pass true to keep only facilities open at this moment. (optional)
     * @param search Free-text term matched against facility text. (optional)
     * @param serviceId The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
     * @param serviceTagId Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @param specialtyId Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @return [MapMarkerList]
     */
    @GET("api/v1/public/map/facilities/")
    suspend fun publicMapFacilitiesList(@Query("provinceId") provinceId: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("dutyNow") dutyNow: kotlin.String? = null, @Query("dutyToday") dutyToday: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("openNow") openNow: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.Int? = null, @Query("serviceTagId") serviceTagId: kotlin.Int? = null, @Query("specialtyId") specialtyId: kotlin.Int? = null): Response<MapMarkerList>

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
     * @param serviceId The earlier name of serviceTagId, still accepted; it behaves the same way. (optional)
     * @param serviceTagId Optional service filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares serviceFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @param specialtyId Optional specialty filter, an id from publicCategoryTagsRetrieve. Only facilities whose category declares specialtyFilter can match. Anything but a positive whole number is refused with 400. (optional)
     * @return [FacilityCursorPage]
     */
    @GET("api/v1/public/search/")
    suspend fun publicSearchList(@Query("provinceId") provinceId: kotlin.String, @Query("q") q: kotlin.String, @Query("bbox") bbox: kotlin.String? = null, @Query("categoryId") categoryId: kotlin.String? = null, @Query("cityId") cityId: kotlin.String? = null, @Query("cursor") cursor: kotlin.String? = null, @Query("latitude") latitude: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null, @Query("longitude") longitude: kotlin.String? = null, @Query("neighborhoodId") neighborhoodId: kotlin.String? = null, @Query("search") search: kotlin.String? = null, @Query("serviceId") serviceId: kotlin.Int? = null, @Query("serviceTagId") serviceTagId: kotlin.Int? = null, @Query("specialtyId") specialtyId: kotlin.Int? = null): Response<FacilityCursorPage>

}
