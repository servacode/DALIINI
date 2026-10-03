package com.servacode.directory.core.database

import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.RecentFacility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * «شوهدت مؤخراً»: the facilities opened on this device, newest first, at most [LIMIT].
 *
 * Public data only (a name, a category, when it was opened) and never sent anywhere; clearing it
 * removes every row.
 */
interface RecentlyViewedStore {
    fun observe(): Flow<List<RecentFacility>>

    /** Records a visit; the same facility moves to the top rather than appearing twice. */
    suspend fun record(value: RecentFacility)

    suspend fun clear()

    companion object {
        const val LIMIT = 20
    }
}

/** The emergency numbers last served for a province, for when the network is not there. */
interface EmergencyNumbersCache {
    /** Everything cached for [provinceId]: the national rows and that province's. Empty if none. */
    suspend fun read(provinceId: String?): List<EmergencyNumber>

    /** Replaces what is cached for [provinceId] (and the national rows) with [values]. */
    suspend fun write(provinceId: String?, values: List<EmergencyNumber>)
}

/**
 * Says when the home snapshot of a province was written, so what shows the same data outside
 * the app — the home-screen widget — can redraw from it without asking the backend itself.
 */
object CacheEvents {
    private val homeWritten = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Province ids, as their home snapshot is stored. */
    val homeSnapshots: SharedFlow<String> = homeWritten.asSharedFlow()

    fun homeWritten(provinceId: String) {
        homeWritten.tryEmit(provinceId)
    }
}
