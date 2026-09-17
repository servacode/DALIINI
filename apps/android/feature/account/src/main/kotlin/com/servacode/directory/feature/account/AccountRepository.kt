package com.servacode.directory.feature.account

import com.servacode.directory.core.auth.AccessTokenStore
import com.servacode.directory.core.auth.RefreshTokenVault
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.network.PublicApiBoundary
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val accessTokens: AccessTokenStore,
    private val refreshTokens: RefreshTokenVault,
) {
    suspend fun profile(): Result<AccountProfile> = runCatching { api.profile() }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        api.requestAccountDeletion()
        accessTokens.set(null)
        refreshTokens.clear()
    }
}
