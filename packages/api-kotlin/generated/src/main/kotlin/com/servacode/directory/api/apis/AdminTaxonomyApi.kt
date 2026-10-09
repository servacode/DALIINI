package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminCapabilities
import com.servacode.directory.api.models.AdminCapabilitiesRequest
import com.servacode.directory.api.models.AdminCategory
import com.servacode.directory.api.models.AdminCategoryCardList
import com.servacode.directory.api.models.AdminCategoryCreateRequest
import com.servacode.directory.api.models.AdminCategoryGroup
import com.servacode.directory.api.models.AdminCategoryGroupList
import com.servacode.directory.api.models.AdminCategoryGroupRequest
import com.servacode.directory.api.models.AdminCategoryProvinceRequest
import com.servacode.directory.api.models.AdminCategoryUpdateRequest
import com.servacode.directory.api.models.AdminId
import com.servacode.directory.api.models.AdminServiceTag
import com.servacode.directory.api.models.AdminServiceTagCreateRequest
import com.servacode.directory.api.models.AdminServiceTagList
import com.servacode.directory.api.models.AdminSpecialty
import com.servacode.directory.api.models.AdminSpecialtyCreateRequest
import com.servacode.directory.api.models.AdminSpecialtyList
import com.servacode.directory.api.models.AdminTagUpdateRequest
import com.servacode.directory.api.models.ApiError

interface AdminTaxonomyApi {
    /**
     * GET api/v1/admin/categories/
     * List categories
     * Each category with its capability flags, its province switches and how many facilities it holds: everything its card and settings window show, in one query count whatever the number of categories.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminCategoryCardList]
     */
    @GET("api/v1/admin/categories/")
    suspend fun adminCategoriesList(): Response<AdminCategoryCardList>

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
     * POST api/v1/admin/categories/create/
     * Create a category
     * &#x60;code&#x60; and &#x60;slug&#x60; are fixed at creation and cannot be changed afterwards. A new category is invisible everywhere until its per-province switches are turned on, whatever &#x60;active&#x60; says.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminCategoryCreateRequest 
     * @return [AdminId]
     */
    @POST("api/v1/admin/categories/create/")
    suspend fun adminCategoryCreate(@Body adminCategoryCreateRequest: AdminCategoryCreateRequest): Response<AdminId>

    /**
     * POST api/v1/admin/category-groups/create/
     * Create a category group
     * Cycle J begins here: a group has to exist before a category can join it.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminCategoryGroupRequest  (optional)
     * @return [AdminId]
     */
    @POST("api/v1/admin/category-groups/create/")
    suspend fun adminCategoryGroupCreate(@Body adminCategoryGroupRequest: AdminCategoryGroupRequest? = null): Response<AdminId>

    /**
     * PUT api/v1/admin/category-groups/{group_id}/
     * Rename, reorder or deactivate a category group
     * The group code is immutable; sending a different one is refused.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param groupId 
     * @param adminCategoryGroupRequest  (optional)
     * @return [AdminCategoryGroup]
     */
    @PUT("api/v1/admin/category-groups/{group_id}/")
    suspend fun adminCategoryGroupUpdate(@Path("group_id") groupId: java.util.UUID, @Body adminCategoryGroupRequest: AdminCategoryGroupRequest? = null): Response<AdminCategoryGroup>

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

    /**
     * POST api/v1/admin/categories/{category_id}/service-tags/
     * Add a service to a category
     * Requires admin.taxonomy.manage, re-checked inside the handler. A name already used in the category is refused, retired services included.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @param adminServiceTagCreateRequest 
     * @return [AdminServiceTag]
     */
    @POST("api/v1/admin/categories/{category_id}/service-tags/")
    suspend fun adminCategoryServiceTagCreate(@Path("category_id") categoryId: java.util.UUID, @Body adminServiceTagCreateRequest: AdminServiceTagCreateRequest): Response<AdminServiceTag>

    /**
     * GET api/v1/admin/categories/{category_id}/service-tags/
     * List the services of a category
     * Retired ones included, in the order the public sees them.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @return [AdminServiceTagList]
     */
    @GET("api/v1/admin/categories/{category_id}/service-tags/")
    suspend fun adminCategoryServiceTagsList(@Path("category_id") categoryId: java.util.UUID): Response<AdminServiceTagList>

    /**
     * GET api/v1/admin/categories/{category_id}/specialties/
     * List the specialties a category offers, in both scopes
     * The category&#39;s own specialties and those its specialization shares, retired ones included, in the order the public sees them.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @return [AdminSpecialtyList]
     */
    @GET("api/v1/admin/categories/{category_id}/specialties/")
    suspend fun adminCategorySpecialtiesList(@Path("category_id") categoryId: java.util.UUID): Response<AdminSpecialtyList>

    /**
     * POST api/v1/admin/categories/{category_id}/specialties/
     * Add a specialty to a category or to its specialization
     * Requires admin.taxonomy.manage, re-checked inside the handler. &#x60;scope&#x60; CATEGORY scopes it to this category; SPECIALIZATION shares it with every category of this category&#39;s specialization and is refused for a GENERIC category. A name already used in the same scope is refused, retired items included.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @param adminSpecialtyCreateRequest 
     * @return [AdminSpecialty]
     */
    @POST("api/v1/admin/categories/{category_id}/specialties/")
    suspend fun adminCategorySpecialtyCreate(@Path("category_id") categoryId: java.util.UUID, @Body adminSpecialtyCreateRequest: AdminSpecialtyCreateRequest): Response<AdminSpecialty>

    /**
     * PUT api/v1/admin/categories/{category_id}/
     * Rename, move, reorder or deactivate a category
     * &#x60;code&#x60; and &#x60;slug&#x60; are immutable and are not accepted. Changing the specialization re-validates the capability set, so a category that carries duty cannot be moved off PHARMACY while it does.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param categoryId 
     * @param adminCategoryUpdateRequest  (optional)
     * @return [AdminCategory]
     */
    @PUT("api/v1/admin/categories/{category_id}/")
    suspend fun adminCategoryUpdate(@Path("category_id") categoryId: java.util.UUID, @Body adminCategoryUpdateRequest: AdminCategoryUpdateRequest? = null): Response<AdminCategory>

    /**
     * DELETE api/v1/admin/service-tags/{service_tag_id}/
     * Delete a service no facility lists
     * One that a facility lists answers 409 &#x60;SERVICE_TAG_IN_USE&#x60;; retire it with &#x60;active &#x3D; false&#x60; instead.
     * Responses:
     *  - 204: No response body
     *  - 409: The request conflicts with the current state or with a domain rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param serviceTagId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/service-tags/{service_tag_id}/")
    suspend fun adminServiceTagDelete(@Path("service_tag_id") serviceTagId: kotlin.Int): Response<Unit>

    /**
     * PUT api/v1/admin/service-tags/{service_tag_id}/
     * Rename, reorder, retire or bring back a service
     * Omitted fields keep their value. The category is fixed: &#x60;categoryId&#x60; is refused.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param serviceTagId 
     * @param adminTagUpdateRequest  (optional)
     * @return [AdminServiceTag]
     */
    @PUT("api/v1/admin/service-tags/{service_tag_id}/")
    suspend fun adminServiceTagUpdate(@Path("service_tag_id") serviceTagId: kotlin.Int, @Body adminTagUpdateRequest: AdminTagUpdateRequest? = null): Response<AdminServiceTag>

    /**
     * DELETE api/v1/admin/specialties/{specialty_id}/
     * Delete a specialty no facility lists
     * One that a facility lists answers 409 &#x60;SPECIALTY_IN_USE&#x60;; retire it with &#x60;active &#x3D; false&#x60; instead.
     * Responses:
     *  - 204: No response body
     *  - 409: The request conflicts with the current state or with a domain rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param specialtyId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/specialties/{specialty_id}/")
    suspend fun adminSpecialtyDelete(@Path("specialty_id") specialtyId: kotlin.Int): Response<Unit>

    /**
     * PUT api/v1/admin/specialties/{specialty_id}/
     * Rename, reorder, retire or bring back a specialty
     * Omitted fields keep their value. The scope is fixed: &#x60;scope&#x60;, &#x60;categoryId&#x60; and &#x60;specialization&#x60; are refused. A specialization&#39;s specialty changes for every category of that specialization.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param specialtyId 
     * @param adminTagUpdateRequest  (optional)
     * @return [AdminSpecialty]
     */
    @PUT("api/v1/admin/specialties/{specialty_id}/")
    suspend fun adminSpecialtyUpdate(@Path("specialty_id") specialtyId: kotlin.Int, @Body adminTagUpdateRequest: AdminTagUpdateRequest? = null): Response<AdminSpecialty>

}
