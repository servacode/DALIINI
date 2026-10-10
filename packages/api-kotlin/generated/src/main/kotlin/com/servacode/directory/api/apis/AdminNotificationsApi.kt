package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AdminBroadcast
import com.servacode.directory.api.models.AdminBroadcastAudience
import com.servacode.directory.api.models.AdminBroadcastPage
import com.servacode.directory.api.models.AdminBroadcastRequest
import com.servacode.directory.api.models.ApiError

interface AdminNotificationsApi {
    /**
     * POST api/v1/admin/notifications/broadcast/
     * Send a notification to many users
     * ALL reaches every active account (narrowed to accounts that chose &#x60;provinceId&#x60; when given); OWNERS reaches every active owner or manager (narrowed to facilities in &#x60;provinceId&#x60;). Each recipient gets an inbox notification of type &#x60;platform.broadcast&#x60;, and a push is queued for accounts with a device. Audited. At most 5 broadcasts per operator per hour (429).
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *
     * @param adminBroadcastRequest 
     * @return [AdminBroadcast]
     */
    @POST("api/v1/admin/notifications/broadcast/")
    suspend fun adminNotificationBroadcast(@Body adminBroadcastRequest: AdminBroadcastRequest): Response<AdminBroadcast>


    /**
    * enum for parameter audience
    */
    enum class AudienceAdminNotificationBroadcastAudience(val value: kotlin.String) {
        @SerialName(value = "ALL") ALL("ALL"),
        @SerialName(value = "OWNERS") OWNERS("OWNERS")
    }

    /**
     * GET api/v1/admin/notifications/broadcast/audience/
     * How many accounts a broadcast would reach
     * How many accounts a broadcast would reach, asked before it is sent.  A broadcast to a province nobody chose said «أُرسل» to nobody at all; the console now shows the count in its confirmation and warns when it is zero.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param audience 
     * @param provinceId  (optional)
     * @return [AdminBroadcastAudience]
     */
    @GET("api/v1/admin/notifications/broadcast/audience/")
    suspend fun adminNotificationBroadcastAudience(@Query("audience") audience: AudienceAdminNotificationBroadcastAudience, @Query("provinceId") provinceId: kotlin.String? = null): Response<AdminBroadcastAudience>

    /**
     * GET api/v1/admin/notifications/broadcasts/
     * Broadcast history, newest first
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param cursor  (optional)
     * @param limit  (optional)
     * @return [AdminBroadcastPage]
     */
    @GET("api/v1/admin/notifications/broadcasts/")
    suspend fun adminNotificationBroadcastsList(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null): Response<AdminBroadcastPage>

}
