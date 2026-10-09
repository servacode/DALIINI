package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.AppRelease
import com.servacode.directory.api.models.ContactMessageCreated
import com.servacode.directory.api.models.ContactMessageRequest
import com.servacode.directory.api.models.ContentPage
import com.servacode.directory.api.models.EmergencyNumberList
import com.servacode.directory.api.models.FaqList
import com.servacode.directory.api.models.LegalDocument
import com.servacode.directory.api.models.LegalDocumentList
import com.servacode.directory.api.models.PublicSupport

interface ContentApi {

    /**
    * enum for parameter platform
    */
    enum class PlatformPublicAppReleaseRetrieve(val value: kotlin.String) {
        @SerialName(value = "ANDROID") ANDROID("ANDROID"),
        @SerialName(value = "IOS") IOS("IOS")
    }

    /**
     * GET api/v1/public/app-release/
     * The minimum and newest build of the mobile app
     * A build below &#x60;minimumVersionCode&#x60; must stop and show &#x60;noticeAr&#x60;. A build below &#x60;latestVersionCode&#x60; may offer an update and carry on. Both are zero until an operator sets them, and zero blocks nothing. Cacheable for five minutes.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param platform Defaults to ANDROID. (optional)
     * @return [AppRelease]
     */
    @GET("api/v1/public/app-release/")
    suspend fun publicAppReleaseRetrieve(@Query("platform") platform: PlatformPublicAppReleaseRetrieve? = null): Response<AppRelease>

    /**
     * POST api/v1/contact/
     * Send a message to the platform team
     * Anonymous or signed in; a signed-in sender is linked to their account. Strictly throttled per account or per client address (3/hour by default). The client address is taken from X-Forwarded-For only when the server is configured with the number of trusted proxies.
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *
     * @param contactMessageRequest 
     * @return [ContactMessageCreated]
     */
    @POST("api/v1/contact/")
    suspend fun publicContactCreate(@Body contactMessageRequest: ContactMessageRequest): Response<ContactMessageCreated>

    /**
     * GET api/v1/content/pages/{slug}/
     * Retrieve one published content page
     * Only the published version is served; an unpublished or unknown slug is 404. Cacheable for five minutes (&#x60;Cache-Control: public, max-age&#x3D;300&#x60;).
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param slug 
     * @return [ContentPage]
     */
    @GET("api/v1/content/pages/{slug}/")
    suspend fun publicContentPageRetrieve(@Path("slug") slug: kotlin.String): Response<ContentPage>

    /**
     * GET api/v1/emergency-numbers/
     * Emergency numbers: national, plus the province&#39;s own
     * National numbers come first, then those of &#x60;provinceId&#x60; when one is given. Cacheable for five minutes.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param provinceId Also include this province&#39;s numbers. (optional)
     * @return [EmergencyNumberList]
     */
    @GET("api/v1/emergency-numbers/")
    suspend fun publicEmergencyNumbersList(@Query("provinceId") provinceId: kotlin.String? = null): Response<EmergencyNumberList>

    /**
     * GET api/v1/content/faq/
     * List the published questions and answers, in order
     * Cacheable for five minutes (&#x60;Cache-Control: public, max-age&#x3D;300&#x60;).
     * Responses:
     *  - 200: 
     *
     * @return [FaqList]
     */
    @GET("api/v1/content/faq/")
    suspend fun publicFaqList(): Response<FaqList>

    /**
     * GET api/v1/public/legal/{key}/
     * Retrieve one published page
     * One published page, in full.
     * Responses:
     *  - 200: 
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param key 
     * @return [LegalDocument]
     */
    @GET("api/v1/public/legal/{key}/")
    suspend fun publicLegalDocumentRetrieve(@Path("key") key: kotlin.String): Response<LegalDocument>

    /**
     * GET api/v1/public/legal/
     * List the published pages
     * Titles and versions only. A client compares the version it cached with the one here and fetches a page&#39;s words only when they have changed.
     * Responses:
     *  - 200: 
     *
     * @return [LegalDocumentList]
     */
    @GET("api/v1/public/legal/")
    suspend fun publicLegalDocumentsList(): Response<LegalDocumentList>

    /**
     * GET api/v1/public/support/
     * How to reach the team
     * The WhatsApp number the team answers on, a link that opens a chat with it, and an email address. Null where not configured: a client shows nothing rather than an empty field.
     * Responses:
     *  - 200: 
     *
     * @return [PublicSupport]
     */
    @GET("api/v1/public/support/")
    suspend fun publicSupportContact(): Response<PublicSupport>

}
