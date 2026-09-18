package com.servacode.directory.feature.ratings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.UserRating
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RatingsUiState {
    data object Loading : RatingsUiState
    data class Content(
        val values: List<UserRating>,
        val savingFacilityId: String? = null,
        val message: String? = null,
    ) : RatingsUiState
    data object Error : RatingsUiState
}

@HiltViewModel
class RatingsViewModel @Inject constructor(
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
                    _state.value = RatingsUiState.Content(current, message = AppErrorText.of(it.toAppError()))
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
