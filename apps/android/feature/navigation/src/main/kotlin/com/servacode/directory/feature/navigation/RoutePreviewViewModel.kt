package com.servacode.directory.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RoutingProvider
import com.servacode.directory.core.model.DirectoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The way there, before anyone follows it.
 *
 * Pressing "الطريق" on a facility used to start live navigation at once — voice, camera
 * following, the lot — for someone who only wanted to know how far it is. This computes the
 * route once, shows it whole, and leaves starting to the user.
 */
data class RoutePreviewUiState(
    val route: NavigationRoute? = null,
    val origin: MapPoint? = null,
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

    init {
        compute()
    }

    fun compute() {
        _state.value = RoutePreviewUiState(loading = true)
        viewModelScope.launch {
            val origin = when (val current = locationProvider.current()) {
                is LocationResult.Available -> current.fix
                LocationResult.PermissionDenied -> {
                    _state.value = RoutePreviewUiState(
                        loading = false,
                        failure = "LOCATION_PERMISSION_REQUIRED",
                    )
                    return@launch
                }
                LocationResult.Unavailable -> locationProvider.lastKnown()
            }
            if (origin == null) {
                _state.value = RoutePreviewUiState(loading = false, failure = "LOCATION_UNAVAILABLE")
                return@launch
            }
            val from = MapPoint(origin.latitude, origin.longitude)
            runCatching { routingProvider.route(from, destination) }
                .onSuccess { found ->
                    _state.value = RoutePreviewUiState(route = found, origin = from, loading = false)
                }
                .onFailure {
                    _state.value = RoutePreviewUiState(
                        origin = from,
                        loading = false,
                        failure = "ROUTING_UNAVAILABLE",
                    )
                }
        }
    }
}
