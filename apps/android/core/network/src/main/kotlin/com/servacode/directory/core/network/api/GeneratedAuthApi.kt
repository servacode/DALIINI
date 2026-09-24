package com.servacode.directory.core.network.api

import com.servacode.directory.api.apis.AuthApi
import com.servacode.directory.api.models.ChallengeVerify
import com.servacode.directory.api.models.Login
import com.servacode.directory.api.models.LogoutRequest
import com.servacode.directory.api.models.RecoveryReset
import com.servacode.directory.api.models.RecoveryStart
import com.servacode.directory.api.models.Refresh
import com.servacode.directory.api.models.RegisterComplete
import com.servacode.directory.api.models.RegisterStart
import com.servacode.directory.core.auth.RefreshGateway
import com.servacode.directory.core.auth.RefreshRejectedException
import com.servacode.directory.core.auth.SessionTokens
import com.servacode.directory.core.model.AccountSession
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.AuthChallenge
import com.servacode.directory.core.network.AuthApiBoundary
import java.util.UUID

private const val PLATFORM = "ANDROID"

/**
 * [AuthApiBoundary] over the generated client.
 *
 * Two clients, on purpose. Sign-in, registration, recovery and refresh go through [anonymous],
 * which has no refresh authenticator: a wrong password must come back as a wrong password,
 * not trigger a refresh and a retry. Logout and session management need the caller's access
 * token and go through [authorized].
 */
class GeneratedAuthApi(
    anonymous: GeneratedClient,
    authorized: GeneratedClient,
    private val deviceName: String,
) : AuthApiBoundary {
    private val open by lazy { anonymous.create<AuthApi>() }
    private val signedIn by lazy { authorized.create<AuthApi>() }

    override suspend fun login(phone: String, password: String): SessionTokens = call {
        open.authLogin(Login(phone = phone, password = password, platform = PLATFORM, deviceName = deviceName))
    }.toDomain()

    override suspend fun registerStart(phone: String, provinceId: String): AuthChallenge =
        call {
            open.authRegisterStart(
                RegisterStart(phone = phone, provinceId = UUID.fromString(provinceId)),
            )
        }.toDomain()

    override suspend fun registerVerify(challengeId: String, code: String) {
        val verified = call {
            open.authRegisterVerify(ChallengeVerify(challengeId = UUID.fromString(challengeId), code = code))
        }
        if (!verified.verified) throw AppException(AppError(AppError.Kind.VALIDATION, code = "VALIDATION_ERROR"))
    }

    override suspend fun registerComplete(
        challengeId: String,
        displayName: String,
        password: String,
    ): SessionTokens = call {
        open.authRegisterComplete(
            RegisterComplete(
                challengeId = UUID.fromString(challengeId),
                displayName = displayName,
                password = password,
                platform = PLATFORM,
                deviceName = deviceName,
            ),
        )
    }.toDomain()

    override suspend fun recoveryStart(phone: String): AuthChallenge =
        call { open.authRecoveryStart(RecoveryStart(phone = phone)) }.toDomain()

    override suspend fun recoveryVerify(challengeId: String, code: String) {
        val verified = call {
            open.authRecoveryVerify(ChallengeVerify(challengeId = UUID.fromString(challengeId), code = code))
        }
        if (!verified.verified) throw AppException(AppError(AppError.Kind.VALIDATION, code = "VALIDATION_ERROR"))
    }

    override suspend fun recoveryReset(challengeId: String, password: String) {
        callForNoContent {
            open.authRecoveryReset(RecoveryReset(challengeId = UUID.fromString(challengeId), password = password))
        }
    }

    override suspend fun logout(sessionId: String) {
        callForNoContent { signedIn.authLogout(LogoutRequest(sessionId = UUID.fromString(sessionId))) }
    }

    override suspend fun sessions(): List<AccountSession> =
        call { signedIn.authSessionsList() }.items.map { it.toDomain() }

    override suspend fun revokeSession(sessionId: String) {
        callForNoContent { signedIn.authSessionRevoke(UUID.fromString(sessionId)) }
    }
}

/**
 * Rotates the refresh secret through the generated `authRefresh`.
 *
 * A 400 or 401 means the backend refused the secret, and the session ends. Anything else —
 * no network, a timeout, a 5xx — is transient: the secret was not spent, so it is kept.
 */
class GeneratedRefreshGateway(anonymous: GeneratedClient) : RefreshGateway {
    private val auth by lazy { anonymous.create<AuthApi>() }

    override suspend fun rotate(refreshToken: String): SessionTokens = try {
        call { auth.authRefresh(Refresh(refreshToken = refreshToken)) }.toDomain()
    } catch (failure: AppException) {
        when (failure.error.kind) {
            AppError.Kind.UNAUTHENTICATED, AppError.Kind.VALIDATION -> throw RefreshRejectedException()
            else -> throw failure
        }
    }
}
