package com.servacode.directory.feature.facility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.FacilityReportReason
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Why a report did not go through, in the words the sheet uses. */
enum class ReportFailure {
    /** The backend's limit on reports from one sender (429). */
    THROTTLED,

    /** No answer at all. */
    OFFLINE,

    /** Anything else: the report can be tried again. */
    OTHER,
    ;

    companion object {
        fun of(error: AppError): ReportFailure = when (error.kind) {
            AppError.Kind.RATE_LIMITED -> THROTTLED
            AppError.Kind.OFFLINE -> OFFLINE
            else -> OTHER
        }
    }
}

data class FacilityReportUiState(
    val reason: FacilityReportReason? = null,
    val note: String = "",
    val sending: Boolean = false,
    val failure: ReportFailure? = null,
    /** Set once when the backend accepted the report; the screen thanks and closes, then consumes it. */
    val sent: Boolean = false,
) {
    val canSend: Boolean get() = reason != null && !sending
}

/**
 * The "report a problem" sheet on a facility's page.
 *
 * It works signed out: a report is about the facility, not about the reader. The facility is
 * passed in with [submit] rather than read from the route, so the sheet has no navigation of its
 * own to know about.
 */
@HiltViewModel
class FacilityReportViewModel @Inject constructor(
    private val report: ReportFacilityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(FacilityReportUiState())
    val state: StateFlow<FacilityReportUiState> = _state.asStateFlow()

    fun choose(reason: FacilityReportReason) {
        _state.value = _state.value.copy(reason = reason, failure = null)
    }

    /** The note is optional; anything past the backend's limit is not taken. */
    fun updateNote(value: String) {
        _state.value = _state.value.copy(note = value.take(FacilityRepository.REPORT_NOTE_MAX))
    }

    fun submit(facilityId: String) {
        val current = _state.value
        val reason = current.reason ?: return
        if (current.sending) return
        _state.value = current.copy(sending = true, failure = null)
        viewModelScope.launch {
            report(facilityId, reason, current.note)
                .onSuccess { _state.value = FacilityReportUiState(sent = true) }
                .onFailure {
                    _state.value = _state.value.copy(
                        sending = false,
                        failure = ReportFailure.of(it.toAppError()),
                    )
                }
        }
    }

    /** The thanks has been shown; the next sheet starts empty. */
    fun consumeSent() {
        _state.value = FacilityReportUiState()
    }

    /** The sheet was dismissed without sending: what was chosen is kept for a second try. */
    fun clearFailure() {
        _state.value = _state.value.copy(failure = null)
    }
}
