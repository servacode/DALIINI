package com.servacode.directory.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
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
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class NavigationUiState(
    val navigation: NavigationState = NavigationState.Idle,
    val warning: String? = null,
    /** True when the readings are made up rather than measured, so the screen can say so. */
    val simulated: Boolean = false,
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routingProvider: RoutingProvider,
    private val locationProvider: LocationProvider,
    private val voice: NavigationVoice,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<DirectoryRoute.BuiltInNavigation>()
    private val destination = MapPoint(route.latitude, route.longitude)
    private val profile = runCatching { RoutingProfile.valueOf(route.profile) }
        .getOrDefault(RoutingProfile.DRIVING)

    /**
     * Whether this trip is being driven by made-up readings.
     *
     * A demonstration, offered only by a debug build, so that guidance — the map following, the
     * maneuvers advancing, the voice speaking — can be watched without anyone getting into a car.
     */
    private val simulated = route.simulated

    private val engine = NavigationEngine()
    private val planner = NavigationVoicePlanner()
    private val _state = MutableStateFlow(NavigationUiState(simulated = simulated))
    // Internal like the state it carries: only this module's screen reads it.
    internal val state: StateFlow<NavigationUiState> = _state.asStateFlow()
    private var locationJob: Job? = null

    init {
        start()
    }

    fun retry() {
        locationJob?.cancel()
        planner.reset()
        start()
    }

    private fun start() {
        viewModelScope.launch {
            _state.value = NavigationUiState(engine.routing(), simulated = simulated)
            when (val current = locationProvider.current()) {
                is LocationResult.Available -> requestInitialRoute(current.fix.toPoint())
                LocationResult.PermissionDenied -> fail("LOCATION_PERMISSION_REQUIRED")
                LocationResult.Unavailable -> fail("LOCATION_UNAVAILABLE")
            }
        }
    }

    private fun fail(reason: String) {
        _state.value = NavigationUiState(NavigationState.Error(reason), simulated = simulated)
    }

    private suspend fun requestInitialRoute(origin: MapPoint) {
        runCatching { routingProvider.route(origin, destination, profile) }
            .onSuccess { found ->
                publish(engine.start(found, origin, System.currentTimeMillis()))
                if (simulated) driveTheRoute(found) else collectLocationUpdates()
            }
            .onFailure {
                engine.fail("ROUTING_UNAVAILABLE")
                _state.value = NavigationUiState(engine.currentState(), simulated = simulated)
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
            val fixes = NavigationReplay.fixes(found.geometry)
            val interval = (NavigationReplay.DEFAULT_STEP_SECONDS * 1000 / PLAYBACK_SPEED).toLong()
            fixes.forEach { fix ->
                delay(interval)
                handleLocation(fix.point, fix.accuracyMeters, fix.speedMetersPerSecond)
                if (_state.value.navigation is NavigationState.Arrived) return@launch
            }
        }
    }

    private fun collectLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationProvider.updates().collectLatest { result ->
                when (result) {
                    is LocationResult.Available ->
                        // The reading's own accuracy travels with it: the engine weighs a vague
                        // fix differently from a sharp one rather than believing both exactly.
                        handleLocation(result.fix.toPoint(), result.fix.accuracyMeters)
                    LocationResult.PermissionDenied -> {
                        engine.fail("LOCATION_PERMISSION_REQUIRED")
                        _state.value = NavigationUiState(engine.currentState(), simulated = simulated)
                    }
                    LocationResult.Unavailable -> {
                        _state.value = _state.value.copy(warning = "LOCATION_TEMPORARILY_UNAVAILABLE")
                    }
                }
            }
        }
    }

    private suspend fun handleLocation(
        point: MapPoint,
        accuracyMeters: Float = 0f,
        speedMetersPerSecond: Float? = null,
    ) {
        val update = engine.update(point, System.currentTimeMillis(), accuracyMeters)
        publish(update, speedMetersPerSecond = speedMetersPerSecond)
        if (!update.rerouteRequired) return
        runCatching { routingProvider.route(point, destination, profile) }
            .onSuccess { rerouted ->
                publish(
                    engine.applyReroute(rerouted, point, System.currentTimeMillis(), accuracyMeters),
                    speedMetersPerSecond = speedMetersPerSecond,
                )
            }
            .onFailure {
                val fallback = engine.rerouteFailed(System.currentTimeMillis())
                publish(fallback, warning = "REROUTE_NETWORK_FAILED")
            }
    }

    private fun publish(
        update: NavigationUpdate,
        warning: String? = null,
        speedMetersPerSecond: Float? = null,
    ) {
        _state.value = NavigationUiState(update.state, warning, simulated)
        // What is due now, and nothing else. The planner decides whether this reading crossed a
        // threshold; speaking on every advance is what used to make guidance arrive after the
        // junction and never before it.
        planner.onState(update.state, speedMetersPerSecond).forEach { voice.speak(it.text) }
    }

    override fun onCleared() {
        voice.stop()
        super.onCleared()
    }

    private fun com.servacode.directory.core.location.LocationFix.toPoint() =
        MapPoint(latitude, longitude)

    private companion object {
        /** How much faster than life a demonstration runs. */
        const val PLAYBACK_SPEED = 5.0
    }
}
