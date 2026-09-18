package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminCapabilities
import com.servacode.directory.api.models.AdminCapabilitiesRequest
import com.servacode.directory.api.models.AdminCategoryGroupList
import com.servacode.directory.api.models.AdminCategoryList
import com.servacode.directory.api.models.AdminCategoryProvinceRequest
import com.servacode.directory.api.models.AdminId
import com.servacode.directory.api.models.ApiError

interface AdminTaxonomyApi {
    /**
     * GET api/v1/admin/categories/
     * List categories
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminCategoryList]
     */
    @GET("api/v1/admin/categories/")
    suspend fun adminCategoriesList(): Response<AdminCategoryList>

    /**
     * PUT api/v1/admin/categories/{category_id}/capabilities/
     * Set the capability flags of a category
     * Duty can only be enabled for an approved specialization.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @param adminCapabilitiesRequest  (optional)
     * @return [AdminCapabilities]
     */
    @PUT("api/v1/admin/categories/{category_id}/capabilities/")
    suspend fun adminCategoryCapabilitiesReplace(@Path("category_id") categoryId: java.util.UUID, @Body adminCapabilitiesRequest: AdminCapabilitiesRequest? = null): Response<AdminCapabilities>

    /**
     * GET api/v1/admin/category-groups/
     * List category groups
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminCategoryGroupList]
     */
    @GET("api/v1/admin/category-groups/")
    suspend fun adminCategoryGroupsList(): Response<AdminCategoryGroupList>

    /**
     * PUT api/v1/admin/categories/{category_id}/provinces/
     * Set the per-province switches of a category
     * Public visibility and owner onboarding are independent switches.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @param adminCategoryProvinceRequest 
     * @return [AdminId]
     */
    @PUT("api/v1/admin/categories/{category_id}/provinces/")
    suspend fun adminCategoryProvinceReplace(@Path("category_id") categoryId: java.util.UUID, @Body adminCategoryProvinceRequest: AdminCategoryProvinceRequest): Response<AdminId>

}
