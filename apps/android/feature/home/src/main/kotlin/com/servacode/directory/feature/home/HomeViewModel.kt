package com.servacode.directory.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object ProvinceRequired : HomeUiState
    data class Content(val snapshot: HomeSnapshot, val stale: Boolean) : HomeUiState
    data object Error : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val loadHome: HomeUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (event.scope.type == "province" && event.name.startsWith("public.")) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            _state.value = when (val result = loadHome()) {
                is HomeLoadResult.Content -> HomeUiState.Content(result.snapshot, result.stale)
                HomeLoadResult.ProvinceRequired -> HomeUiState.ProvinceRequired
                HomeLoadResult.Unavailable -> HomeUiState.Error
            }
        }
    }
}
