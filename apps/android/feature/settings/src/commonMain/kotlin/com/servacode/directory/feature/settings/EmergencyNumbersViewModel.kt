package com.servacode.directory.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.EmergencyNumbers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EmergencyUiState {
    data object Loading : EmergencyUiState

    /**
     * [stale]: the platform could not be reached and these are the device's copy. [builtIn]: the
     * device had nothing, so these are the app's own few, to be checked with the authorities.
     */
    data class Content(val numbers: EmergencyNumbers, val stale: Boolean, val builtIn: Boolean) : EmergencyUiState
}

open class EmergencyNumbersViewModel(
    private val repository: EmergencyNumbersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val state: StateFlow<EmergencyUiState> = _state.asStateFlow()
    private var loading: Job? = null

    init { refresh() }

    fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            repository.load().collect { load ->
                _state.value = when (load) {
                    is EmergencyLoad.Cached -> EmergencyUiState.Content(load.numbers, stale = false, builtIn = false)
                    is EmergencyLoad.Fresh -> EmergencyUiState.Content(load.numbers, stale = false, builtIn = false)
                    is EmergencyLoad.Stale -> EmergencyUiState.Content(load.numbers, stale = true, builtIn = false)
                    is EmergencyLoad.BuiltIn -> EmergencyUiState.Content(load.numbers, stale = false, builtIn = true)
                }
            }
        }
    }
}
