package com.servacode.directory.feature.owner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.FacilityMember
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.TemporaryClosure
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
    data class Content(val items: List<OwnerFacilitySummary>) : MyFacilitiesUiState
    data object Error : MyFacilitiesUiState
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
                if (event.scope.type == "user" && event.name.startsWith("user.")) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = MyFacilitiesUiState.Loading
            _state.value = load().fold(
                onSuccess = { MyFacilitiesUiState.Content(it) },
                onFailure = { MyFacilitiesUiState.Error },
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
    ) : ManageFacilityUiState
    data object Error : ManageFacilityUiState
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
                if (event.scope.type == "user" && event.name.startsWith("user.")) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val (facility, closures, members) = load(id)
            _state.value = if (facility.isSuccess && closures.isSuccess && members.isSuccess) {
                ManageFacilityUiState.Content(
                    facility.getOrThrow(), closures.getOrThrow(), members.getOrThrow()
                )
            } else ManageFacilityUiState.Error
        }
    }

    fun addManager(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            manage.upsertMember(id, userId.trim(), FacilityMemberRole.MANAGER)
                .onSuccess { refresh() }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch { manage.deleteMember(id, userId).onSuccess { refresh() } }
    }

    fun createTemporaryClosure(start: Long, end: Long, reason: String?) {
        if (end <= start) return
        viewModelScope.launch {
            manage.createClosure(id, TemporaryClosureInput(start, end, reason))
                .onSuccess { refresh() }
        }
    }

    fun deleteTemporaryClosure(closureId: String) {
        viewModelScope.launch { manage.deleteClosure(id, closureId).onSuccess { refresh() } }
    }
}
