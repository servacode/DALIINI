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

    /** Whether the profile shows "منشآتي" or the invitation to join. */
    suspend fun ownsFacility(): Boolean = repository.ownsFacility()

    /** The fields the backend accepts, and no others: a display name and a province. */
    suspend fun update(displayName: String, provinceId: String?): Result<AccountProfile> =
        repository.update(displayName, provinceId)
    suspend fun logout() = repository.logout()
}

class DeleteAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteAccount()
}
