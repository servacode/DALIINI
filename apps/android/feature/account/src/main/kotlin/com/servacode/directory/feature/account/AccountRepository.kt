package com.servacode.directory.feature.account

import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.network.PublicApiBoundary
import javax.inject.Inject

class AccountRepository @Inject constructor(
    private val api: PublicApiBoundary,
) {
    suspend fun profile(): Result<AccountProfile> = runCatching { api.profile() }
}
