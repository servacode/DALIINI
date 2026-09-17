package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteManeuver

internal data class NavigationThresholds(
    val offRouteMeters: Double = 55.0,
    val arrivalMeters: Double = 25.0,
    val maneuverAdvanceMeters: Double = 35.0,
    val rerouteCooldownMillis: Long = 12_000L,
)

internal data class NavigationProgress(
    val route: NavigationRoute,
    val location: MapPoint,
    val maneuverIndex: Int,
    val remainingDistanceMeters: Double,
    val remainingDurationSeconds: Double,
    val offRouteDistanceMeters: Double,
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

    fun currentState(): NavigationState = state

    fun routing(): NavigationState {
        state = NavigationState.Routing
        return state
    }

    fun start(route: NavigationRoute, location: MapPoint, nowMillis: Long): NavigationUpdate {
        lastRerouteAtMillis = Long.MIN_VALUE
        progressBeforeReroute = null
        return updateForRoute(route, location, nowMillis, allowReroute = false)
    }

    fun update(location: MapPoint, nowMillis: Long): NavigationUpdate {
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
        )
    }

    fun applyReroute(route: NavigationRoute, location: MapPoint, nowMillis: Long): NavigationUpdate {
        progressBeforeReroute = null
        return updateForRoute(route, location, nowMillis, allowReroute = false)
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
        return state
    }

    private fun updateForRoute(
        route: NavigationRoute,
        location: MapPoint,
        nowMillis: Long,
        allowReroute: Boolean,
        previousIndex: Int = 0,
    ): NavigationUpdate {
        if (GeoMath.distanceMeters(location, route.destination) <= thresholds.arrivalMeters) {
            state = NavigationState.Arrived(route)
            return NavigationUpdate(state)
        }
        val offRoute = GeoMath.distanceToPolylineMeters(location, route.geometry)
        val maneuverIndex = advanceManeuver(route, location, previousIndex)
        val progress = NavigationProgress(
            route = route,
            location = location,
            maneuverIndex = maneuverIndex,
            remainingDistanceMeters = remainingDistance(route, maneuverIndex),
            remainingDurationSeconds = remainingDuration(route, maneuverIndex),
            offRouteDistanceMeters = offRoute,
        )
        val cooldownElapsed = lastRerouteAtMillis == Long.MIN_VALUE ||
            nowMillis - lastRerouteAtMillis >= thresholds.rerouteCooldownMillis
        if (allowReroute && offRoute > thresholds.offRouteMeters && cooldownElapsed) {
            progressBeforeReroute = progress
            lastRerouteAtMillis = nowMillis
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

    private fun remainingDistance(route: NavigationRoute, index: Int): Double {
        if (route.maneuvers.isEmpty()) return route.distanceMeters
        return route.maneuvers.drop(index).sumOf(RouteManeuver::distanceMeters)
    }

    private fun remainingDuration(route: NavigationRoute, index: Int): Double {
        if (route.maneuvers.isEmpty()) return route.durationSeconds
        return route.maneuvers.drop(index).sumOf(RouteManeuver::durationSeconds)
    }
}
