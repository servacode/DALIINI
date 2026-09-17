package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.datastore.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface BootstrapResult {
    data class Ready(val selectedProvinceId: String?) : BootstrapResult
    data class Failed(val reason: String) : BootstrapResult
}

interface BootstrapRepository {
    suspend fun initialize(): BootstrapResult
}

class DefaultBootstrapRepository @Inject constructor(
    private val preferences: PreferencesRepository,
) : BootstrapRepository {
    override suspend fun initialize(): BootstrapResult = runCatching {
        BootstrapResult.Ready(preferences.values.first().selectedProvinceId)
    }.getOrElse {
        BootstrapResult.Failed("BOOTSTRAP_STORAGE_UNAVAILABLE")
    }
}
