package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AccountDeletionRequested
import com.servacode.directory.api.models.AccountRatingList
import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.ChallengeAccepted
import com.servacode.directory.api.models.ChallengeVerify
import com.servacode.directory.api.models.DeletionRequest
import com.servacode.directory.api.models.FavoriteList
import com.servacode.directory.api.models.FavoriteState
import com.servacode.directory.api.models.FavoriteWrite
import com.servacode.directory.api.models.NotificationPage
import com.servacode.directory.api.models.PasswordChange
import com.servacode.directory.api.models.PatchedProfilePatch
import com.servacode.directory.api.models.PhoneChangeStart
import com.servacode.directory.api.models.Profile
import com.servacode.directory.api.models.PushToken
import com.servacode.directory.api.models.PushTokenRegister
import com.servacode.directory.api.models.UnreadCount

import okhttp3.MultipartBody

interface AccountApi {
    /**
     * POST api/v1/account/deletion-request/
     * Request deletion of the account of the caller
     * Required by Play policy for any app that creates accounts. Ownership obligations and legally retained records are handled by the deletion policy.
     * Responses:
     *  - 202: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param deletionRequest 
     * @return [AccountDeletionRequested]
     */
    @POST("api/v1/account/deletion-request/")
    suspend fun accountDeletionRequestCreate(@Body deletionRequest: DeletionRequest): Response<AccountDeletionRequested>

    /**
     * POST api/v1/account/favorites/
     * Save a facility
     * Idempotent: saving a facility that is already saved changes nothing.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param favoriteWrite 
     * @return [FavoriteState]
     */
    @POST("api/v1/account/favorites/")
    suspend fun accountFavoriteAdd(@Body favoriteWrite: FavoriteWrite): Response<FavoriteState>

    /**
     * DELETE api/v1/account/favorites/{facility_id}/
     * Remove a facility the caller had saved
     * Idempotent: removing what was not saved is not an error.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param facilityId 
     * @return [FavoriteState]
     */
    @DELETE("api/v1/account/favorites/{facility_id}/")
    suspend fun accountFavoriteRemove(@Path("facility_id") facilityId: java.util.UUID): Response<FavoriteState>

    /**
     * GET api/v1/account/favorites/
     * List the facilities the caller has saved
     * Newest first, cursor-paginated. A saved facility that is no longer public — closed, suspended, or in a category the province stopped serving — is not returned, because this list is served by the same public query every other list uses.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param limit Page size, maximum 100, default 30. (optional)
     * @return [FavoriteList]
     */
    @GET("api/v1/account/favorites/")
    suspend fun accountFavoritesList(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null): Response<FavoriteList>

    /**
     * POST api/v1/account/notifications/{notification_id}/read/
     * Mark one notification as read
     * Idempotent: a message that was already read keeps the time it was read.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *  - 404: The addressed resource does not exist or is not visible to the caller.
     *
     * @param notificationId 
     * @return [UnreadCount]
     */
    @POST("api/v1/account/notifications/{notification_id}/read/")
    suspend fun accountNotificationMarkRead(@Path("notification_id") notificationId: java.util.UUID): Response<UnreadCount>

    /**
     * GET api/v1/account/notifications/
     * List the caller&#39;s notifications, newest first
     * The account&#39;s own inbox.  Every message the platform has sent this account is here whether or not a push ever reached the device, which is what makes the inbox the record and the push only an announcement.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param cursor Opaque token returned as &#x60;nextCursor&#x60; by the previous page. (optional)
     * @param limit Page size, maximum 100, default 30. (optional)
     * @return [NotificationPage]
     */
    @GET("api/v1/account/notifications/")
    suspend fun accountNotificationsList(@Query("cursor") cursor: kotlin.String? = null, @Query("limit") limit: kotlin.Int? = null): Response<NotificationPage>

    /**
     * POST api/v1/account/notifications/read-all/
     * Mark every unread notification as read
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [UnreadCount]
     */
    @POST("api/v1/account/notifications/read-all/")
    suspend fun accountNotificationsMarkAllRead(): Response<UnreadCount>

    /**
     * GET api/v1/account/notifications/unread-count/
     * How many of the caller&#39;s notifications are unread
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [UnreadCount]
     */
    @GET("api/v1/account/notifications/unread-count/")
    suspend fun accountNotificationsUnreadCount(): Response<UnreadCount>

    /**
     * POST api/v1/account/password/
     * Change the caller&#39;s password
     * The caller proves the current password first. A successful change revokes every session, including this one, so the caller signs in again with the new password.
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param passwordChange 
     * @return [Unit]
     */
    @POST("api/v1/account/password/")
    suspend fun accountPasswordChange(@Body passwordChange: PasswordChange): Response<Unit>

    /**
     * POST api/v1/account/phone/confirm/
     * Confirm the code and move the account to the new number
     * Every session ends, this one included: the phone is how this account signs in, so a session issued to the old identity does not outlive it.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param challengeVerify 
     * @return [Profile]
     */
    @POST("api/v1/account/phone/confirm/")
    suspend fun accountPhoneChangeConfirm(@Body challengeVerify: ChallengeVerify): Response<Profile>

    /**
     * POST api/v1/account/phone/start/
     * Start moving the account to another phone number
     * The code is sent to the new number, which is what proves the caller can receive on it. The account is not changed until the code is confirmed.
     * Responses:
     *  - 202: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param phoneChangeStart 
     * @return [ChallengeAccepted]
     */
    @POST("api/v1/account/phone/start/")
    suspend fun accountPhoneChangeStart(@Body phoneChangeStart: PhoneChangeStart): Response<ChallengeAccepted>

    /**
     * DELETE api/v1/account/profile/image/
     * Remove the profile picture of the caller
     * The picture on the account: one at a time, replaced or removed.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [Profile]
     */
    @DELETE("api/v1/account/profile/image/")
    suspend fun accountProfileImageDelete(): Response<Profile>

    /**
     * PUT api/v1/account/profile/image/
     * Upload or replace the profile picture of the caller
     * Sent as multipart/form-data. The server decodes the file, enforces byte and pixel limits, re-encodes to JPEG and strips metadata — a photograph carries where it was taken. The declared extension and MIME type are not trusted.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param file 
     * @return [Profile]
     */
    @Multipart
    @PUT("api/v1/account/profile/image/")
    suspend fun accountProfileImageUpdate(@Part file: MultipartBody.Part): Response<Profile>

    /**
     * GET api/v1/account/profile/
     * Retrieve the profile of the caller
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [Profile]
     */
    @GET("api/v1/account/profile/")
    suspend fun accountProfileRetrieve(): Response<Profile>

    /**
     * PATCH api/v1/account/profile/
     * Update the display name or profile province of the caller
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param patchedProfilePatch  (optional)
     * @return [Profile]
     */
    @PATCH("api/v1/account/profile/")
    suspend fun accountProfileUpdate(@Body patchedProfilePatch: PatchedProfilePatch? = null): Response<Profile>

    /**
     * PUT api/v1/account/push-token/
     * Register or refresh this device&#39;s push token
     * Idempotent. A new token from the same session replaces the previous one. The token is tied to the calling session and deactivated when that session ends.
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param pushTokenRegister 
     * @return [Unit]
     */
    @PUT("api/v1/account/push-token/")
    suspend fun accountPushTokenRegister(@Body pushTokenRegister: PushTokenRegister): Response<Unit>

    /**
     * POST api/v1/account/push-token/unregister/
     * Stop sending pushes to a device token
     * Idempotent: an unknown or already inactive token also answers 204.
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param pushToken 
     * @return [Unit]
     */
    @POST("api/v1/account/push-token/unregister/")
    suspend fun accountPushTokenUnregister(@Body pushToken: PushToken): Response<Unit>

    /**
     * GET api/v1/account/ratings/
     * List the ratings written by the caller
     * 
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [AccountRatingList]
     */
    @GET("api/v1/account/ratings/")
    suspend fun accountRatingsList(): Response<AccountRatingList>

}
