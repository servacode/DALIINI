package com.servacode.directory.feature.duty

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.network.DutyShiftInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DutyUiState {
    data object Loading : DutyUiState
    data class Content(val shifts: List<DutyShift>, val message: String? = null) : DutyUiState
    data object Error : DutyUiState
}

@HiltViewModel
class DutyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val load: LoadDutyUseCase,
    private val manage: ManageDutyUseCase,
) : ViewModel() {
    private val facilityId = savedStateHandle.toRoute<DirectoryRoute.Duty>().id
    private val _state = MutableStateFlow<DutyUiState>(DutyUiState.Loading)
    val state: StateFlow<DutyUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = load(facilityId).fold(
                onSuccess = { DutyUiState.Content(it) },
                onFailure = { DutyUiState.Error },
            )
        }
    }

    fun schedule(startsAt: Long, endsAt: Long) {
        if (!DutyValidator.isValid(startsAt, endsAt)) {
            _state.value = DutyUiState.Content(currentShifts(), "وقت المناوبة غير صالح")
            return
        }
        viewModelScope.launch {
            manage.create(facilityId, DutyShiftInput(startsAt, endsAt))
                .onSuccess { refresh() }
                .onFailure { report(it) }
        }
    }

    fun startNow(endsAt: Long) {
        schedule(System.currentTimeMillis(), endsAt)
    }

    fun endEarly(shift: DutyShift) {
        val now = System.currentTimeMillis()
        if (now <= shift.startsAtEpochMillis) return
        viewModelScope.launch {
            manage.update(
                facilityId,
                shift.id,
                DutyShiftInput(shift.startsAtEpochMillis, now),
            ).onSuccess { refresh() }.onFailure { report(it) }
        }
    }

    fun cancel(shiftId: String) {
        viewModelScope.launch { manage.delete(facilityId, shiftId).onSuccess { refresh() }.onFailure { report(it) } }
    }

    /** The backend's refusal, in its Arabic wording: overlap, category without duty, and so on. */
    private fun report(failure: Throwable) {
        _state.value = DutyUiState.Content(currentShifts(), AppErrorText.of(failure.toAppError()))
    }

    private fun currentShifts(): List<DutyShift> =
        (_state.value as? DutyUiState.Content)?.shifts.orEmpty()
}
