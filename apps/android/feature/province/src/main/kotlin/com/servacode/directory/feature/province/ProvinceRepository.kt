package com.servacode.directory.feature.province

import com.servacode.directory.core.database.PublicCacheDataSource
import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.network.PublicApiBoundary
import javax.inject.Inject

class ProvinceRepository @Inject constructor(
    private val cache: PublicCacheDataSource,
    private val api: PublicApiBoundary,
    private val preferences: PreferencesRepository,
) {
    suspend fun load(): Pair<List<Province>, Boolean> {
        val cached = cache.provinces()
        return runCatching {
            val remote = api.provinces()
            cache.putProvinces(remote)
            remote
        }.fold(
                onSuccess = { it to false },
                onFailure = { cached to true },
            )
    }

    suspend fun select(id: String) = preferences.selectProvince(id)
}
