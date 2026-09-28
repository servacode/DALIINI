package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminContactHandleRequest
import com.servacode.directory.api.models.AdminContactMessage
import com.servacode.directory.api.models.AdminContactMessagePage
import com.servacode.directory.api.models.AdminContentPage
import com.servacode.directory.api.models.AdminContentPageCreateRequest
import com.servacode.directory.api.models.AdminContentPageList
import com.servacode.directory.api.models.AdminContentPageUpdateRequest
import com.servacode.directory.api.models.AdminEmergencyNumber
import com.servacode.directory.api.models.AdminEmergencyNumberList
import com.servacode.directory.api.models.AdminEmergencyNumberRequest
import com.servacode.directory.api.models.AdminFaqEntry
import com.servacode.directory.api.models.AdminFaqEntryList
import com.servacode.directory.api.models.AdminFaqEntryRequest
import com.servacode.directory.api.models.ApiError

interface AdminContentApi {
    /**
     * POST api/v1/admin/contact-messages/{message_id}/handle/
     * Mark a contact message handled
     * Idempotent: a handled message keeps who handled it and when.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param messageId 
     * @param adminContactHandleRequest  (optional)
     * @return [AdminContactMessage]
     */
    @POST("api/v1/admin/contact-messages/{message_id}/handle/")
    suspend fun adminContactMessageHandle(@Path("message_id") messageId: java.util.UUID, @Body adminContactHandleRequest: AdminContactHandleRequest? = null): Response<AdminContactMessage>


    /**
    * enum for parameter kind
    */
    enum class KindAdminContactMessagesList(val value: kotlin.String) {
        @SerialName(value = "CORRECTION") CORRECTION("CORRECTION"),
        @SerialName(value = "GENERAL") GENERAL("GENERAL"),
        @SerialName(value = "OWNER") OWNER("OWNER")
    }


    /**
    * enum for parameter status
    */
    enum class StatusAdminContactMessagesList(val value: kotlin.String) {
        @SerialName(value = "handled") handled("handled"),
        @SerialName(value = "open") `open`("open")
    }

    /**
     * GET api/v1/admin/contact-messages/
     * The contact inbox, newest first
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param cursor  (optional)
     * @param kind  (optional)
     * @param limit  (optional)
     * @param status &#x60;open&#x60; keeps unhandled messages, &#x60;handled&#x60; the rest. (optional)
     * @return [AdminContactMessagePage]
     */
    @GET("api/v1/admin/contact-messages/")
    suspend fun adminContactMessagesList(@Query("cursor") cursor: kotlin.String? = null, @Query("kind") kind: KindAdminContactMessagesList? = null, @Query("limit") limit: kotlin.Int? = null, @Query("status") status: StatusAdminContactMessagesList? = null): Response<AdminContactMessagePage>

    /**
     * POST api/v1/admin/content/pages/
     * Create a content page
     * 409 CONTENT_PAGE_EXISTS when the slug is taken (case-insensitive).
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param adminContentPageCreateRequest 
     * @return [AdminContentPage]
     */
    @POST("api/v1/admin/content/pages/")
    suspend fun adminContentPageCreate(@Body adminContentPageCreateRequest: AdminContentPageCreateRequest): Response<AdminContentPage>

    /**
     * DELETE api/v1/admin/content/pages/{slug}/
     * Delete a content page and all its versions
     * 409 CONTENT_PAGE_BUILT_IN for the six built-in pages: unpublish them.
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param slug 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/content/pages/{slug}/")
    suspend fun adminContentPageDelete(@Path("slug") slug: kotlin.String): Response<Unit>

    /**
     * GET api/v1/admin/content/pages/{slug}/
     * Retrieve a content page with its newest words
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param slug 
     * @return [AdminContentPage]
     */
    @GET("api/v1/admin/content/pages/{slug}/")
    suspend fun adminContentPageRetrieve(@Path("slug") slug: kotlin.String): Response<AdminContentPage>

    /**
     * PUT api/v1/admin/content/pages/{slug}/
     * Edit, publish or unpublish a content page
     * Changing the words of a page that was ever published writes a new version and keeps the old one as history; until &#x60;published: true&#x60; is sent the live version stays as it was (&#x60;hasUnpublishedChanges&#x60;). &#x60;published: false&#x60; takes the page offline. Omitted fields keep their value.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param slug 
     * @param adminContentPageUpdateRequest  (optional)
     * @return [AdminContentPage]
     */
    @PUT("api/v1/admin/content/pages/{slug}/")
    suspend fun adminContentPageUpdate(@Path("slug") slug: kotlin.String, @Body adminContentPageUpdateRequest: AdminContentPageUpdateRequest? = null): Response<AdminContentPage>

    /**
     * GET api/v1/admin/content/pages/
     * List content pages, including the built-in legal pages
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminContentPageList]
     */
    @GET("api/v1/admin/content/pages/")
    suspend fun adminContentPagesList(): Response<AdminContentPageList>

    /**
     * POST api/v1/admin/emergency-numbers/
     * Add an emergency number
     * No &#x60;provinceId&#x60; (or null) makes it national.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminEmergencyNumberRequest 
     * @return [AdminEmergencyNumber]
     */
    @POST("api/v1/admin/emergency-numbers/")
    suspend fun adminEmergencyNumberCreate(@Body adminEmergencyNumberRequest: AdminEmergencyNumberRequest): Response<AdminEmergencyNumber>

    /**
     * DELETE api/v1/admin/emergency-numbers/{number_id}/
     * Delete an emergency number
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param numberId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/emergency-numbers/{number_id}/")
    suspend fun adminEmergencyNumberDelete(@Path("number_id") numberId: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/admin/emergency-numbers/{number_id}/
     * Edit, move, reorder or deactivate an emergency number
     * Omitted fields keep their value; &#x60;provinceId: null&#x60; makes it national.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param numberId 
     * @param adminEmergencyNumberRequest 
     * @return [AdminEmergencyNumber]
     */
    @PUT("api/v1/admin/emergency-numbers/{number_id}/")
    suspend fun adminEmergencyNumberUpdate(@Path("number_id") numberId: java.util.UUID, @Body adminEmergencyNumberRequest: AdminEmergencyNumberRequest): Response<AdminEmergencyNumber>

    /**
     * GET api/v1/admin/emergency-numbers/
     * List emergency numbers, national and provincial, active or not
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param provinceId Keep this province&#39;s numbers; &#x60;national&#x60; keeps national ones. (optional)
     * @return [AdminEmergencyNumberList]
     */
    @GET("api/v1/admin/emergency-numbers/")
    suspend fun adminEmergencyNumbersList(@Query("provinceId") provinceId: kotlin.String? = null): Response<AdminEmergencyNumberList>

    /**
     * GET api/v1/admin/content/faq/
     * List FAQ entries, published or not
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminFaqEntryList]
     */
    @GET("api/v1/admin/content/faq/")
    suspend fun adminFaqEntriesList(): Response<AdminFaqEntryList>

    /**
     * POST api/v1/admin/content/faq/
     * Add a FAQ entry
     * 
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminFaqEntryRequest 
     * @return [AdminFaqEntry]
     */
    @POST("api/v1/admin/content/faq/")
    suspend fun adminFaqEntryCreate(@Body adminFaqEntryRequest: AdminFaqEntryRequest): Response<AdminFaqEntry>

    /**
     * DELETE api/v1/admin/content/faq/{entry_id}/
     * Delete a FAQ entry
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param entryId 
     * @return [Unit]
     */
    @DELETE("api/v1/admin/content/faq/{entry_id}/")
    suspend fun adminFaqEntryDelete(@Path("entry_id") entryId: java.util.UUID): Response<Unit>

    /**
     * PUT api/v1/admin/content/faq/{entry_id}/
     * Edit, reorder, publish or unpublish a FAQ entry
     * Omitted fields keep their value.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param entryId 
     * @param adminFaqEntryRequest 
     * @return [AdminFaqEntry]
     */
    @PUT("api/v1/admin/content/faq/{entry_id}/")
    suspend fun adminFaqEntryUpdate(@Path("entry_id") entryId: java.util.UUID, @Body adminFaqEntryRequest: AdminFaqEntryRequest): Response<AdminFaqEntry>

}
