package com.servacode.directory.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.fixWithoutPrompt
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Province
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * What a form shows after the backend refused it: the refusal, and the fields at fault.
 *
 * The refusal itself rather than a sentence, because the sentence belongs to the reader's
 * language and is read from resources where the form is drawn.
 */
data class FormFailure(
    val error: AppError,
    val fields: Set<String> = emptySet(),
    /** The backend's code, for the one or two failures a screen answers rather than states. */
    val code: String? = null,
)

internal fun AppError.toFormFailure() = FormFailure(this, fieldErrors.keys, code)

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
    private val location: LocationProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(ChallengeUiState())
    val state: StateFlow<ChallengeUiState> = _state.asStateFlow()
    private var challengeId: String? = null

    init {
        viewModelScope.launch {
            val provinces = runCatching { publicApi.provinces() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(provinceId = chosen(provinces))
        }
    }

    /**
     * Which province the account is opened in, decided rather than asked.
     *
     * In order: where the person is standing, then what this device is already browsing, then
     * the first in the list. It is shown rather than hidden and one tap changes it — the
     * permission may be refused, and someone registering while away from home would otherwise
     * have their account bound silently to the wrong directory.
     *
     * The position is taken only if the app already has it: registration is not the moment to
     * put a permission dialog in front of someone.
     */
    private suspend fun chosen(provinces: List<Province>): String? {
        val here = location.fixWithoutPrompt()
            ?.let { fix -> runCatching { publicApi.resolvePlace(fix.latitude, fix.longitude) }.getOrNull() }
            ?.province
            ?.id
        val known = { id: String? -> id?.takeIf { candidate -> provinces.any { it.id == candidate } } }
        return known(here)
            ?: known(preferences.values.first().selectedProvinceId)
            ?: provinces.firstOrNull()?.id
    }

    fun start(phone: String) {
        val provinceId = _state.value.provinceId ?: return
        step(ChallengeStep.CODE) {
            auth.startRegistration(phone, provinceId).map { challengeId = it.id }
        }
    }

    fun verify(code: String) {
        val id = challengeId ?: return
        step(ChallengeStep.PASSWORD) { auth.verifyRegistration(id, code) }
    }

    fun complete(displayName: String, password: String) {
        val id = challengeId ?: return
        step(ChallengeStep.DONE) { auth.completeRegistration(id, displayName, password) }
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
