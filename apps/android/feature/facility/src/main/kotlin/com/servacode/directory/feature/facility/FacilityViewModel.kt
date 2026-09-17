package com.servacode.directory.feature.facility

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FacilityUiState {
    data object Loading : FacilityUiState
    data class Content(val value: FacilityDetail, val stale: Boolean) : FacilityUiState
    data object Error : FacilityUiState
}

@HiltViewModel
class FacilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadFacility: FacilityUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DirectoryRoute.FacilityDetailRoute>().id
    private val _state = MutableStateFlow<FacilityUiState>(FacilityUiState.Loading)
    val state: StateFlow<FacilityUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                val facilityEvent = event.name in setOf(
                    "public.facility.changed",
                    "public.facility.availability_changed",
                    "public.duty.changed",
                )
                if (facilityEvent && event.resourceId == id) refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.value = when (val result = loadFacility(id)) {
                is FacilityLoadResult.Content -> FacilityUiState.Content(result.value, result.stale)
                FacilityLoadResult.Unavailable -> FacilityUiState.Error
            }
        }
    }
}
