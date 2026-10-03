package com.servacode.directory.feature.duty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.model.toAppError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DutyRosterUiState {
    data object Loading : DutyRosterUiState
    data object ProvinceRequired : DutyRosterUiState
    data class Content(val days: List<DutyDay>) : DutyRosterUiState {
        val isEmpty: Boolean get() = days.all { it.facilities.isEmpty() }
    }
    data class Error(val error: AppError) : DutyRosterUiState
}

/**
 * «المناوبات»: today, tomorrow or the week, as the site's `/duty` page shows them. Android's
 * navigation asks Hilt for the subclass in androidMain (DECISION-095).
 */
open class DutyRosterViewModel(
    private val repository: DutyRosterRepository,
) : ViewModel() {
    private val _range = MutableStateFlow(RosterRange.TODAY)
    val range: StateFlow<RosterRange> = _range.asStateFlow()
    private val _state = MutableStateFlow<DutyRosterUiState>(DutyRosterUiState.Loading)
    val state: StateFlow<DutyRosterUiState> = _state.asStateFlow()
    private var loading: Job? = null

    init { refresh() }

    fun select(range: RosterRange) {
        if (range == _range.value) return
        _range.value = range
        refresh()
    }

    fun refresh() {
        loading?.cancel()
        _state.value = DutyRosterUiState.Loading
        loading = viewModelScope.launch {
            _state.value = when (val result = repository.load(_range.value)) {
                null -> DutyRosterUiState.ProvinceRequired
                else -> result.fold(
                    onSuccess = { DutyRosterUiState.Content(it) },
                    onFailure = { DutyRosterUiState.Error(it.toAppError()) },
                )
            }
        }
    }
}
