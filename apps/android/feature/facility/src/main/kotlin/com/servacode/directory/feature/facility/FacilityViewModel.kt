package com.servacode.directory.feature.facility

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.FacilityDetail
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FacilityUiState {
    data object Loading : FacilityUiState
    data class Content(
        val value: FacilityDetail,
        val stale: Boolean,
        val signedIn: Boolean = false,
        val myRating: Int? = null,
        val ratingMessage: String? = null,
    ) : FacilityUiState
    data class Error(val message: String) : FacilityUiState
}

@HiltViewModel
class FacilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val facility: FacilityUseCase,
    private val invalidations: RealtimeInvalidationBus,
    private val session: SessionCoordinator,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DirectoryRoute.FacilityDetailRoute>().id
    private val _state = MutableStateFlow<FacilityUiState>(FacilityUiState.Loading)
    val state: StateFlow<FacilityUiState> = _state.asStateFlow()
    private var loading: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (RealtimeInvalidation.refreshesFacility(event, id)) refresh()
            }
        }
    }

    private fun signedIn() = session.state.value == SessionState.SIGNED_IN

    /**
     * Save or unsave the facility on screen.
     *
     * The heart fills at once and goes back if the backend refuses: the state shown belongs to
     * the account, and the backend is the one that holds it.
     */
    fun toggleFavorite() {
        val current = _state.value as? FacilityUiState.Content ?: return
        if (!current.signedIn) return
        val saved = current.value.summary.isFavorite
        _state.value = current.copy(
            value = current.value.copy(
                summary = current.value.summary.copy(isFavorite = !saved),
            ),
        )
        viewModelScope.launch {
            val result = runCatching {
                if (saved) facility.unsave(current.value.summary.id)
                else facility.save(current.value.summary.id)
            }
            result.onFailure { _state.value = current }
        }
    }

    private fun refresh() {
        loading?.cancel()
        loading = viewModelScope.launch {
            facility(id).collect { loaded ->
                val previous = _state.value as? FacilityUiState.Content
                _state.value = when (loaded) {
                    is Loaded.Cached -> content(loaded.value, stale = false, previous)
                    is Loaded.Fresh -> content(loaded.value, stale = false, previous)
                    is Loaded.Stale -> content(loaded.value, stale = true, previous)
                    is Loaded.Failed -> FacilityUiState.Error(AppErrorText.of(loaded.error))
                }
            }
            if (signedIn()) {
                facility.myRating(id).onSuccess { stars ->
                    (_state.value as? FacilityUiState.Content)?.let { _state.value = it.copy(myRating = stars) }
                }
            }
        }
    }

    private fun content(value: FacilityDetail, stale: Boolean, previous: FacilityUiState.Content?) =
        FacilityUiState.Content(value, stale, signedIn(), previous?.myRating)

    fun removeRating() {
        val current = _state.value as? FacilityUiState.Content ?: return
        viewModelScope.launch {
            facility.removeRating(id)
                .onSuccess {
                    _state.value = current.copy(myRating = null, ratingMessage = null)
                    // The average and count are the backend's; fetch them again.
                    refresh()
                }
                .onFailure { _state.value = current.copy(ratingMessage = AppErrorText.of(it.toAppError())) }
        }
    }

    fun rate(stars: Int) {
        val current = _state.value as? FacilityUiState.Content ?: return
        viewModelScope.launch {
            facility.rate(id, stars)
                .onSuccess { stored ->
                    _state.value = current.copy(myRating = stored, ratingMessage = null)
                    // The average and count are the backend's; fetch them again.
                    refresh()
                }
                .onFailure { _state.value = current.copy(ratingMessage = AppErrorText.of(it.toAppError())) }
        }
    }
}
