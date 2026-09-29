package com.servacode.directory.core.testing

import com.servacode.directory.core.database.EmergencyNumbersCache
import com.servacode.directory.core.database.RecentlyViewedStore
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppException
import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.model.RecentFacility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** The backend of a deployment that has not added an endpoint yet. */
val missingEndpoint = AppException(AppError(AppError.Kind.NOT_FOUND, code = "NOT_FOUND", status = 404))

fun emergency(name: String, number: String, scope: EmergencyScope = EmergencyScope.NATIONAL, provinceId: String? = null) =
    EmergencyNumber(name, number, scope, provinceId)

/** The Room cache, in memory, with its semantics: national rows shared, province rows per id. */
class FakeEmergencyNumbersCache : EmergencyNumbersCache {
    val rows = mutableMapOf<String, List<EmergencyNumber>>()

    override suspend fun read(provinceId: String?): List<EmergencyNumber> =
        rows["national"].orEmpty() + (provinceId?.let { rows[it] }.orEmpty())

    override suspend fun write(provinceId: String?, values: List<EmergencyNumber>) {
        rows["national"] = values.filter { it.scope == EmergencyScope.NATIONAL }
        if (provinceId != null) rows[provinceId] = values.filter { it.scope == EmergencyScope.PROVINCE }
    }
}

/** «شوهدت مؤخراً» in memory, newest first, capped as the Room store is. */
class FakeRecentlyViewedStore : RecentlyViewedStore {
    val state = MutableStateFlow<List<RecentFacility>>(emptyList())

    override fun observe(): Flow<List<RecentFacility>> = state

    override suspend fun record(value: RecentFacility) {
        state.value = (listOf(value) + state.value.filter { it.id != value.id })
            .sortedByDescending { it.viewedAtEpochMillis }
            .take(RecentlyViewedStore.LIMIT)
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}
