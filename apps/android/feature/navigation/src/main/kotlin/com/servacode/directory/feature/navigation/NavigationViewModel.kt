package com.servacode.directory.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RoutingProvider
import com.servacode.directory.core.model.DirectoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class NavigationUiState(
    val navigation: NavigationState = NavigationState.Idle,
    val warning: String? = null,
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
    private val engine = NavigationEngine()
    private val _state = MutableStateFlow(NavigationUiState())
    // Internal like the state it carries: only this module's screen reads it.
    internal val state: StateFlow<NavigationUiState> = _state.asStateFlow()
    private var locationJob: Job? = null
    private var lastSpokenKey: String? = null

    init {
        start()
    }

    fun retry() {
        locationJob?.cancel()
        start()
    }

    private fun start() {
        viewModelScope.launch {
            _state.value = NavigationUiState(engine.routing())
            when (val current = locationProvider.current()) {
                is LocationResult.Available -> requestInitialRoute(current.fix.toPoint())
                LocationResult.PermissionDenied -> {
                    _state.value = NavigationUiState(
                        NavigationState.Error("LOCATION_PERMISSION_REQUIRED"),
                    )
                }
                LocationResult.Unavailable -> {
                    _state.value = NavigationUiState(
                        NavigationState.Error("LOCATION_UNAVAILABLE"),
                    )
                }
            }
        }
    }

    private suspend fun requestInitialRoute(origin: MapPoint) {
        runCatching { routingProvider.route(origin, destination) }
            .onSuccess { routeResult ->
                publish(engine.start(routeResult, origin, System.currentTimeMillis()))
                collectLocationUpdates()
            }
            .onFailure {
                engine.fail("ROUTING_UNAVAILABLE")
                _state.value = NavigationUiState(engine.currentState())
            }
    }

    private fun collectLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationProvider.updates().collectLatest { result ->
                when (result) {
                    is LocationResult.Available -> handleLocation(result.fix.toPoint())
                    LocationResult.PermissionDenied -> {
                        engine.fail("LOCATION_PERMISSION_REQUIRED")
                        _state.value = NavigationUiState(engine.currentState())
                    }
                    LocationResult.Unavailable -> {
                        _state.value = _state.value.copy(warning = "LOCATION_TEMPORARILY_UNAVAILABLE")
                    }
                }
            }
        }
    }

    private suspend fun handleLocation(point: MapPoint) {
        val update = engine.update(point, System.currentTimeMillis())
        publish(update)
        if (!update.rerouteRequired) return
        runCatching { routingProvider.route(point, destination) }
            .onSuccess { rerouted ->
                publish(engine.applyReroute(rerouted, point, System.currentTimeMillis()))
            }
            .onFailure {
                val fallback = engine.rerouteFailed(System.currentTimeMillis())
                publish(fallback, warning = "REROUTE_NETWORK_FAILED")
            }
    }

    private fun publish(update: NavigationUpdate, warning: String? = null) {
        _state.value = NavigationUiState(update.state, warning)
        val progress = when (val current = update.state) {
            is NavigationState.Navigating -> current.progress
            is NavigationState.Rerouting -> current.progress
            else -> null
        }
        val maneuver = progress?.maneuver ?: return
        val key = listOf(
            maneuver.kind,
            maneuver.modifier,
            maneuver.point.latitude,
            maneuver.point.longitude,
        ).joinToString(":")
        if (key != lastSpokenKey) {
            lastSpokenKey = key
            voice.speak(ArabicManeuverPhraseBuilder.phrase(maneuver))
        }
    }

    override fun onCleared() {
        voice.stop()
        super.onCleared()
    }

    private fun com.servacode.directory.core.location.LocationFix.toPoint() =
        MapPoint(latitude, longitude)
}
