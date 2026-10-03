package com.servacode.directory.feature.duty

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.DutyPresets
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.DutyShiftInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DutyUiState {
    data object Loading : DutyUiState
    data class Content(
        val shifts: List<DutyShift>,
        /** The backend's own refusal, as it came; the screen reads its sentence. */
        val failure: AppError? = null,
        /**
         * The app's own objection to the times typed: a problem rather than a sentence, so the
         * words live in the resources where a second language can reach them.
         */
        val problem: DutyProblem? = null,
        /** The facility's temporary closures, to check a new shift against. */
        val closures: List<TemporaryClosure> = emptyList(),
    ) : DutyUiState
    data object Error : DutyUiState
}

/** Times to put in the form: a preset, or the day a gap nudge named. Consumed once shown. */
data class DutyDraft(val startsAt: Long, val endsAt: Long)

@HiltViewModel
class DutyViewModel(
    private val facilityId: String,
    initialDate: String?,
    private val load: LoadDutyUseCase,
    private val manage: ManageDutyUseCase,
    private val clock: () -> Long,
) : ViewModel() {
    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        load: LoadDutyUseCase,
        manage: ManageDutyUseCase,
    ) : this(
        savedStateHandle.toRoute<DirectoryRoute.Duty>().id,
        savedStateHandle.toRoute<DirectoryRoute.Duty>().date,
        load,
        manage,
        System::currentTimeMillis,
    )

    private val _state = MutableStateFlow<DutyUiState>(DutyUiState.Loading)
    val state: StateFlow<DutyUiState> = _state.asStateFlow()

    /** A gap nudge opens the roster with that night already filled in. */
    private val _draft = MutableStateFlow(DutyPresets.parseDate(initialDate)?.let { DutyPresets.night(it).toDraft() })
    val draft: StateFlow<DutyDraft?> = _draft.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val shifts = load(facilityId)
            // Closures are advisory here: without them the backend still refuses a clash.
            val closures = load.closures(facilityId).getOrDefault(emptyList())
            _state.value = shifts.fold(
                onSuccess = { DutyUiState.Content(it, closures = closures) },
                onFailure = { DutyUiState.Error },
            )
        }
    }

    /** «مناوبة الليلة»: tonight 20:00 to tomorrow 08:00, into the form to confirm. */
    fun presetTonight() {
        _draft.value = DutyPresets.tonight(today()).toDraft()
    }

    /** «غداً»: tomorrow night, the same hours. */
    fun presetTomorrow() {
        _draft.value = DutyPresets.tomorrow(today()).toDraft()
    }

    fun draftShown() {
        _draft.value = null
    }

    fun schedule(startsAt: Long, endsAt: Long) {
        val current = _state.value as? DutyUiState.Content
        val problem = DutyConflicts.check(
            startsAt,
            endsAt,
            current?.shifts.orEmpty(),
            current?.closures.orEmpty(),
            clock(),
        )
        if (problem != null) {
            _state.value = content().copy(problem = problem, failure = null)
            return
        }
        viewModelScope.launch {
            manage.create(facilityId, DutyShiftInput(startsAt, endsAt))
                .onSuccess { refresh() }
                .onFailure { report(it) }
        }
    }

    fun startNow(endsAt: Long) {
        schedule(clock(), endsAt)
    }

    fun endEarly(shift: DutyShift) {
        val now = clock()
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

    /** The backend's refusal, by its code: overlap, a closure, a category without duty. */
    private fun report(failure: Throwable) {
        _state.value = content().copy(failure = failure.toAppError(), problem = null)
    }

    private fun content(): DutyUiState.Content =
        _state.value as? DutyUiState.Content ?: DutyUiState.Content(emptyList())

    private fun today() = DamascusTime.localDateTime(clock()).date

    private fun Pair<Long, Long>.toDraft() = DutyDraft(first, second)
}
