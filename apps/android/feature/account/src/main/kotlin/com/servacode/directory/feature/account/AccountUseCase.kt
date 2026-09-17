package com.servacode.directory.feature.account

import com.servacode.directory.core.model.AccountProfile
import javax.inject.Inject

class AccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(): Result<AccountProfile> = repository.profile()
}
