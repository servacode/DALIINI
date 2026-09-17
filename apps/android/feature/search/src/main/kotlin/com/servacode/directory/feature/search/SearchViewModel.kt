package com.servacode.directory.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.FacilitySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val values: List<FacilitySummary>) : SearchUiState
    data object Error : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val search: SearchUseCase,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun updateQuery(value: String) {
        _query.value = value
        job?.cancel()
        val normalized = SearchQuery.normalize(value)
        if (normalized.isEmpty()) {
            _state.value = SearchUiState.Idle
            return
        }
        job = viewModelScope.launch {
            delay(300)
            _state.value = SearchUiState.Loading
            _state.value = search(normalized).fold(
                onSuccess = { SearchUiState.Results(it) },
                onFailure = { SearchUiState.Error },
            )
        }
    }
}
