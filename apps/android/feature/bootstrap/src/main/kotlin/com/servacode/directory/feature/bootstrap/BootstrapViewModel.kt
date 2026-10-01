package com.servacode.directory.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BootstrapUiState {
    data object Loading : BootstrapUiState
    data class Ready(val selectedProvinceId: String?, val start: StartDestination) : BootstrapUiState
    data class Error(val code: String) : BootstrapUiState
}

@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val bootstrap: BootstrapUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<BootstrapUiState>(BootstrapUiState.Loading)
    val state: StateFlow<BootstrapUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // The work starts first and the wait runs beside it, so the splash is held for the
            // time it takes to read the mark and not a moment longer than that: a start that
            // takes more than MINIMUM_ON_SCREEN adds nothing, and one that takes less is not
            // a flash of a logo nobody saw.
            val started = async { bootstrap() }
            delay(MINIMUM_ON_SCREEN)
            _state.value = when (val result = started.await()) {
                is BootstrapResult.Ready -> BootstrapUiState.Ready(result.selectedProvinceId, result.start)
                is BootstrapResult.Failed -> BootstrapUiState.Error(result.reason)
            }
        }
    }

    private companion object {
        /** Long enough for the mark to grow and be read, in milliseconds. */
        const val MINIMUM_ON_SCREEN = 1200L
    }
}
