package com.servacode.directory.feature.settings

import com.servacode.directory.core.database.EmergencyNumbersCache
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.EmergencyNumber
import com.servacode.directory.core.model.EmergencyNumbers
import com.servacode.directory.core.model.EmergencyScope
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * The names of the few lines the app carries itself, read from this module's resources: words
 * the reader sees live in resources, never in code.
 */
data class BuiltInEmergencyLabels(val ambulance: String, val police: String, val fire: String)

/** What the emergency screen has to show, in the order it can show it. */
sealed interface EmergencyLoad {
    /** From the device, while the platform is being asked. */
    data class Cached(val numbers: EmergencyNumbers) : EmergencyLoad

    /** From the platform, and now on the device. */
    data class Fresh(val numbers: EmergencyNumbers) : EmergencyLoad

    /** The platform could not be reached; the device's copy stays, marked as such. */
    data class Stale(val numbers: EmergencyNumbers) : EmergencyLoad

    /** Nothing on the device and nothing from the platform: the few built-in numbers, with a warning. */
    data class BuiltIn(val numbers: EmergencyNumbers) : EmergencyLoad
}

/**
 * The country's and the province's emergency numbers, cache first.
 *
 * A number is the one piece of this app someone may need with no connection at all, so what the
 * platform last served stays on the device, and a failure never empties the screen. Only when
 * the device has never had the list and the platform cannot give it — offline, or a backend
 * that does not serve the list yet — does the app show the handful it carries itself, and says
 * to check them with the authorities.
 */
class EmergencyNumbersRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val cache: EmergencyNumbersCache,
    private val preferences: DirectoryPreferencesStore,
    private val labels: BuiltInEmergencyLabels,
) {
    fun load(): Flow<EmergencyLoad> = flow {
        val provinceId = runCatching { preferences.values.first().selectedProvinceId }.getOrNull()
        val cached = runCatching { cache.read(provinceId) }.getOrDefault(emptyList())
        if (cached.isNotEmpty()) emit(EmergencyLoad.Cached(group(cached)))
        val fresh = try {
            api.emergencyNumbers(provinceId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emit(fallback(cached))
            return@flow
        }
        if (fresh.isEmpty()) {
            // An empty answer is not a reason to forget what was known.
            emit(fallback(cached))
            return@flow
        }
        runCatching { cache.write(provinceId, fresh) }
        emit(EmergencyLoad.Fresh(group(fresh)))
    }

    /** The device's copy when there is one; only a device that never had the list gets the built-ins. */
    private fun fallback(cached: List<EmergencyNumber>): EmergencyLoad =
        if (cached.isNotEmpty()) EmergencyLoad.Stale(group(cached)) else EmergencyLoad.BuiltIn(builtIn(labels))

    companion object {
        fun group(values: List<EmergencyNumber>) = EmergencyNumbers(
            national = values.filter { it.scope == EmergencyScope.NATIONAL },
            province = values.filter { it.scope == EmergencyScope.PROVINCE },
        )

        /** Syria's three national lines, for a device that has never had the platform's list. */
        fun builtIn(labels: BuiltInEmergencyLabels) = EmergencyNumbers(
            national = listOf(
                EmergencyNumber(labels.ambulance, "110", EmergencyScope.NATIONAL),
                EmergencyNumber(labels.police, "112", EmergencyScope.NATIONAL),
                EmergencyNumber(labels.fire, "113", EmergencyScope.NATIONAL),
            ),
            province = emptyList(),
            builtIn = true,
        )
    }
}
