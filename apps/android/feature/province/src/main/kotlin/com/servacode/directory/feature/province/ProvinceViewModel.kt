package com.servacode.directory.feature.province

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.Province
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProvinceUiState {
    data object Loading : ProvinceUiState
    data class Content(val provinces: List<Province>, val stale: Boolean) : ProvinceUiState
    data object Error : ProvinceUiState
}

@HiltViewModel
class ProvinceViewModel @Inject constructor(
    private val useCase: ProvinceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<ProvinceUiState>(ProvinceUiState.Loading)
    val state: StateFlow<ProvinceUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ProvinceUiState.Loading
            val (values, stale) = useCase.load()
            _state.value = if (values.isEmpty()) ProvinceUiState.Error else ProvinceUiState.Content(values, stale)
        }
    }

    fun select(id: String, onSelected: () -> Unit) {
        viewModelScope.launch {
            useCase.select(id)
            onSelected()
        }
    }
}
