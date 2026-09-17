package com.servacode.directory.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.PublicMapFacility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapViewport(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double,
) {
    init {
        require(west in -180.0..180.0 && east in -180.0..180.0)
        require(south in -90.0..90.0 && north in -90.0..90.0)
    }
}

sealed interface MapUiState {
    data object Idle : MapUiState
    data object Loading : MapUiState
    data class Content(val facilities: List<PublicMapFacility>) : MapUiState
    data object Error : MapUiState
}

@HiltViewModel
class MapViewModel @Inject constructor(
    private val loadMap: MapUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    fun viewportChanged(viewport: MapViewport) {
        viewModelScope.launch {
            _state.value = MapUiState.Loading
            _state.value = loadMap(viewport).fold(
                onSuccess = { MapUiState.Content(it) },
                onFailure = { MapUiState.Error },
            )
        }
    }
}
