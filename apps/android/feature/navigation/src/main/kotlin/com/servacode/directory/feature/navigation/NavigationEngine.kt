package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteManeuver

/**
 * The numbers this engine judges by.
 *
 * Two of them come from what RahalGo's driver navigation learned in the field and this app had
 * not: one reading must never decide anything on its own, and a reading's own accuracy is part
 * of what it means. See `docs/design/RAHALGO-NAVIGATION-AUDIT.md`.
 */
internal data class NavigationThresholds(
    val offRouteMeters: Double = 55.0,
    val arrivalMeters: Double = 25.0,
    val maneuverAdvanceMeters: Double = 35.0,
    val rerouteCooldownMillis: Long = 12_000L,
    /**
     * How many readings in a row must agree that the route was left.
     *
     * A phone under a bridge or between tall buildings reports a position tens of metres away
     * and corrects itself a second later; rerouting on that throws away a good route and starts
     * talking over the driver.
     */
    val offRouteConfirmations: Int = 3,
    /**
     * A reading less accurate than this says too little about where anyone is, so it decides
     * nothing: it cannot end a trip and it cannot confirm leaving the route.
     */
    val unusableAccuracyMeters: Float = 60f,
)

internal data class NavigationProgress(
    val route: NavigationRoute,
    val location: MapPoint,
    val maneuverIndex: Int,
    val remainingDistanceMeters: Double,
    val remainingDurationSeconds: Double,
    val offRouteDistanceMeters: Double,
    /**
     * How far the next turn still is.
     *
     * Straight-line to its point rather than measured along the road: it is the same figure the
     * engine already advances the maneuver by, it is never longer than the road, and voice
     * guidance that speaks a little early is kinder than voice guidance that speaks late.
     */
    val distanceToManeuverMeters: Double = 0.0,
) {
    val maneuver: RouteManeuver? = route.maneuvers.getOrNull(maneuverIndex)
}

internal sealed interface NavigationState {
    data object Idle : NavigationState
    data object Routing : NavigationState
    data class Navigating(val progress: NavigationProgress) : NavigationState
    data class Rerouting(val progress: NavigationProgress) : NavigationState
    data class Arrived(val route: NavigationRoute) : NavigationState
    data class Error(val reason: String) : NavigationState
}

internal data class NavigationUpdate(
    val state: NavigationState,
    val rerouteRequired: Boolean = false,
)

internal class NavigationEngine(
    private val thresholds: NavigationThresholds = NavigationThresholds(),
) {
    private var state: NavigationState = NavigationState.Idle
    private var lastRerouteAtMillis: Long = Long.MIN_VALUE
    private var progressBeforeReroute: NavigationProgress? = null

    /** How many readings in a row have now agreed that the route was left. */
    private var offRouteStreak: Int = 0

    fun currentState(): NavigationState = state

    fun routing(): NavigationState {
        state = NavigationState.Routing
        return state
    }

    fun start(
        route: NavigationRoute,
        location: MapPoint,
        nowMillis: Long,
        accuracyMeters: Float = 0f,
    ): NavigationUpdate {
        lastRerouteAtMillis = Long.MIN_VALUE
        progressBeforeReroute = null
        offRouteStreak = 0
        return updateForRoute(
            route,
            location,
            nowMillis,
            allowReroute = false,
            accuracyMeters = accuracyMeters,
        )
    }

    fun update(location: MapPoint, nowMillis: Long, accuracyMeters: Float = 0f): NavigationUpdate {
        val currentProgress = when (val current = state) {
            is NavigationState.Navigating -> current.progress
            is NavigationState.Rerouting -> current.progress
            else -> return NavigationUpdate(state)
        }
        return updateForRoute(
            currentProgress.route,
            location,
            nowMillis,
            allowReroute = state !is NavigationState.Rerouting,
            previousIndex = currentProgress.maneuverIndex,
            accuracyMeters = accuracyMeters,
        )
    }

    fun applyReroute(
        route: NavigationRoute,
        location: MapPoint,
        nowMillis: Long,
        accuracyMeters: Float = 0f,
    ): NavigationUpdate {
        progressBeforeReroute = null
        offRouteStreak = 0
        return updateForRoute(
            route,
            location,
            nowMillis,
            allowReroute = false,
            accuracyMeters = accuracyMeters,
        )
    }

    fun rerouteFailed(nowMillis: Long): NavigationUpdate {
        lastRerouteAtMillis = nowMillis
        val previous = progressBeforeReroute
        progressBeforeReroute = null
        state = if (previous == null) {
            NavigationState.Error("Reroute failed without an active route.")
        } else {
            NavigationState.Navigating(previous)
        }
        return NavigationUpdate(state)
    }

    fun fail(reason: String): NavigationState {
        state = NavigationState.Error(reason)
        return state
    }

    fun reset(): NavigationState {
        state = NavigationState.Idle
        progressBeforeReroute = null
        lastRerouteAtMillis = Long.MIN_VALUE
        offRouteStreak = 0
        return state
    }

    private fun updateForRoute(
        route: NavigationRoute,
        location: MapPoint,
        nowMillis: Long,
        allowReroute: Boolean,
        previousIndex: Int = 0,
        accuracyMeters: Float = 0f,
    ): NavigationUpdate {
        // A reading that cannot place the driver within the arrival radius cannot end the trip,
        // and one that is merely vague widens the radius rather than being believed exactly.
        val trustworthy = accuracyMeters <= thresholds.unusableAccuracyMeters
        val arrivalRadius = thresholds.arrivalMeters + accuracyMeters.coerceAtLeast(0f)
        if (trustworthy && GeoMath.distanceMeters(location, route.destination) <= arrivalRadius) {
            offRouteStreak = 0
            state = NavigationState.Arrived(route)
            return NavigationUpdate(state)
        }
        val offRoute = GeoMath.distanceToPolylineMeters(location, route.geometry)
        val maneuverIndex = advanceManeuver(route, location, previousIndex)
        // How much of the road being travelled is still ahead. The index names the turn to
        // announce next, so the leg under the wheels belongs to the turn before it and is
        // not in the sum below — which is why the trip used to lose its first leg the
        // moment it began, and said 6.3 km of a way it had just offered as 10.10.
        val toManeuver = route.maneuvers.getOrNull(maneuverIndex)
            ?.let { GeoMath.distanceMeters(location, it.point) }
            ?: 0.0
        val progress = NavigationProgress(
            route = route,
            location = location,
            maneuverIndex = maneuverIndex,
            remainingDistanceMeters = remainingDistance(route, maneuverIndex, toManeuver),
            remainingDurationSeconds = remainingDuration(route, maneuverIndex, toManeuver),
            offRouteDistanceMeters = offRoute,
            distanceToManeuverMeters = toManeuver,
        )
        val cooldownElapsed = lastRerouteAtMillis == Long.MIN_VALUE ||
            nowMillis - lastRerouteAtMillis >= thresholds.rerouteCooldownMillis
        // Evidence, not one reading. A vague reading neither adds to the evidence nor clears it.
        offRouteStreak = when {
            !trustworthy -> offRouteStreak
            offRoute > thresholds.offRouteMeters -> offRouteStreak + 1
            else -> 0
        }
        val confirmed = offRouteStreak >= thresholds.offRouteConfirmations
        if (allowReroute && confirmed && cooldownElapsed) {
            progressBeforeReroute = progress
            lastRerouteAtMillis = nowMillis
            offRouteStreak = 0
            state = NavigationState.Rerouting(progress)
            return NavigationUpdate(state, rerouteRequired = true)
        }
        state = NavigationState.Navigating(progress)
        return NavigationUpdate(state)
    }

    private fun advanceManeuver(
        route: NavigationRoute,
        location: MapPoint,
        previousIndex: Int,
    ): Int {
        if (route.maneuvers.isEmpty()) return 0
        var index = previousIndex.coerceIn(0, route.maneuvers.lastIndex)
        while (index < route.maneuvers.lastIndex) {
            val distance = GeoMath.distanceMeters(location, route.maneuvers[index].point)
            if (distance > thresholds.maneuverAdvanceMeters) break
            index += 1
        }
        return index
    }

    /** What is left: the road still to run before the next turn, and every leg after it. */
    private fun remainingDistance(route: NavigationRoute, index: Int, toManeuver: Double): Double {
        if (route.maneuvers.isEmpty()) return route.distanceMeters
        return toManeuver + route.maneuvers.drop(index).sumOf(RouteManeuver::distanceMeters)
    }

    /**
     * The same, in time.
     *
     * The part still to run before the next turn is timed at the pace the engine gave the leg it
     * belongs to, rather than at an average of the whole trip: the leg before a turn is the one
     * whose speed the estimate is actually about.
     */
    private fun remainingDuration(route: NavigationRoute, index: Int, toManeuver: Double): Double {
        if (route.maneuvers.isEmpty()) return route.durationSeconds
        val after = route.maneuvers.drop(index).sumOf(RouteManeuver::durationSeconds)
        val leg = route.maneuvers.getOrNull(index - 1) ?: return after
        if (leg.distanceMeters <= 0.0) return after
        return after + leg.durationSeconds * (toManeuver / leg.distanceMeters)
    }
}
