package com.servacode.directory.feature.home

import com.servacode.directory.core.database.PublicCacheDataSource
import com.servacode.directory.core.datastore.PreferencesRepository
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

sealed interface HomeLoadResult {
    data class Content(val snapshot: HomeSnapshot, val stale: Boolean) : HomeLoadResult
    data object ProvinceRequired : HomeLoadResult
    data object Unavailable : HomeLoadResult
}

class HomeRepository @Inject constructor(
    private val cache: PublicCacheDataSource,
    private val api: PublicApiBoundary,
    private val preferences: PreferencesRepository,
    private val locationProvider: LocationProvider,
) {
    suspend fun load(): HomeLoadResult {
        val provinceId = preferences.values.first().selectedProvinceId
            ?: return HomeLoadResult.ProvinceRequired
        val cached = cache.home(provinceId)
        val fix = when (val location = locationProvider.current()) {
            is LocationResult.Available -> location.fix
            else -> locationProvider.lastKnown()
        }
        return runCatching {
            val remote = api.home(provinceId, fix?.latitude, fix?.longitude)
            cache.putHome(remote)
            remote
        }.fold(
            onSuccess = { HomeLoadResult.Content(it, stale = false) },
            onFailure = {
                cached?.let { value -> HomeLoadResult.Content(value, stale = true) }
                    ?: HomeLoadResult.Unavailable
            },
        )
    }
}
