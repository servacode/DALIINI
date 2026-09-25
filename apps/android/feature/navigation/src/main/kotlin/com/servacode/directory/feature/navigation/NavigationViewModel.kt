package com.servacode.directory.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RoutingException
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.maps.RoutingProvider
import com.servacode.directory.core.model.DirectoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class NavigationUiState(
    val navigation: NavigationState = NavigationState.Idle,
    val warning: String? = null,
    /** The way the person is travelling, which they may change without leaving the screen. */
    val profile: RoutingProfile = RoutingProfile.DRIVING,
    /**
     * True while a new route is being fetched for a mode just chosen.
     *
     * Separate from [NavigationState.Routing] on purpose: the route already on screen stays
     * there while the next one is fetched, so choosing a mode does not empty the map.
     */
    val switching: Boolean = false,
    /** Which way the person is facing, for the mark that stands where they are. */
    val bearingDegrees: Float? = null,
    /** True when the readings are made up rather than measured, so the screen can say so. */
    val simulated: Boolean = false,
) {
    /** The route as it is right now, whichever state holds it. */
    val progress: NavigationProgress?
        get() = when (navigation) {
            is NavigationState.Navigating -> navigation.progress
            is NavigationState.Rerouting -> navigation.progress
            else -> null
        }

    val route: NavigationRoute?
        get() = progress?.route ?: (navigation as? NavigationState.Arrived)?.route
}

@HiltViewModel
class NavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routingProvider: RoutingProvider,
    private val locationProvider: LocationProvider,
    private val voice: NavigationVoice,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<DirectoryRoute.BuiltInNavigation>()
    private val destination = MapPoint(route.latitude, route.longitude)

    /**
     * Whether this trip is being driven by made-up readings.
     *
     * A demonstration, offered only by a debug build, so that guidance — the map following, the
     * maneuvers advancing, the voice speaking — can be watched without anyone getting into a car.
     */
    private val simulated = route.simulated

    private val engine = NavigationEngine()
    private val planner = NavigationVoicePlanner()
    private val _state = MutableStateFlow(
        NavigationUiState(
            profile = runCatching { RoutingProfile.valueOf(route.profile) }
                .getOrDefault(RoutingProfile.DRIVING),
            simulated = simulated,
        ),
    )
    // Internal like the state it carries: only this module's screen reads it.
    internal val state: StateFlow<NavigationUiState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var routeJob: Job? = null

    /**
     * The last place the person was known to be.
     *
     * Kept so that changing the travel mode is one request to the routing engine and nothing
     * else. Asking the receiver again costs up to eight seconds of "جارٍ حساب المسار…" for a
     * position that has not moved since the question was last asked.
     */
    private var origin: MapPoint? = null

    init {
        start()
    }

    fun retry() {
        locationJob?.cancel()
        routeJob?.cancel()
        planner.reset()
        origin = null
        start()
    }

    /** Walk it, ride it or drive it — the same destination, a different set of streets. */
    fun selectProfile(profile: RoutingProfile) {
        if (profile == _state.value.profile) return
        _state.update { it.copy(profile = profile) }
        planner.reset()
        val from = origin
        if (from == null) {
            start()
            return
        }
        computeRoute(from, profile, switching = true)
    }

    private fun start() {
        viewModelScope.launch {
            _state.update { it.copy(navigation = engine.routing(), warning = null) }
            when (val current = locationProvider.current()) {
                is LocationResult.Available -> {
                    val from = MapPoint(current.fix.latitude, current.fix.longitude)
                    origin = from
                    computeRoute(from, _state.value.profile, switching = false)
                }
                LocationResult.PermissionDenied -> fail("LOCATION_PERMISSION_REQUIRED")
                LocationResult.Unavailable -> {
                    val last = locationProvider.lastKnown()
                    if (last == null) {
                        fail("LOCATION_UNAVAILABLE")
                    } else {
                        val from = MapPoint(last.latitude, last.longitude)
                        origin = from
                        computeRoute(from, _state.value.profile, switching = false)
                    }
                }
            }
        }
    }

    private fun fail(reason: String) {
        _state.update {
            it.copy(navigation = NavigationState.Error(reason), switching = false)
        }
    }

    private fun computeRoute(from: MapPoint, profile: RoutingProfile, switching: Boolean) {
        routeJob?.cancel()
        // Only the very first route empties the screen. A mode chosen afterwards keeps the way
        // that is already drawn until the next one is ready, so the map never blinks.
        _state.update {
            if (switching) it.copy(switching = true, warning = null) else it.copy(warning = null)
        }
        routeJob = viewModelScope.launch {
            runCatching { routingProvider.route(from, destination, profile) }
                .onSuccess { found ->
                    publish(engine.start(found, from, System.currentTimeMillis()))
                    _state.update { it.copy(switching = false) }
                    if (simulated) driveTheRoute(found) else collectLocationUpdates()
                }
                .onFailure { cause ->
                    engine.fail((cause as? RoutingException)?.failure?.name ?: "ROUTING_UNAVAILABLE")
                    _state.update {
                        it.copy(navigation = engine.currentState(), switching = false)
                    }
                }
        }
    }

    /**
     * Walk the route that was just computed, as though someone were on it.
     *
     * Played faster than life so a trip across a city can be watched in a couple of minutes;
     * the readings themselves are spaced as real ones are, so everything that times itself by
     * seconds — which is all of the voice guidance — behaves exactly as it would on the road.
     */
    private fun driveTheRoute(found: NavigationRoute) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val profile = _state.value.profile
            val fixes = NavigationReplay.fixes(found.geometry, NavigationReplay.speedFor(profile))
            // Played fast enough that any trip can be watched in under a minute, and never
            // slower than five times life: a walk across a neighbourhood is an hour of
            // readings, and nobody watches an hour to see whether the voice speaks.
            val seconds = fixes.size * NavigationReplay.DEFAULT_STEP_SECONDS
            val speed = maxOf(PLAYBACK_SPEED, seconds / DEMO_SECONDS)
            val interval = (NavigationReplay.DEFAULT_STEP_SECONDS * 1000 / speed).toLong()
            fixes.forEach { fix ->
                delay(interval)
                handleLocation(
                    fix.point,
                    fix.accuracyMeters,
                    fix.speedMetersPerSecond,
                    fix.bearingDegrees,
                )
                if (_state.value.navigation is NavigationState.Arrived) return@launch
            }
        }
    }

    private fun collectLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationProvider.updates().collectLatest { result ->
                when (result) {
                    is LocationResult.Available -> {
                        origin = MapPoint(result.fix.latitude, result.fix.longitude)
                        // The reading's own accuracy travels with it: the engine weighs a vague
                        // fix differently from a sharp one rather than believing both exactly.
                        handleLocation(origin!!, result.fix.accuracyMeters)
                    }
                    LocationResult.PermissionDenied -> fail("LOCATION_PERMISSION_REQUIRED")
                    LocationResult.Unavailable ->
                        _state.update { it.copy(warning = "LOCATION_TEMPORARILY_UNAVAILABLE") }
                }
            }
        }
    }

    private suspend fun handleLocation(
        point: MapPoint,
        accuracyMeters: Float = 0f,
        speedMetersPerSecond: Float? = null,
        bearingDegrees: Float? = null,
    ) {
        val update = engine.update(point, System.currentTimeMillis(), accuracyMeters)
        publish(update, speedMetersPerSecond = speedMetersPerSecond, bearing = bearingDegrees)
        if (!update.rerouteRequired) return
        val profile = _state.value.profile
        runCatching { routingProvider.route(point, destination, profile) }
            .onSuccess { rerouted ->
                publish(
                    engine.applyReroute(rerouted, point, System.currentTimeMillis(), accuracyMeters),
                    speedMetersPerSecond = speedMetersPerSecond,
                    bearing = bearingDegrees,
                )
            }
            .onFailure {
                publish(
                    engine.rerouteFailed(System.currentTimeMillis()),
                    warning = "REROUTE_NETWORK_FAILED",
                )
            }
    }

    private fun publish(
        update: NavigationUpdate,
        warning: String? = null,
        speedMetersPerSecond: Float? = null,
        bearing: Float? = null,
    ) {
        _state.update {
            it.copy(
                navigation = update.state,
                warning = warning,
                switching = false,
                bearingDegrees = bearing ?: it.bearingDegrees,
            )
        }
        // What is due now, and nothing else. The planner decides whether this reading crossed a
        // threshold; speaking on every advance is what used to make guidance arrive after the
        // junction and never before it.
        planner.onState(update.state, speedMetersPerSecond).forEach(voice::say)
    }

    override fun onCleared() {
        voice.stop()
        super.onCleared()
    }

    private companion object {
        /** How much faster than life a demonstration runs, at the very least. */
        const val PLAYBACK_SPEED = 5.0

        /** And how long the whole of it may take to watch, however long the trip is. */
        const val DEMO_SECONDS = 45.0
    }
}
