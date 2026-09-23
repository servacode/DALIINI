package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Editing what the account says about itself.
 *
 * Only the fields the backend actually accepts: a display name and the account's province.
 * Nothing else is offered, because nothing else exists to save.
 */
data class ProfileEditUiState(
    val displayName: String = "",
    val provinceId: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(
    private val account: AccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            account().onSuccess { profile ->
                _state.value = _state.value.copy(
                    displayName = profile.name,
                    provinceId = profile.provinceId,
                )
            }
        }
    }

    fun updateName(value: String) {
        _state.value = _state.value.copy(displayName = value, saved = false, error = null)
    }

    fun chooseProvince(id: String) {
        _state.value = _state.value.copy(provinceId = id, saved = false, error = null)
    }

    fun save() {
        val current = _state.value
        if (current.displayName.isBlank() || current.saving) return
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            account.update(displayName = current.displayName.trim(), provinceId = current.provinceId)
                .onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
                .onFailure {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = AppErrorText.of(it.toAppError()),
                    )
                }
        }
    }
}

/**
 * Changing the password.
 *
 * The caller proves the current one, and a success ends every session — this device's included,
 * which is why the screen's own success state says so and sends them back to signing in rather
 * than pretending they are still where they were.
 *
 * Neither password is ever put in state that outlives the screen, logged, or sent anywhere but
 * the one call that changes it.
 */
data class PasswordChangeUiState(
    val current: String = "",
    val next: String = "",
    val confirmation: String = "",
    val saving: Boolean = false,
    val changed: Boolean = false,
    val error: String? = null,
) {
    val mismatch: Boolean get() = confirmation.isNotEmpty() && next != confirmation

    val canSubmit: Boolean
        get() = current.isNotEmpty() && next.isNotEmpty() && next == confirmation && !saving
}

@HiltViewModel
class PasswordChangeViewModel @Inject constructor(
    private val api: PublicApiBoundary,
) : ViewModel() {
    private val _state = MutableStateFlow(PasswordChangeUiState())
    val state: StateFlow<PasswordChangeUiState> = _state.asStateFlow()

    fun updateCurrent(value: String) {
        _state.value = _state.value.copy(current = value, error = null)
    }

    fun updateNext(value: String) {
        _state.value = _state.value.copy(next = value, error = null)
    }

    fun updateConfirmation(value: String) {
        _state.value = _state.value.copy(confirmation = value, error = null)
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { api.changePassword(current.current, current.next) }
                .onSuccess {
                    // Nothing typed is kept: the fields are cleared as the call returns.
                    _state.value = PasswordChangeUiState(changed = true)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = AppErrorText.of(it.toAppError()),
                    )
                }
        }
    }
}
