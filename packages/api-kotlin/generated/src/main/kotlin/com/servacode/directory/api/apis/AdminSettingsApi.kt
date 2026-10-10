package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminAppRelease
import com.servacode.directory.api.models.AdminAppReleaseRequest
import com.servacode.directory.api.models.AdminSettingList
import com.servacode.directory.api.models.AdminSettingWriteRequest
import com.servacode.directory.api.models.AdminSettingWritten
import com.servacode.directory.api.models.AdminWhatsAppState
import com.servacode.directory.api.models.ApiError

interface AdminSettingsApi {

    /**
    * enum for parameter platform
    */
    enum class PlatformAdminAppReleaseRetrieve(val value: kotlin.String) {
        @SerialName(value = "ANDROID") ANDROID("ANDROID"),
        @SerialName(value = "IOS") IOS("IOS")
    }

    /**
     * GET api/v1/admin/app-release/
     * What a mobile build must be
     * Zeros mean nothing is enforced, which is what an unset platform reads as.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param platform Defaults to ANDROID. (optional)
     * @return [AdminAppRelease]
     */
    @GET("api/v1/admin/app-release/")
    suspend fun adminAppReleaseRetrieve(@Query("platform") platform: PlatformAdminAppReleaseRetrieve? = null): Response<AdminAppRelease>


    /**
    * enum for parameter platform
    */
    enum class PlatformAdminAppReleaseUpdate(val value: kotlin.String) {
        @SerialName(value = "ANDROID") ANDROID("ANDROID"),
        @SerialName(value = "IOS") IOS("IOS")
    }

    /**
     * PUT api/v1/admin/app-release/
     * Set what a mobile build must be
     * Requires admin.settings.manage, re-checked inside the handler. A minimum above the latest is refused: nobody can install a build that does not exist. Audited.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminAppReleaseRequest 
     * @param platform Defaults to ANDROID. (optional)
     * @return [AdminAppRelease]
     */
    @PUT("api/v1/admin/app-release/")
    suspend fun adminAppReleaseUpdate(@Body adminAppReleaseRequest: AdminAppReleaseRequest, @Query("platform") platform: PlatformAdminAppReleaseUpdate? = null): Response<AdminAppRelease>

    /**
     * PUT api/v1/admin/settings/
     * Create or update a typed platform setting
     * Requires the manage permission, which is re-checked inside the handler.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param adminSettingWriteRequest 
     * @return [AdminSettingWritten]
     */
    @PUT("api/v1/admin/settings/")
    suspend fun adminSettingWrite(@Body adminSettingWriteRequest: AdminSettingWriteRequest): Response<AdminSettingWritten>

    /**
     * GET api/v1/admin/settings/
     * List typed platform settings
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminSettingList]
     */
    @GET("api/v1/admin/settings/")
    suspend fun adminSettingsList(): Response<AdminSettingList>

    /**
     * POST api/v1/admin/whatsapp/relink/
     * Forget the linked WhatsApp account and start a fresh pairing
     * The bot logs its account out (it leaves the phone&#39;s linked devices), forgets it and offers a new QR code. Codes cannot be sent until it is scanned.
     * Responses:
     *  - 202: 
     *  - 400: A domain rule rejected the request; `code` names the rule.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminWhatsAppState]
     */
    @POST("api/v1/admin/whatsapp/relink/")
    suspend fun adminWhatsAppRelink(): Response<AdminWhatsAppState>

    /**
     * GET api/v1/admin/whatsapp/
     * The WhatsApp bot&#39;s link: state, number, or the QR code to scan
     * The bot&#39;s link, for the console&#39;s «ربط واتساب» card.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AdminWhatsAppState]
     */
    @GET("api/v1/admin/whatsapp/")
    suspend fun adminWhatsAppRetrieve(): Response<AdminWhatsAppState>

}
