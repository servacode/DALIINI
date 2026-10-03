package com.servacode.directory.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HoursConfirmationUiState {
    /** Nothing to ask: confirmed this week, not published, no hours, or no endpoint. */
    data object Hidden : HoursConfirmationUiState

    data class Due(val sending: Boolean = false, val failure: AppError? = null) : HoursConfirmationUiState

    /** Said once, where the question was. */
    data object Confirmed : HoursConfirmationUiState
}

/**
 * The weekly «تأكيد أوقات الدوام» card on the management screen. Its own view model, like the
 * statistics, so a refusal here never takes the rest of the screen with it.
 */
@HiltViewModel
class HoursConfirmationViewModel(
    private val confirm: ConfirmHoursUseCase,
    private val clock: () -> Long,
) : ViewModel() {
    @Inject constructor(confirm: ConfirmHoursUseCase) : this(confirm, System::currentTimeMillis)

    private val _state = MutableStateFlow<HoursConfirmationUiState>(HoursConfirmationUiState.Hidden)
    val state: StateFlow<HoursConfirmationUiState> = _state.asStateFlow()
    private var facilityId: String? = null

    /** Decides for [facility]; a reload of the same facility after confirming keeps the thanks. */
    fun show(facility: OwnerFacilityDetail) {
        val id = facility.summary.id
        if (id == facilityId && _state.value == HoursConfirmationUiState.Confirmed) return
        facilityId = id
        _state.value = if (HoursConfirmationCard.shows(facility, clock())) {
            HoursConfirmationUiState.Due()
        } else {
            HoursConfirmationUiState.Hidden
        }
    }

    fun confirm() {
        val id = facilityId ?: return
        val current = _state.value as? HoursConfirmationUiState.Due ?: return
        if (current.sending) return
        _state.value = HoursConfirmationUiState.Due(sending = true)
        viewModelScope.launch {
            _state.value = confirm(id).fold(
                onSuccess = { HoursConfirmationUiState.Confirmed },
                onFailure = { failure ->
                    if (HoursConfirmationCard.withdrawsOn(failure)) {
                        HoursConfirmationUiState.Hidden
                    } else {
                        HoursConfirmationUiState.Due(failure = failure.toAppError())
                    }
                },
            )
        }
    }
}
