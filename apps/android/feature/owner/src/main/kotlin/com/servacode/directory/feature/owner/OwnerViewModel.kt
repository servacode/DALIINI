package com.servacode.directory.feature.owner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.ClaimStatus
import com.servacode.directory.core.model.FacilityClaim
import com.servacode.directory.core.model.FacilityInvitation
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.InvitationStatus
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.ReceivedInvitation
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.network.TemporaryClosureInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The `id` of the route a screen was opened with: [com.servacode.directory.core.model.DirectoryRoute.ManageFacility]
 * or [com.servacode.directory.core.model.DirectoryRoute.Claim].
 *
 * Type-safe navigation keeps each route argument in the saved state under its property's name,
 * which is what `toRoute` reads too. Reading the one string directly does the same without
 * going through a Bundle, so the view models that need it also run in plain JVM tests. It lives
 * here, with the view models, because the platform-free harness compiles none of them.
 */
internal fun SavedStateHandle.routeId(): String =
    checkNotNull(get<String>("id")) { "The route carries no id." }

sealed interface MyFacilitiesUiState {
    data object Loading : MyFacilitiesUiState
    data class Content(
        val items: List<OwnerFacilitySummary>,
        /** Claims still open, and refused ones with their reason; an approved one is a facility above. */
        val claims: List<FacilityClaim> = emptyList(),
    ) : MyFacilitiesUiState
    data class Error(val error: AppError) : MyFacilitiesUiState
}

@HiltViewModel
class MyFacilitiesViewModel @Inject constructor(
    private val load: LoadOwnerFacilitiesUseCase,
    private val claims: ClaimFacilityUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val _state = MutableStateFlow<MyFacilitiesUiState>(MyFacilitiesUiState.Loading)
    val state: StateFlow<MyFacilitiesUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (RealtimeInvalidation.refreshesOwnerState(event)) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = MyFacilitiesUiState.Loading
            val facilities = load()
            // A claim list that cannot be read does not hide the facilities themselves.
            val open = claims.claims().getOrDefault(emptyList()).filter { it.status != ClaimStatus.APPROVED }
            _state.value = facilities.fold(
                onSuccess = { MyFacilitiesUiState.Content(it, open) },
                onFailure = { MyFacilitiesUiState.Error(it.toAppError()) },
            )
        }
    }
}

sealed interface ManageFacilityUiState {
    data object Loading : ManageFacilityUiState
    data class Content(
        val facility: OwnerFacilityDetail,
        val closures: List<TemporaryClosure>,
        val members: List<FacilityMember>,
        /** Null when this account may not invite: only the facility's owner may (DECISION-064). */
        val invitations: List<FacilityInvitation>? = null,
        /** A phone number just invited, so the screen can say so and clear its field. */
        val invited: String? = null,
        val failure: AppError? = null,
    ) : ManageFacilityUiState
    data class Error(val error: AppError) : ManageFacilityUiState
}

@HiltViewModel
class ManageFacilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val load: LoadManageFacilityUseCase,
    private val manage: ManageFacilityUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val id = savedStateHandle.routeId()
    private val _state = MutableStateFlow<ManageFacilityUiState>(ManageFacilityUiState.Loading)
    val state: StateFlow<ManageFacilityUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            invalidations.events.collect { event ->
                if (RealtimeInvalidation.refreshesOwnerState(event)) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val loaded = load(id)
            val failure = listOf(loaded.facility, loaded.closures, loaded.members)
                .firstNotNullOfOrNull { it.exceptionOrNull() }
            val invited = (_state.value as? ManageFacilityUiState.Content)?.invited
            _state.value = if (failure == null) {
                ManageFacilityUiState.Content(
                    facility = loaded.facility.getOrThrow(),
                    closures = loaded.closures.getOrThrow(),
                    members = loaded.members.getOrThrow(),
                    // Still waiting first, then the rest as the backend ordered them.
                    invitations = loaded.invitations.getOrNull()
                        ?.sortedBy { if (it.status == InvitationStatus.PENDING) 0 else 1 },
                    invited = invited,
                )
            } else {
                ManageFacilityUiState.Error(failure.toAppError())
            }
        }
    }

    private fun report(failure: Throwable) {
        val current = _state.value as? ManageFacilityUiState.Content ?: return
        _state.value = current.copy(failure = failure.toAppError())
    }

    /** Invite a phone number to help run the facility, as a manager. */
    fun invite(phone: String) {
        val number = phone.trim()
        if (number.isEmpty()) return
        viewModelScope.launch {
            manage.invite(id, number)
                .onSuccess {
                    (_state.value as? ManageFacilityUiState.Content)?.let { current ->
                        _state.value = current.copy(invited = number, failure = null)
                    }
                    refresh()
                }
                .onFailure(::report)
        }
    }

    fun revokeInvitation(invitationId: String) {
        viewModelScope.launch {
            manage.revokeInvitation(id, invitationId).onSuccess { refresh() }.onFailure(::report)
        }
    }

    fun dismissInvited() {
        (_state.value as? ManageFacilityUiState.Content)?.let { _state.value = it.copy(invited = null) }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch { manage.deleteMember(id, userId).onSuccess { refresh() }.onFailure(::report) }
    }

    fun createTemporaryClosure(start: Long, end: Long, reason: String?) {
        if (end <= start) return
        viewModelScope.launch {
            manage.createClosure(id, TemporaryClosureInput(start, end, reason))
                // The backend validates the window; once it accepts, read the facility again.
                .onSuccess { refresh() }
                .onFailure(::report)
        }
    }

    fun deleteTemporaryClosure(closureId: String) {
        viewModelScope.launch { manage.deleteClosure(id, closureId).onSuccess { refresh() }.onFailure(::report) }
    }
}

sealed interface InvitationsUiState {
    data object Loading : InvitationsUiState
    data class Content(
        val items: List<ReceivedInvitation>,
        /** The invitation being answered, so its buttons wait. */
        val busy: String? = null,
        val failure: AppError? = null,
    ) : InvitationsUiState
    data class Error(val error: AppError) : InvitationsUiState
}

/** The invitations waiting for this account: accept to join the facility, or decline. */
@HiltViewModel
class InvitationsViewModel @Inject constructor(
    private val invitations: ReceivedInvitationsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<InvitationsUiState>(InvitationsUiState.Loading)
    val state: StateFlow<InvitationsUiState> = _state.asStateFlow()

    /** The facility just joined; the screen opens its management page. */
    private val _joined = MutableStateFlow<String?>(null)
    val joined: StateFlow<String?> = _joined.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = invitations.load().fold(
                onSuccess = { InvitationsUiState.Content(it) },
                onFailure = { InvitationsUiState.Error(it.toAppError()) },
            )
        }
    }

    fun accept(invitationId: String) = answer(invitationId) {
        invitations.accept(invitationId).map { facilityId -> _joined.value = facilityId }
    }

    fun decline(invitationId: String) = answer(invitationId) { invitations.decline(invitationId) }

    fun consumeJoined() {
        _joined.value = null
    }

    private fun answer(invitationId: String, call: suspend () -> Result<Unit>) {
        val current = _state.value as? InvitationsUiState.Content ?: return
        if (current.busy != null) return
        _state.value = current.copy(busy = invitationId, failure = null)
        viewModelScope.launch {
            call().fold(
                onSuccess = {
                    _state.value = current.copy(items = current.items.filterNot { it.id == invitationId }, busy = null)
                },
                onFailure = { _state.value = current.copy(busy = null, failure = it.toAppError()) },
            )
        }
    }
}
