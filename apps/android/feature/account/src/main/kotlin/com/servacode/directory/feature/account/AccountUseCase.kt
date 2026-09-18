package com.servacode.directory.feature.account

import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.Province
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    val session: StateFlow<SessionState> get() = repository.state
    suspend operator fun invoke(): Result<AccountProfile> = repository.profile()
    suspend fun provinces(): Result<List<Province>> = repository.provinces()
    suspend fun changeProvince(provinceId: String): Result<AccountProfile> = repository.changeProvince(provinceId)
    suspend fun logout() = repository.logout()
}

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteAccount()
}
