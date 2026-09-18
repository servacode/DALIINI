package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DeletionUiState {
    data object Idle : DeletionUiState
    data object Deleting : DeletionUiState
    data object Deleted : DeletionUiState
    data class Error(val message: String) : DeletionUiState
}

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data object SignedOut : AccountUiState
    data class Content(
        val profile: AccountProfile,
        val provinces: List<Province> = emptyList(),
        val message: String? = null,
    ) : AccountUiState
    data class Error(val message: String) : AccountUiState
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val account: AccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()
    private val _deletionState = MutableStateFlow<DeletionUiState>(DeletionUiState.Idle)
    val deletionState: StateFlow<DeletionUiState> = _deletionState.asStateFlow()

    init {
        // Follows the session: signing out here, or the session ending elsewhere, shows the
        // signed-out screen instead of a profile that is no longer the user's.
        viewModelScope.launch {
            account.session.distinctUntilChanged().collect { session ->
                if (session == SessionState.SIGNED_IN) refresh() else _state.value = AccountUiState.SignedOut
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AccountUiState.Loading
            _state.value = account().fold(
                onSuccess = { profile ->
                    AccountUiState.Content(profile, account.provinces().getOrDefault(emptyList()))
                },
                onFailure = { AccountUiState.Error(AppErrorText.of(it.toAppError())) },
            )
        }
    }

    fun changeProvince(provinceId: String) {
        val current = _state.value as? AccountUiState.Content ?: return
        viewModelScope.launch {
            _state.value = account.changeProvince(provinceId).fold(
                onSuccess = { current.copy(profile = it, message = "تم تغيير المحافظة") },
                onFailure = { current.copy(message = AppErrorText.of(it.toAppError())) },
            )
        }
    }

    fun logout() {
        viewModelScope.launch { account.logout() }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deletionState.value = DeletionUiState.Deleting
            _deletionState.value = deleteAccount().fold(
                onSuccess = { DeletionUiState.Deleted },
                onFailure = { DeletionUiState.Error(AppErrorText.of(it.toAppError())) },
            )
        }
    }

    fun clearDeletionError() {
        if (_deletionState.value is DeletionUiState.Error) {
            _deletionState.value = DeletionUiState.Idle
        }
    }
}
