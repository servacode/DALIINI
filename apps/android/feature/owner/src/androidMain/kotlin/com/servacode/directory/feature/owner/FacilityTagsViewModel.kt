package com.servacode.directory.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FacilityTagsUiState {
    /** Nothing to draw: not shown yet, or the category offers owners no specialty and no service. */
    data object Hidden : FacilityTagsUiState

    data object Loading : FacilityTagsUiState

    /** The choices could not be read; a retry asks again. */
    data class Error(val error: AppError) : FacilityTagsUiState

    data class Content(
        val form: FacilityTagsForm,
        val saving: Boolean = false,
        /** No connection: what the facility carries stays in sight and cannot change until it returns. */
        val offline: Boolean = false,
        val failure: FacilityTagsFailure? = null,
        /** Said once the backend accepted the choices, until the next change. */
        val saved: Boolean = false,
    ) : FacilityTagsUiState {
        val editable: Boolean get() = !saving && !offline
        val canSave: Boolean get() = editable && form.changed
    }
}

/**
 * «التخصصات والخدمات» on the owner's management screen.
 *
 * Its own view model, like the statistics and the weekly hours question, so that a refusal here
 * never takes the rest of the screen with it. The facility comes from the screen, which already
 * read it; the choices come from the owner configuration of its province.
 */
@HiltViewModel
class FacilityTagsViewModel @Inject constructor(
    private val choices: LoadTagChoicesUseCase,
    private val manage: ManageFacilityUseCase,
    network: NetworkMonitor,
) : ViewModel() {
    private val _state = MutableStateFlow<FacilityTagsUiState>(FacilityTagsUiState.Hidden)
    val state: StateFlow<FacilityTagsUiState> = _state.asStateFlow()
    private var facility: OwnerFacilityDetail? = null
    private var online = true
    private var loading: Job? = null

    init {
        viewModelScope.launch {
            network.online.collect { now ->
                online = now
                when (val current = _state.value) {
                    is FacilityTagsUiState.Content -> _state.value = current.copy(offline = !now)
                    // What failed for want of a connection is asked again as soon as there is one.
                    is FacilityTagsUiState.Error ->
                        if (now && current.error.kind == AppError.Kind.OFFLINE) load(keepTicks = false)
                    else -> Unit
                }
            }
        }
    }

    /**
     * The section for [facility], as the screen has it. The same facility read again, after any
     * change, keeps the owner's unsaved ticks and takes what the backend holds as saved.
     */
    fun show(facility: OwnerFacilityDetail) {
        val same = this.facility?.summary?.id == facility.summary.id
        this.facility = facility
        if (!same) {
            load(keepTicks = false)
            return
        }
        val current = _state.value as? FacilityTagsUiState.Content ?: return
        _state.value = current.copy(form = current.form.rebase(facility))
    }

    /**
     * Read the choices again: after a failure, or when the owner is told some are gone. Ticks not
     * saved yet stay where their choice is still offered.
     */
    fun retry() = load(keepTicks = true)

    fun toggleSpecialty(id: String) = edit { it.toggleSpecialty(id) }

    fun toggleService(id: String) = edit { it.toggleService(id) }

    /**
     * Sends the lists that changed and nothing else. The backend answers with the facility as it
     * now stands, which becomes what is saved.
     */
    fun save() {
        val current = _state.value as? FacilityTagsUiState.Content ?: return
        val id = facility?.summary?.id ?: return
        if (!current.canSave) return
        val patch = current.form.patch() ?: return
        _state.value = current.copy(saving = true, failure = null, saved = false)
        viewModelScope.launch {
            manage.patch(id, patch)
                .onSuccess { detail ->
                    facility = detail
                    content {
                        it.copy(form = FacilityTagsForm.of(detail, it.form.choices), saving = false, saved = true)
                    }
                }
                .onFailure { failure ->
                    content { it.copy(saving = false, failure = FacilityTagsFailure.of(failure.toAppError())) }
                }
        }
    }

    private fun load(keepTicks: Boolean) {
        val facility = facility ?: return
        val kept = if (keepTicks) (_state.value as? FacilityTagsUiState.Content)?.form else null
        loading?.cancel()
        _state.value = FacilityTagsUiState.Loading
        loading = viewModelScope.launch {
            _state.value = choices(facility.summary.province.id, facility.summary.category.id).fold(
                onSuccess = { offered ->
                    if (offered == null || offered.isEmpty) {
                        FacilityTagsUiState.Hidden
                    } else {
                        // The facility as last read, which may have changed while this was asked.
                        val latest = this@FacilityTagsViewModel.facility ?: facility
                        val form = kept?.withChoices(offered)?.rebase(latest) ?: FacilityTagsForm.of(latest, offered)
                        FacilityTagsUiState.Content(form = form, offline = !online)
                    }
                },
                onFailure = { FacilityTagsUiState.Error(it.toAppError()) },
            )
        }
    }

    private fun edit(change: (FacilityTagsForm) -> FacilityTagsForm) {
        val current = _state.value as? FacilityTagsUiState.Content ?: return
        if (!current.editable) return
        _state.value = current.copy(form = change(current.form), failure = null, saved = false)
    }

    private fun content(change: (FacilityTagsUiState.Content) -> FacilityTagsUiState.Content) {
        val current = _state.value as? FacilityTagsUiState.Content ?: return
        _state.value = change(current)
    }
}
