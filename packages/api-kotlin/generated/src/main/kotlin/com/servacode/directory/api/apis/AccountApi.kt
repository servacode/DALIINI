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
import com.servacode.directory.api.models.DeletionRequest
import com.servacode.directory.api.models.PatchedProfilePatch
import com.servacode.directory.api.models.Profile
import com.servacode.directory.api.models.PushToken
import com.servacode.directory.api.models.PushTokenRegister

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
