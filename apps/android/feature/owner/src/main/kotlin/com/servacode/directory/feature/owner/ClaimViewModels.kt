package com.servacode.directory.feature.owner

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.UploadReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How long typing must pause before the name is looked up. */
internal const val CLAIM_SEARCH_PAUSE_MS = 350L

/** The backend needs two letters of a name before it looks. */
internal const val CLAIM_SEARCH_MIN = 2

data class ClaimSearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    /** Null until a name long enough has been looked up. */
    val results: List<ClaimableFacility>? = null,
    /** The facility a claim is being started on, so its row waits. */
    val starting: String? = null,
    val failure: AppError? = null,
)

/**
 * «هذه منشأتي»: the owner types the facility's name, picks it from the facilities nobody owns,
 * and a claim is started on it (or the open one they already have is reopened).
 */
@HiltViewModel
class ClaimSearchViewModel @Inject constructor(
    private val claims: ClaimFacilityUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ClaimSearchUiState())
    val state: StateFlow<ClaimSearchUiState> = _state.asStateFlow()

    /** The claim just started or reopened; the screen opens it. */
    private val _opened = MutableStateFlow<String?>(null)
    val opened: StateFlow<String?> = _opened.asStateFlow()

    private var search: Job? = null

    fun query(text: String) {
        _state.update { it.copy(query = text, failure = null) }
        search?.cancel()
        val name = text.trim()
        if (name.length < CLAIM_SEARCH_MIN) {
            _state.update { it.copy(searching = false, results = null) }
            return
        }
        search = viewModelScope.launch {
            delay(CLAIM_SEARCH_PAUSE_MS)
            _state.update { it.copy(searching = true) }
            claims.search(name).fold(
                onSuccess = { found -> _state.update { it.copy(searching = false, results = found) } },
                onFailure = { error -> _state.update { it.copy(searching = false, failure = error.toAppError()) } },
            )
        }
    }

    fun retry() = query(_state.value.query)

    fun start(facilityId: String) {
        if (_state.value.starting != null) return
        _state.update { it.copy(starting = facilityId, failure = null) }
        viewModelScope.launch {
            claims.start(facilityId).fold(
                onSuccess = { claim ->
                    _state.update { it.copy(starting = null) }
                    _opened.value = claim.id
                },
                onFailure = { error -> _state.update { it.copy(starting = null, failure = error.toAppError()) } },
            )
        }
    }

    fun consumeOpened() {
        _opened.value = null
    }
}

sealed interface ClaimUiState {
    data object Loading : ClaimUiState
    data class Content(
        val claim: FacilityClaim,
        /** The requirement a document is being uploaded for. */
        val uploading: String? = null,
        /** Sending or withdrawing: every button waits. */
        val busy: Boolean = false,
        val failure: AppError? = null,
        /** The picked file could not be read on this device. */
        val unreadable: Boolean = false,
    ) : ClaimUiState
    data class Error(val error: AppError) : ClaimUiState
}

/** One claim: its documents, sending it, and withdrawing it. */
@HiltViewModel
class ClaimViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val claims: ClaimFacilityUseCase,
    private val uploadReader: UploadReader,
) : ViewModel() {
    private val id = savedStateHandle.routeId()
    private val _state = MutableStateFlow<ClaimUiState>(ClaimUiState.Loading)
    val state: StateFlow<ClaimUiState> = _state.asStateFlow()

    /** Withdrawn: the claim is gone, and so is its screen. */
    private val _withdrawn = MutableStateFlow(false)
    val withdrawn: StateFlow<Boolean> = _withdrawn.asStateFlow()

    /** A fresh claim on the same facility after a refusal; the screen opens it instead. */
    private val _reopened = MutableStateFlow<String?>(null)
    val reopened: StateFlow<String?> = _reopened.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = claims.claim(id).fold(
                onSuccess = { ClaimUiState.Content(it) },
                onFailure = { ClaimUiState.Error(it.toAppError()) },
            )
        }
    }

    fun upload(requirementId: String, uri: Uri) {
        val current = content() ?: return
        if (current.uploading != null || current.busy) return
        _state.value = current.copy(uploading = requirementId, failure = null, unreadable = false)
        viewModelScope.launch {
            val payload = withContext(Dispatchers.IO) { uploadReader.read(uri) }
            send(requirementId, payload)
        }
    }

    /** The second half of [upload], once the file is read; apart so a test can hand it bytes. */
    internal suspend fun send(requirementId: String, payload: Result<OwnerUploadPayload>) {
        val file = payload.getOrElse {
            _state.update { (it as? ClaimUiState.Content)?.copy(uploading = null, unreadable = true) ?: it }
            return
        }
        claims.upload(id, requirementId, file)
            .onSuccess { evidence ->
                _state.update { state ->
                    (state as? ClaimUiState.Content)?.let {
                        it.copy(claim = it.claim.copy(evidence = it.claim.evidence + evidence), uploading = null)
                    } ?: state
                }
            }
            .onFailure { error ->
                _state.update { state ->
                    (state as? ClaimUiState.Content)?.copy(uploading = null, failure = error.toAppError()) ?: state
                }
            }
    }

    fun deleteEvidence(evidenceId: String) = act { current ->
        claims.deleteEvidence(id, evidenceId).map {
            current.claim.copy(evidence = current.claim.evidence.filterNot { it.id == evidenceId })
        }
    }

    fun submit() = act { claims.submit(id) }

    fun withdraw() {
        val current = content() ?: return
        if (current.busy) return
        _state.value = current.copy(busy = true, failure = null)
        viewModelScope.launch {
            claims.withdraw(id)
                .onSuccess { _withdrawn.value = true }
                .onFailure { _state.value = current.copy(busy = false, failure = it.toAppError()) }
        }
    }

    /** A refused claim stays as the record; trying again is a new claim on the same facility. */
    fun startAgain() {
        val current = content() ?: return
        if (current.busy) return
        _state.value = current.copy(busy = true, failure = null)
        viewModelScope.launch {
            claims.start(current.claim.facilityId)
                .onSuccess { _reopened.value = it.id }
                .onFailure { _state.value = current.copy(busy = false, failure = it.toAppError()) }
        }
    }

    fun consumeReopened() {
        _reopened.value = null
    }

    private fun act(call: suspend (ClaimUiState.Content) -> Result<FacilityClaim>) {
        val current = content() ?: return
        if (current.busy || current.uploading != null) return
        _state.value = current.copy(busy = true, failure = null, unreadable = false)
        viewModelScope.launch {
            _state.value = call(current).fold(
                onSuccess = { current.copy(claim = it, busy = false) },
                onFailure = { current.copy(busy = false, failure = it.toAppError()) },
            )
        }
    }

    private fun content() = _state.value as? ClaimUiState.Content
}
