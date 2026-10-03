package com.servacode.directory.feature.ratings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.model.UserRating
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RatingsUiState {
    data object Loading : RatingsUiState
    data class Content(
        val values: List<UserRating>,
        val savingFacilityId: String? = null,
        val failure: AppError? = null,
    ) : RatingsUiState
    data object Error : RatingsUiState
}

/**
 * The user's own ratings, on both platforms. Android's navigation asks Hilt for the subclass in
 * androidMain; the iPhone makes this one with its graph's use case (DECISION-095).
 */
open class RatingsViewModel(
    private val ratings: RatingsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<RatingsUiState>(RatingsUiState.Loading)
    val state: StateFlow<RatingsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = RatingsUiState.Loading
            _state.value = ratings.list().fold(
                onSuccess = { RatingsUiState.Content(it) },
                onFailure = { RatingsUiState.Error },
            )
        }
    }

    fun update(facilityId: String, stars: Int) {
        viewModelScope.launch {
            val current = (_state.value as? RatingsUiState.Content)?.values.orEmpty()
            _state.value = RatingsUiState.Content(current, savingFacilityId = facilityId)
            ratings.update(facilityId, stars)
                .onSuccess { refresh() }
                .onFailure {
                    _state.value = RatingsUiState.Content(current, failure = it.toAppError())
                }
        }
    }

    fun delete(facilityId: String) {
        viewModelScope.launch {
            ratings.delete(facilityId)
            refresh()
        }
    }
}
