package com.servacode.directory.feature.directory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DirectoryUiState {
    data object Loading : DirectoryUiState
    data class Content(
        val values: List<FacilitySummary>,
        val filter: DirectoryFilter,
        val stale: Boolean,
    ) : DirectoryUiState
    data object ProvinceRequired : DirectoryUiState
    data object Error : DirectoryUiState
}

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadDirectory: DirectoryUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val categoryId = savedStateHandle.toRoute<DirectoryRoute.Directory>().categoryId
    private val _state = MutableStateFlow<DirectoryUiState>(DirectoryUiState.Loading)
    val state: StateFlow<DirectoryUiState> = _state.asStateFlow()
    private var filter = DirectoryFilter()

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (event.scope.type == "province" && event.name.startsWith("public.")) refresh()
            }
        }
    }

    fun setOpenNow(value: Boolean) { filter = filter.copy(openNow = value); refresh() }
    fun setDutyNow(value: Boolean) { filter = filter.copy(dutyNow = value); refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = DirectoryUiState.Loading
            _state.value = when (val result = loadDirectory(categoryId, filter)) {
                is DirectoryLoadResult.Content -> DirectoryUiState.Content(result.values, filter, result.stale)
                DirectoryLoadResult.ProvinceRequired -> DirectoryUiState.ProvinceRequired
                DirectoryLoadResult.Unavailable -> DirectoryUiState.Error
            }
        }
    }
}
