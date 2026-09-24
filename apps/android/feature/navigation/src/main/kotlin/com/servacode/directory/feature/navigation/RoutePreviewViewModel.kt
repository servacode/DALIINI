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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The way there, before anyone follows it.
 *
 * Pressing "الطريق" on a facility used to start live navigation at once — voice, camera
 * following, the lot — for someone who only wanted to know how far it is. This computes the
 * route once, shows it whole, and leaves starting to the user.
 *
 * The three modes are three routes, not one route with three labels: changing the mode asks the
 * engine again, because walking and driving do not use the same streets and a one-way road that
 * costs a driver ten minutes costs someone on foot nothing.
 */
data class RoutePreviewUiState(
    val route: NavigationRoute? = null,
    val origin: MapPoint? = null,
    val profile: RoutingProfile = RoutingProfile.DRIVING,
    val loading: Boolean = true,
    /** A code, not a sentence: the screen decides the words. */
    val failure: String? = null,
) {
    val canStart: Boolean get() = route != null
}

@HiltViewModel
class RoutePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routingProvider: RoutingProvider,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<DirectoryRoute.RoutePreview>()
    val destination = MapPoint(route.latitude, route.longitude)
    val facilityId: String = route.facilityId

    private val _state = MutableStateFlow(RoutePreviewUiState())
    val state: StateFlow<RoutePreviewUiState> = _state.asStateFlow()

    /** The one in flight, so a quick change of mode cannot be answered by the older of the two. */
    private var inFlight: Job? = null

    init {
        compute()
    }

    /**
     * Ask again on foot, on a motorcycle or by car.
     *
     * Choosing the mode that is already shown does nothing at all: the route on screen is
     * already its answer, and asking for it twice would only make the screen blink.
     */
    fun selectProfile(profile: RoutingProfile) {
        if (profile == _state.value.profile) return
        _state.update { it.copy(profile = profile) }
        compute()
    }

    fun compute() {
        val profile = _state.value.profile
        inFlight?.cancel()
        _state.value = RoutePreviewUiState(profile = profile, loading = true)
        inFlight = viewModelScope.launch {
            val origin = when (val current = locationProvider.current()) {
                is LocationResult.Available -> current.fix
                LocationResult.PermissionDenied -> {
                    _state.value = RoutePreviewUiState(
                        profile = profile,
                        loading = false,
                        failure = LOCATION_PERMISSION_REQUIRED,
                    )
                    return@launch
                }
                LocationResult.Unavailable -> locationProvider.lastKnown()
            }
            if (origin == null) {
                _state.value = RoutePreviewUiState(
                    profile = profile,
                    loading = false,
                    failure = LOCATION_UNAVAILABLE,
                )
                return@launch
            }
            val from = MapPoint(origin.latitude, origin.longitude)
            runCatching { routingProvider.route(from, destination, profile) }
                .onSuccess { found ->
                    _state.value = RoutePreviewUiState(
                        route = found,
                        origin = from,
                        profile = profile,
                        loading = false,
                    )
                }
                .onFailure { cause ->
                    _state.value = RoutePreviewUiState(
                        origin = from,
                        profile = profile,
                        loading = false,
                        // The engine's own reason, kept: "nothing near you is on a road a car
                        // may use" and "the engine is not running" are not the same news.
                        failure = (cause as? RoutingException)?.failure?.name ?: ROUTING_UNAVAILABLE,
                    )
                }
        }
    }

    private companion object {
        const val LOCATION_PERMISSION_REQUIRED = "LOCATION_PERMISSION_REQUIRED"
        const val LOCATION_UNAVAILABLE = "LOCATION_UNAVAILABLE"
        const val ROUTING_UNAVAILABLE = "ROUTING_UNAVAILABLE"
    }
}
