package com.servacode.directory.feature.bootstrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BootstrapUiState {
    data object Loading : BootstrapUiState
    data class Ready(val selectedProvinceId: String?) : BootstrapUiState
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
            _state.value = when (val result = bootstrap()) {
                is BootstrapResult.Ready -> BootstrapUiState.Ready(result.selectedProvinceId)
                is BootstrapResult.Failed -> BootstrapUiState.Error(result.reason)
            }
        }
    }
}
