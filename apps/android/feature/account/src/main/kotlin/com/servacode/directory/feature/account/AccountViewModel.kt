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

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Content(val profile: AccountProfile) : AccountUiState
    data object Error : AccountUiState
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val loadAccount: AccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountUiState>(AccountUiState.Loading)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init { refresh() }

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
