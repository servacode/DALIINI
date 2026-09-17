package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AccountProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DeletionUiState {
    data object Idle : DeletionUiState
    data object Deleting : DeletionUiState
    data object Deleted : DeletionUiState
    data object Error : DeletionUiState
}

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Content(val profile: AccountProfile) : AccountUiState
    data object Error : AccountUiState
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val loadAccount: AccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()
    private val _deletionState = MutableStateFlow<DeletionUiState>(DeletionUiState.Idle)
    val deletionState: StateFlow<DeletionUiState> = _deletionState.asStateFlow()

    init { refresh() }

    fun deleteAccount() {
        viewModelScope.launch {
            _deletionState.value = DeletionUiState.Deleting
            _deletionState.value = deleteAccount().fold(
                onSuccess = { DeletionUiState.Deleted },
                onFailure = { DeletionUiState.Error },
            )
        }
    }

    fun clearDeletionError() {
        if (_deletionState.value == DeletionUiState.Error) {
            _deletionState.value = DeletionUiState.Idle
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AccountUiState.Loading
            _state.value = loadAccount().fold(
                onSuccess = { AccountUiState.Content(it) },
                onFailure = { AccountUiState.Error },
            )
        }
    }
}
