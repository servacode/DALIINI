package com.servacode.directory.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.model.DirectoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Owns the map's state across views; never the MapView itself. */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loadMap: MapUseCase,
    private val startCamera: MapStartUseCase,
) : ViewModel() {
    private val focusFacilityId = savedStateHandle.toRoute<DirectoryRoute.Map>().focusFacilityId
    private val _state = MutableStateFlow(
        MapUiState(selectedFacilityId = savedStateHandle[SELECTED] ?: focusFacilityId),
    )
    val state: StateFlow<MapUiState> = _state.asStateFlow()
    private var loading: Job? = null

    private var lastViewport: MapViewport? = null

    init {
        viewModelScope.launch {
            val restored = savedStateHandle.get<DoubleArray>(CAMERA)?.toCamera()
            val camera = startCamera(focusFacilityId, restored)
            _state.update { it.copy(camera = camera, cameraResolved = true) }
        }
        viewModelScope.launch {
            val categories = loadMap.categories()
            _state.update { it.copy(categories = categories) }
        }
    }

    /**
     * A filter changed: the markers on screen answer a question the user has just changed, so
     * the same viewport is asked again. The camera is not touched — changing what is shown
     * must never move the map out from under the person looking at it.
     */
    fun filter(change: (MapFilters) -> MapFilters) {
        val filters = change(_state.value.filters)
        _state.update { it.copy(filters = filters, loadedViewport = null) }
        val viewport = lastViewport ?: return
        loading?.cancel()
        loading = viewModelScope.launch {
            loadMap(viewport, filters).onSuccess { facilities ->
                _state.update { it.copy(facilities = facilities, loadedViewport = viewport) }
            }
        }
    }

    /** The map stopped moving: keep where it is, and fetch markers only for an area not loaded yet. */
    fun cameraIdle(camera: MapCamera, viewport: MapViewport) {
        savedStateHandle[CAMERA] = camera.toSaved()
        _state.update { it.copy(camera = camera) }
        lastViewport = viewport
        if (!_state.value.needsLoad(viewport)) return
        loading?.cancel()
        val filters = _state.value.filters
        loading = viewModelScope.launch {
            loadMap(viewport, filters).onSuccess { facilities ->
                _state.update { it.copy(facilities = facilities, loadedViewport = viewport) }
            }
        }
    }

    /** The user opened a facility from the map; it stays selected when they come back. */
    fun facilityChosen(id: String) {
        savedStateHandle[SELECTED] = id
        _state.update { it.copy(selectedFacilityId = id) }
    }

    private companion object {
        const val CAMERA = "map.camera"
        const val SELECTED = "map.selected"
    }
}
