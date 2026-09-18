package com.servacode.directory.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What a form shows after the backend refused it: one message, and the fields at fault. */
data class FormFailure(val message: String, val fields: Set<String> = emptySet())

internal fun AppError.toFormFailure() = FormFailure(AppErrorText.of(this), fieldErrors.keys)

data class LoginUiState(
    val busy: Boolean = false,
    val failure: FormFailure? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun submit(phone: String, password: String) {
        if (_state.value.busy) return
        _state.value = LoginUiState(busy = true)
        viewModelScope.launch {
            _state.value = auth.login(phone, password).fold(
                onSuccess = { LoginUiState(signedIn = true) },
                onFailure = { LoginUiState(failure = it.toAppError().toFormFailure()) },
            )
        }
    }
}

enum class ChallengeStep { DETAILS, CODE, PASSWORD, DONE }

data class ChallengeUiState(
    val step: ChallengeStep = ChallengeStep.DETAILS,
    val busy: Boolean = false,
    val failure: FormFailure? = null,
    val provinces: List<Province> = emptyList(),
    val provinceId: String? = null,
)

/**
 * Registration: details → the one-time code the backend sent → a password. Only the last
 * step signs in. The code is typed and sent, never stored.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val publicApi: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ChallengeUiState())
    val state: StateFlow<ChallengeUiState> = _state.asStateFlow()
    private var challengeId: String? = null

    init {
        viewModelScope.launch {
            val selected = preferences.values.first().selectedProvinceId
            val provinces = runCatching { publicApi.provinces() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(
                provinces = provinces,
                provinceId = selected?.takeIf { id -> provinces.any { it.id == id } } ?: provinces.firstOrNull()?.id,
            )
        }
    }

    fun chooseProvince(id: String) {
        _state.value = _state.value.copy(provinceId = id)
    }

    fun start(displayName: String, phone: String) {
        val provinceId = _state.value.provinceId ?: return
        step(ChallengeStep.CODE) {
            auth.startRegistration(displayName, phone, provinceId).map { challengeId = it.id }
        }
    }

    fun verify(code: String) {
        val id = challengeId ?: return
        step(ChallengeStep.PASSWORD) { auth.verifyRegistration(id, code) }
    }

    fun complete(password: String) {
        val id = challengeId ?: return
        step(ChallengeStep.DONE) { auth.completeRegistration(id, password) }
    }

    private fun step(next: ChallengeStep, action: suspend () -> Result<*>) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, failure = null)
        viewModelScope.launch {
            _state.value = action().fold(
                onSuccess = { _state.value.copy(step = next, busy = false) },
                onFailure = { _state.value.copy(busy = false, failure = it.toAppError().toFormFailure()) },
            )
        }
    }
}

/** Recovery: phone → code → new password, then back to sign-in. */
@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChallengeUiState())
    val state: StateFlow<ChallengeUiState> = _state.asStateFlow()
    private var challengeId: String? = null

    fun start(phone: String) = step(ChallengeStep.CODE) {
        auth.startRecovery(phone).map { challengeId = it.id }
    }

    fun verify(code: String) {
        val id = challengeId ?: return
        step(ChallengeStep.PASSWORD) { auth.verifyRecovery(id, code) }
    }

    fun reset(password: String) {
        val id = challengeId ?: return
        step(ChallengeStep.DONE) { auth.resetPassword(id, password) }
    }

    private fun step(next: ChallengeStep, action: suspend () -> Result<*>) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, failure = null)
        viewModelScope.launch {
            _state.value = action().fold(
                onSuccess = { _state.value.copy(step = next, busy = false) },
                onFailure = { _state.value.copy(busy = false, failure = it.toAppError().toFormFailure()) },
            )
        }
    }
}
