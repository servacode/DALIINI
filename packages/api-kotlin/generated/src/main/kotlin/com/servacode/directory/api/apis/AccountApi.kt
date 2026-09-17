package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.AccountDeletionRequested
import com.servacode.directory.api.models.AccountRatingList
import com.servacode.directory.api.models.DeletionRequest
import com.servacode.directory.api.models.DetailError
import com.servacode.directory.api.models.PatchedProfilePatch
import com.servacode.directory.api.models.Profile

interface AccountApi {
    /**
     * POST api/v1/account/deletion-request/
     * Request deletion of the account of the caller
     * Required by Play policy for any app that creates accounts. Ownership obligations and legally retained records are handled by the deletion policy.
     * Responses:
     *  - 202: 
     *  - 400: Request validation failed.
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
     *  - 400: Request validation failed.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param patchedProfilePatch  (optional)
     * @return [Profile]
     */
    @PATCH("api/v1/account/profile/")
    suspend fun accountProfileUpdate(@Body patchedProfilePatch: PatchedProfilePatch? = null): Response<Profile>

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
