package com.servacode.directory.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servacode.directory.core.model.AccountProfile
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.OwnerUploadPayload
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Editing what the account says about itself.
 *
 * Only the fields the backend actually accepts: a name, a province, an address and a picture.
 * Nothing else is offered, because nothing else exists to save.
 */
data class ProfileEditUiState(
    val displayName: String = "",
    val provinceId: String? = null,
    val address: String = "",
    val phone: String = "",
    /** The picture as it is on the account now, or null when there is none. */
    val imageUrl: String? = null,
    val saving: Boolean = false,
    /** True while a picture is on its way up or out; the rest of the form stays usable. */
    val savingImage: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

open class ProfileEditViewModel(
    private val account: AccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileEditUiState())
    val state: StateFlow<ProfileEditUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            account().onSuccess(::adopt)
        }
    }

    fun updateName(value: String) {
        _state.value = _state.value.copy(displayName = value, saved = false, error = null)
    }

    fun chooseProvince(id: String) {
        _state.value = _state.value.copy(provinceId = id, saved = false, error = null)
    }

    fun updateAddress(value: String) {
        _state.value = _state.value.copy(address = value, saved = false, error = null)
    }

    /**
     * A picture chosen from the phone, sent as it is read.
     *
     * It goes up on its own rather than waiting for the form to be saved: a picture is a whole
     * decision by itself, and someone who changes it and leaves expects it changed.
     */
    fun chooseImage(payload: OwnerUploadPayload) {
        if (_state.value.savingImage) return
        _state.value = _state.value.copy(savingImage = true, error = null)
        viewModelScope.launch {
            account.updateImage(payload)
                .onSuccess { adopt(it, savingImage = false) }
                .onFailure { failImage(it) }
        }
    }

    fun removeImage() {
        if (_state.value.savingImage) return
        _state.value = _state.value.copy(savingImage = true, error = null)
        viewModelScope.launch {
            account.removeImage()
                .onSuccess { adopt(it, savingImage = false) }
                .onFailure { failImage(it) }
        }
    }

    private fun failImage(cause: Throwable) {
        _state.value = _state.value.copy(
            savingImage = false,
            error = cause.toAppError(),
        )
    }

    /** What the server says the account is now, which is what the form shows from here on. */
    private fun adopt(profile: AccountProfile, savingImage: Boolean = false) {
        _state.value = _state.value.copy(
            displayName = profile.name,
            provinceId = profile.provinceId,
            address = profile.address,
            phone = profile.phone,
            imageUrl = profile.imageUrl,
            savingImage = savingImage,
        )
    }

    fun save() {
        val current = _state.value
        if (current.displayName.isBlank() || current.saving) return
        _state.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            account.update(
                displayName = current.displayName.trim(),
                provinceId = current.provinceId,
                address = current.address.trim(),
            )
                .onSuccess { _state.value = _state.value.copy(saving = false, saved = true) }
                .onFailure {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = it.toAppError(),
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
    val error: AppError? = null,
) {
    val mismatch: Boolean get() = confirmation.isNotEmpty() && next != confirmation

    val canSubmit: Boolean
        get() = current.isNotEmpty() && next.isNotEmpty() && next == confirmation && !saving
}

open class PasswordChangeViewModel(
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
                        error = it.toAppError(),
                    )
                }
        }
    }
}
