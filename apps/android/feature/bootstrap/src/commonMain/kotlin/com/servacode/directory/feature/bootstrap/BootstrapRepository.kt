package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface BootstrapResult {
    data class Ready(val selectedProvinceId: String?, val start: StartDestination) : BootstrapResult
    data class Failed(val reason: String) : BootstrapResult
}

interface BootstrapRepository {
    suspend fun initialize(): BootstrapResult
}

class DefaultBootstrapRepository @Inject constructor(
    private val preferences: DirectoryPreferencesStore,
) : BootstrapRepository {
    override suspend fun initialize(): BootstrapResult = runCatching {
        val values = preferences.values.first()
        BootstrapResult.Ready(values.selectedProvinceId, values.startDestination())
    }.getOrElse {
        BootstrapResult.Failed("BOOTSTRAP_STORAGE_UNAVAILABLE")
    }
}
