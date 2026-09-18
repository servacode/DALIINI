package com.servacode.directory.feature.owner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.RealtimeInvalidation
import com.servacode.directory.core.network.RealtimeInvalidationBus
import com.servacode.directory.core.network.TemporaryClosureInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyFacilitiesUiState {
    data object Loading : MyFacilitiesUiState
    data class Content(val items: List<OwnedFacility>) : MyFacilitiesUiState
    data class Error(val message: String) : MyFacilitiesUiState
}

@HiltViewModel
class MyFacilitiesViewModel @Inject constructor(
    private val load: LoadOwnerFacilitiesUseCase,
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
            _state.value = load().fold(
                onSuccess = { MyFacilitiesUiState.Content(it) },
                onFailure = { MyFacilitiesUiState.Error(AppErrorText.of(it.toAppError())) },
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
        val supportsDuty: Boolean = false,
        val message: String? = null,
    ) : ManageFacilityUiState
    data class Error(val message: String) : ManageFacilityUiState
}

@HiltViewModel
class ManageFacilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val load: LoadManageFacilityUseCase,
    private val manage: ManageFacilityUseCase,
    private val invalidations: RealtimeInvalidationBus,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<DirectoryRoute.ManageFacility>().id
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
            val (facility, closures, members) = load(id)
            val failure = listOf(facility, closures, members).firstNotNullOfOrNull { it.exceptionOrNull() }
            _state.value = if (failure == null) {
                val detail = facility.getOrThrow()
                ManageFacilityUiState.Content(
                    facility = detail,
                    closures = closures.getOrThrow(),
                    members = members.getOrThrow(),
                    supportsDuty = load.supportsDuty(detail.summary),
                )
            } else {
                ManageFacilityUiState.Error(AppErrorText.of(failure.toAppError()))
            }
        }
    }

    private fun report(failure: Throwable) {
        val current = _state.value as? ManageFacilityUiState.Content ?: return
        _state.value = current.copy(message = AppErrorText.of(failure.toAppError()))
    }

    fun addManager(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            manage.upsertMember(id, userId.trim(), FacilityMemberRole.MANAGER)
                .onSuccess { refresh() }
                .onFailure(::report)
        }
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
