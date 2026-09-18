package com.servacode.directory.feature.province

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.Province
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProvinceUiState {
    data object Loading : ProvinceUiState
    data class Content(val provinces: List<Province>, val stale: Boolean) : ProvinceUiState
    data class Error(val message: String) : ProvinceUiState
}

@HiltViewModel
class ProvinceViewModel @Inject constructor(
    private val useCase: ProvinceUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<ProvinceUiState>(ProvinceUiState.Loading)
    val state: StateFlow<ProvinceUiState> = _state.asStateFlow()
    private var loading: Job? = null

    init { refresh() }

    fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            useCase.provinces().collect { loaded ->
                _state.value = when (loaded) {
                    is Loaded.Cached -> ProvinceUiState.Content(loaded.value, stale = false)
                    is Loaded.Fresh -> ProvinceUiState.Content(loaded.value, stale = false)
                    is Loaded.Stale -> ProvinceUiState.Content(loaded.value, stale = true)
                    is Loaded.Failed -> ProvinceUiState.Error(AppErrorText.of(loaded.error))
                }
            }
        }
    }

    fun select(id: String, onSelected: () -> Unit) {
        viewModelScope.launch {
            useCase.select(id)
            onSelected()
        }
    }
}
