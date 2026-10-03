package com.servacode.directory.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.OwnerFacilityInsights
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OwnerInsightsUiState {
    data object Loading : OwnerInsightsUiState
    data class Content(val insights: OwnerFacilityInsights) : OwnerInsightsUiState

    /** The window passed with nothing in it: said in words, not as three zeros. */
    data class Empty(val windowDays: Int) : OwnerInsightsUiState
    data class Error(val error: AppError) : OwnerInsightsUiState
}

/**
 * The statistics card on the owner's management screen.
 *
 * Its own view model, so a failure here never takes the rest of the management screen with it:
 * the numbers are a view on the facility, not a condition for managing it.
 */
@HiltViewModel
class OwnerInsightsViewModel @Inject constructor(
    private val load: LoadOwnerInsightsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<OwnerInsightsUiState>(OwnerInsightsUiState.Loading)
    val state: StateFlow<OwnerInsightsUiState> = _state.asStateFlow()
    private var facilityId: String? = null
    private var loading: Job? = null

    /** Load for [id]; asking again for the same facility while it loads or has loaded does nothing. */
    fun show(id: String) {
        if (id == facilityId && _state.value !is OwnerInsightsUiState.Error) return
        facilityId = id
        refresh()
    }

    fun refresh() {
        val id = facilityId ?: return
        loading?.cancel()
        _state.value = OwnerInsightsUiState.Loading
        loading = viewModelScope.launch {
            _state.value = load(id).fold(
                onSuccess = { insights ->
                    if (insights.isEmpty) OwnerInsightsUiState.Empty(insights.windowDays)
                    else OwnerInsightsUiState.Content(insights)
                },
                onFailure = { OwnerInsightsUiState.Error(it.toAppError()) },
            )
        }
    }
}
