package com.servacode.directory.core.network

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.inject.Singleton
import com.servacode.directory.core.model.PlaceResolution
import com.servacode.directory.core.model.ResolvedPlace
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs
import kotlin.time.Clock

/**
 * What to call the place the user is in.
 *
 * The platform answers this, not the device and not an external geocoder: the name has to be
 * the same name the lists and the filters are scoped by, or the header would say one thing
 * while the results meant another.
 *
 * The resolver exists because a map moves constantly and a position updates constantly, while
 * the *name* of a place changes only when the user actually travels. It asks the backend at
 * most once for any one neighbourhood-sized area, and at most once in [MINIMUM_INTERVAL_MS]
 * whatever happens, so panning a map or walking down a street costs nothing.
 *
 * A failure is not an error the user sees. The last answer stands, and if there has never been
 * one the caller falls back to the province the app already had.
 */
interface LocationNameResolver {
    /** The place for a coordinate, or null when it could not be resolved and nothing is known. */
    suspend fun resolve(latitude: Double, longitude: Double): ResolvedPlace?

    /** The last answer, without asking anything. */
    fun last(): ResolvedPlace?
}

@Singleton
class BackendLocationNameResolver(
    private val api: PublicApiBoundary,
    // Not a constructor default: a default value is invisible to Dagger, which would then look
    // for a binding of `() -> Long`. Tests pass a clock here; the app uses the one below.
    private val clock: () -> Long,
) : LocationNameResolver {
    @Inject constructor(api: PublicApiBoundary) : this(api, { Clock.System.now().toEpochMilliseconds() })

    private val mutex = Mutex()
    private var lastPlace: ResolvedPlace? = null
    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastAskedAt = 0L

    override fun last(): ResolvedPlace? = lastPlace

    override suspend fun resolve(latitude: Double, longitude: Double): ResolvedPlace? = mutex.withLock {
        val now = clock()
        if (!worthAsking(latitude, longitude, now)) return lastPlace
        lastAskedAt = now
        val answer = runCatching { api.resolvePlace(latitude, longitude) }.getOrNull()
            ?: return lastPlace
        if (answer.resolvedBy == PlaceResolution.NONE) {
            // Outside every province the platform serves: keep whatever was known before rather
            // than telling the user they are nowhere.
            return lastPlace
        }
        lastPlace = answer
        lastLatitude = latitude
        lastLongitude = longitude
        return answer
    }

    private fun worthAsking(latitude: Double, longitude: Double, now: Long): Boolean {
        if (lastPlace == null) return true
        if (now - lastAskedAt < MINIMUM_INTERVAL_MS) return false
        val previousLatitude = lastLatitude ?: return true
        val previousLongitude = lastLongitude ?: return true
        val moved = abs(latitude - previousLatitude) > SIGNIFICANT_DEGREES ||
            abs(longitude - previousLongitude) > SIGNIFICANT_DEGREES
        return moved
    }

    companion object {
        /**
         * Roughly 500 m at these latitudes. Less than that and the answer would be the same
         * neighbourhood, so asking again would spend a request to be told what is already known.
         */
        const val SIGNIFICANT_DEGREES = 0.005

        /** No more than one question a minute, however far the device has travelled. */
        const val MINIMUM_INTERVAL_MS = 60_000L
    }
}
