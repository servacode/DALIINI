package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminCityAdmin
import com.servacode.directory.api.models.AdminCityAdminList
import com.servacode.directory.api.models.AdminCityUpdateRequest
import com.servacode.directory.api.models.AdminProvinceList
import com.servacode.directory.api.models.AdminProvinceReadiness
import com.servacode.directory.api.models.AdminProvinceUpdateRequest
import com.servacode.directory.api.models.AdminProvinceUpdated
import com.servacode.directory.api.models.ApiError

interface AdminProvincesApi {
    /**
     * GET api/v1/admin/provinces/{province_id}/cities/
     * List every city of a province, active or not
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId 
     * @return [AdminCityAdminList]
     */
    @GET("api/v1/admin/provinces/{province_id}/cities/")
    suspend fun adminProvinceCitiesList(@Path("province_id") provinceId: java.util.UUID): Response<AdminCityAdminList>

    /**
     * PUT api/v1/admin/provinces/{province_id}/cities/{city_id}/
     * Activate or deactivate a city
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param cityId 
     * @param provinceId 
     * @param adminCityUpdateRequest 
     * @return [AdminCityAdmin]
     */
    @PUT("api/v1/admin/provinces/{province_id}/cities/{city_id}/")
    suspend fun adminProvinceCityUpdate(@Path("city_id") cityId: java.util.UUID, @Path("province_id") provinceId: java.util.UUID, @Body adminCityUpdateRequest: AdminCityUpdateRequest): Response<AdminCityAdmin>

    /**
     * GET api/v1/admin/provinces/{province_id}/readiness/
     * Launch checklist for a province
     * PROVINCE_ACTIVE; CATEGORY_PUBLIC (at least one active category publicly enabled); MIN_ACTIVE_FACILITIES (platform setting &#x60;readiness.minActiveFacilities&#x60;, default 5); DUTY_COVERAGE (no DUTY_GAP in the next 14 days, or not applicable when the province offers no duty category); EMERGENCY_NUMBERS (an active national or provincial number).
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param provinceId 
     * @return [AdminProvinceReadiness]
     */
    @GET("api/v1/admin/provinces/{province_id}/readiness/")
    suspend fun adminProvinceReadinessRetrieve(@Path("province_id") provinceId: java.util.UUID): Response<AdminProvinceReadiness>

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
