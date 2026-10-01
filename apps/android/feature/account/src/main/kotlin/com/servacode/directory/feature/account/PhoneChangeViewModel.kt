package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Moving the account to another number.
 *
 * Two steps, because a phone number is not a field: the code goes to the number being claimed,
 * which is the only thing worth proving, and until it is proved nothing about the account has
 * changed. Confirming it ends every session including this one, so the app signs in again — the
 * screen says so before the person starts rather than after it has happened to them.
 */
data class PhoneChangeUiState(
    val phone: String = "",
    val code: String = "",
    /** Set once a code has been sent; until then the screen is asking for a number. */
    val challengeId: String? = null,
    val working: Boolean = false,
    val changed: Boolean = false,
    val error: AppError? = null,
) {
    val canSend: Boolean get() = phone.isNotBlank() && !working
    val canConfirm: Boolean get() = code.length == CODE_LENGTH && !working

    companion object {
        const val CODE_LENGTH = 6
    }
}

@HiltViewModel
class PhoneChangeViewModel @Inject constructor(
    private val account: AccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PhoneChangeUiState())
    val state: StateFlow<PhoneChangeUiState> = _state.asStateFlow()

    fun updatePhone(value: String) {
        _state.value = _state.value.copy(phone = value, error = null)
    }

    fun updateCode(value: String) {
        // Digits only, and never more than a code's worth: the field is a code, not free text.
        val digits = value.filter(Char::isDigit).take(PhoneChangeUiState.CODE_LENGTH)
        _state.value = _state.value.copy(code = digits, error = null)
    }

    fun send() {
        val current = _state.value
        if (!current.canSend) return
        _state.value = current.copy(working = true, error = null)
        viewModelScope.launch {
            account.startPhoneChange(current.phone.trim())
                .onSuccess { _state.value = _state.value.copy(working = false, challengeId = it) }
                .onFailure { fail(it) }
        }
    }

    fun confirm() {
        val current = _state.value
        val challenge = current.challengeId ?: return
        if (!current.canConfirm) return
        _state.value = current.copy(working = true, error = null)
        viewModelScope.launch {
            account.confirmPhoneChange(challenge, current.code)
                .onSuccess { _state.value = _state.value.copy(working = false, changed = true) }
                .onFailure { fail(it) }
        }
    }

    /** Back to asking for a number: a code that will not be accepted is not worth retyping. */
    fun startOver() {
        _state.value = PhoneChangeUiState(phone = _state.value.phone)
    }

    private fun fail(cause: Throwable) {
        _state.value = _state.value.copy(working = false, error = cause.toAppError())
    }
}
