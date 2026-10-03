package com.servacode.directory.api.apis

import com.servacode.directory.api.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.servacode.directory.api.models.ApiError
import com.servacode.directory.api.models.ChallengeAccepted
import com.servacode.directory.api.models.ChallengeVerified
import com.servacode.directory.api.models.ChallengeVerify
import com.servacode.directory.api.models.Login
import com.servacode.directory.api.models.LogoutRequest
import com.servacode.directory.api.models.RecoveryReset
import com.servacode.directory.api.models.RecoveryStart
import com.servacode.directory.api.models.Refresh
import com.servacode.directory.api.models.RegisterComplete
import com.servacode.directory.api.models.RegisterStart
import com.servacode.directory.api.models.SessionCredentials
import com.servacode.directory.api.models.UserSessionList

interface AuthApi {
    /**
     * POST api/v1/auth/login/
     * Exchange phone and password for session credentials
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *
     * @param login 
     * @return [SessionCredentials]
     */
    @POST("api/v1/auth/login/")
    suspend fun authLogin(@Body login: Login): Response<SessionCredentials>

    /**
     * POST api/v1/auth/logout/
     * Revoke one session
     * 
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param logoutRequest 
     * @return [Unit]
     */
    @POST("api/v1/auth/logout/")
    suspend fun authLogout(@Body logoutRequest: LogoutRequest): Response<Unit>

    /**
     * POST api/v1/auth/logout-all/
     * Revoke every session belonging to the caller
     * 
     * Responses:
     *  - 204: No response body
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [Unit]
     */
    @POST("api/v1/auth/logout-all/")
    suspend fun authLogoutAll(): Response<Unit>

    /**
     * POST api/v1/auth/recovery/reset/
     * Set a new password using a verified recovery challenge
     * A successful reset revokes every existing session for that user.
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param recoveryReset 
     * @return [Unit]
     */
    @POST("api/v1/auth/recovery/reset/")
    suspend fun authRecoveryReset(@Body recoveryReset: RecoveryReset): Response<Unit>

    /**
     * POST api/v1/auth/recovery/start/
     * Start password recovery by requesting an OTP challenge
     * 
     * Responses:
     *  - 202: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 422: The code cannot be delivered to this number at all; `code` is OTP_RECIPIENT_INVALID. Asking again for the same number will not help.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *  - 503: The code could not be sent just now; `code` is OTP_DELIVERY_UNAVAILABLE. The same request may succeed in a little while.
     *
     * @param recoveryStart 
     * @return [ChallengeAccepted]
     */
    @POST("api/v1/auth/recovery/start/")
    suspend fun authRecoveryStart(@Body recoveryStart: RecoveryStart): Response<ChallengeAccepted>

    /**
     * POST api/v1/auth/recovery/verify/
     * Verify the recovery OTP code
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *
     * @param challengeVerify 
     * @return [ChallengeVerified]
     */
    @POST("api/v1/auth/recovery/verify/")
    suspend fun authRecoveryVerify(@Body challengeVerify: ChallengeVerify): Response<ChallengeVerified>

    /**
     * POST api/v1/auth/refresh/
     * Rotate the refresh secret and issue a new access token
     * The supplied secret is rotated on every successful call. Replaying a secret outside the short concurrency grace window is treated as compromise and revokes every session belonging to the user.
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *
     * @param refresh 
     * @return [SessionCredentials]
     */
    @POST("api/v1/auth/refresh/")
    suspend fun authRefresh(@Body refresh: Refresh): Response<SessionCredentials>

    /**
     * POST api/v1/auth/register/complete/
     * Set the password and open the first session
     * 
     * Responses:
     *  - 201: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 409: The request conflicts with the current state or with a domain rule.
     *
     * @param registerComplete 
     * @return [SessionCredentials]
     */
    @POST("api/v1/auth/register/complete/")
    suspend fun authRegisterComplete(@Body registerComplete: RegisterComplete): Response<SessionCredentials>

    /**
     * POST api/v1/auth/register/start/
     * Start registration by requesting an OTP challenge
     * Accepts 09XXXXXXXX, +9639XXXXXXXX or 009639XXXXXXXX and normalises to the canonical form. The OTP code is delivered by the configured provider and is never returned in the response.
     * Responses:
     *  - 202: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 422: The code cannot be delivered to this number at all; `code` is OTP_RECIPIENT_INVALID. Asking again for the same number will not help.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *  - 503: The code could not be sent just now; `code` is OTP_DELIVERY_UNAVAILABLE. The same request may succeed in a little while.
     *
     * @param registerStart 
     * @return [ChallengeAccepted]
     */
    @POST("api/v1/auth/register/start/")
    suspend fun authRegisterStart(@Body registerStart: RegisterStart): Response<ChallengeAccepted>

    /**
     * POST api/v1/auth/register/verify/
     * Verify the registration OTP code
     * 
     * Responses:
     *  - 200: 
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 429: Rate limit exceeded for this endpoint; see the `Retry-After` header.
     *
     * @param challengeVerify 
     * @return [ChallengeVerified]
     */
    @POST("api/v1/auth/register/verify/")
    suspend fun authRegisterVerify(@Body challengeVerify: ChallengeVerify): Response<ChallengeVerified>

    /**
     * DELETE api/v1/auth/sessions/{session_id}/
     * Revoke a specific session of the caller
     * 
     * Responses:
     *  - 204: No response body
     *  - 400: Request validation failed; `code` is VALIDATION_ERROR and `details` is populated.
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @param sessionId 
     * @return [Unit]
     */
    @DELETE("api/v1/auth/sessions/{session_id}/")
    suspend fun authSessionRevoke(@Path("session_id") sessionId: java.util.UUID): Response<Unit>

    /**
     * GET api/v1/auth/sessions/
     * List the sessions and devices of the caller
     * Session secrets are never returned, only metadata and revocation state.
     * Responses:
     *  - 200: 
     *  - 401: No valid access token was supplied.
     *  - 403: Authenticated, but the caller lacks the required permission or membership.
     *
     * @return [UserSessionList]
     */
    @GET("api/v1/auth/sessions/")
    suspend fun authSessionsList(): Response<UserSessionList>

}
