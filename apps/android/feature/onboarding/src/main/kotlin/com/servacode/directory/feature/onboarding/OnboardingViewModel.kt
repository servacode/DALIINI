package com.servacode.directory.feature.onboarding

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.model.AppErrorText
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.OwnerCategoryConfig
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.OwnerFacilityPatch
import com.servacode.directory.core.network.PushAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    PROVINCE_CATEGORY,
    BASIC_INFO,
    MAP_POINT,
    HOURS,
    PUBLIC_IMAGES,
    SPECIALIZED_FIELDS,
    VERIFICATION_EVIDENCE,
    REVIEW,
    SUBMIT,
    STATUS,
}

data class OnboardingForm(
    val categoryId: String? = null,
    val nameAr: String = "",
    val nameEn: String = "",
    val descriptionAr: String = "",
    val phone: String = "",
    val addressAr: String = "",
)

sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data object ProvinceRequired : OnboardingUiState
    data class Content(
        val step: OnboardingStep,
        val config: OwnerConfig,
        val selectedCategory: OwnerCategoryConfig?,
        val draft: OwnerFacilityDetail?,
        val form: OnboardingForm,
        val busy: Boolean = false,
        val message: String? = null,
        /** Set once, on the submission that just succeeded; the screen may then ask for notifications. */
        val justSubmitted: Boolean = false,
    ) : OnboardingUiState
    data object Error : OnboardingUiState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val load: LoadOnboardingUseCase,
    private val save: SaveOnboardingUseCase,
    private val preferences: DirectoryPreferencesStore,
    private val locationProvider: LocationProvider,
    private val uploadReader: OwnerUploadReader,
    private val push: PushAvailability,
) : ViewModel() {
    /** Whether this build can receive push, and so whether a notification permission is of any use. */
    val pushEnabled: Boolean
        get() = push.enabled

    private val route = savedStateHandle.toRoute<DirectoryRoute.Onboarding>()
    private val _state = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Loading)
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()
    private var autosaveJob: Job? = null

    init { initialize() }

    private fun initialize() {
        viewModelScope.launch {
            val existing = route.draftId?.let { load.facility(it).getOrNull() }
            val provinceId = existing?.summary?.province?.id
                ?: preferences.values.first().selectedProvinceId
            if (provinceId == null) {
                _state.value = OnboardingUiState.ProvinceRequired
                return@launch
            }
            val config = load.config(provinceId).getOrElse {
                _state.value = OnboardingUiState.Error
                return@launch
            }
            val selected = existing?.summary?.category?.id?.let { id ->
                config.categories.firstOrNull { it.category.id == id }
            }
            _state.value = OnboardingUiState.Content(
                step = if (existing == null) OnboardingStep.PROVINCE_CATEGORY else OnboardingStep.BASIC_INFO,
                config = config,
                selectedCategory = selected,
                draft = existing,
                form = OnboardingForm(
                    categoryId = existing?.summary?.category?.id,
                    nameAr = existing?.summary?.nameAr.orEmpty(),
                    nameEn = existing?.nameEn.orEmpty(),
                    descriptionAr = existing?.descriptionAr.orEmpty(),
                    phone = existing?.phone.orEmpty(),
                    addressAr = existing?.addressAr.orEmpty(),
                ),
            )
        }
    }

    fun selectCategory(categoryId: String) = mutate { current ->
        current.copy(
            selectedCategory = current.config.categories.firstOrNull { it.category.id == categoryId },
            form = current.form.copy(categoryId = categoryId),
            step = OnboardingStep.BASIC_INFO,
        )
    }

    fun updateNameAr(value: String) = updateForm { copy(nameAr = value) }
    fun updateNameEn(value: String) = updateForm { copy(nameEn = value) }
    fun updateDescriptionAr(value: String) = updateForm { copy(descriptionAr = value) }
    fun updatePhone(value: String) = updateForm { copy(phone = value) }
    fun updateAddressAr(value: String) = updateForm { copy(addressAr = value) }

    private fun updateForm(change: OnboardingForm.() -> OnboardingForm) {
        mutate { it.copy(form = it.form.change()) }
        val content = _state.value as? OnboardingUiState.Content ?: return
        if (content.draft != null) scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(650)
            saveCore(advance = false)
        }
    }

    fun saveBasicAndContinue() {
        viewModelScope.launch { saveCore(advance = true) }
    }

    private suspend fun saveCore(advance: Boolean) {
        val current = _state.value as? OnboardingUiState.Content ?: return
        val categoryId = current.form.categoryId ?: return
        if (current.form.nameAr.isBlank()) return
        val result = if (current.draft == null) {
            save.create(
                OwnerFacilityDraftInput(
                    provinceId = current.config.province.id,
                    categoryId = categoryId,
                    nameAr = current.form.nameAr.trim(),
                    nameEn = current.form.nameEn.trim().ifBlank { null },
                )
            )
        } else {
            save.patch(
                current.draft.summary.id,
                OwnerFacilityPatch(
                    nameAr = current.form.nameAr.trim(),
                    nameEn = current.form.nameEn.trim(),
                    descriptionAr = current.form.descriptionAr.trim(),
                    phone = current.form.phone.trim(),
                    addressAr = current.form.addressAr.trim(),
                ),
            )
        }
        result.onSuccess { draft ->
            mutate {
                it.copy(
                    draft = draft,
                    step = if (advance) OnboardingStep.MAP_POINT else it.step,
                    message = if (advance) null else "تم حفظ المسودة تلقائيًا",
                )
            }
        }.onFailure { failure ->
                mutate { it.copy(message = "تعذر حفظ المسودة: " + AppErrorText.of(failure.toAppError())) }
            }
    }

    fun selectMapPoint(latitude: Double, longitude: Double) {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val draft = content.draft ?: return
        viewModelScope.launch {
            save.location(draft.summary.id, latitude, longitude).onSuccess { updated ->
                mutate { it.copy(draft = updated, step = OnboardingStep.HOURS, message = null) }
            }.onFailure { failure ->
                mutate { it.copy(message = "تعذر حفظ الموقع: " + AppErrorText.of(failure.toAppError())) }
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            val current = _state.value as? OnboardingUiState.Content ?: return@launch
            val draft = current.draft ?: return@launch
            when (val result = locationProvider.current()) {
                is LocationResult.Available -> save.location(
                    draft.summary.id,
                    result.fix.latitude,
                    result.fix.longitude,
                ).onSuccess { updated ->
                    mutate { it.copy(draft = updated, step = OnboardingStep.HOURS, message = null) }
                }.onFailure { failure ->
                mutate { it.copy(message = "تعذر حفظ الموقع: " + AppErrorText.of(failure.toAppError())) }
            }
                LocationResult.PermissionDenied -> mutate { it.copy(message = "يلزم السماح بالموقع") }
                LocationResult.Unavailable -> mutate { it.copy(message = "تعذر تحديد الموقع") }
            }
        }
    }

    fun saveHours(rows: List<BusinessHour>) {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val id = content.draft?.summary?.id ?: return
        viewModelScope.launch {
            save.hours(id, rows).onSuccess {
                mutate { it.copy(step = OnboardingStep.PUBLIC_IMAGES, message = null) }
            }.onFailure { failure ->
                mutate { it.copy(message = "تعذر حفظ ساعات العمل: " + AppErrorText.of(failure.toAppError())) }
            }
        }
    }

    fun uploadPublicImage(uri: Uri) = upload(uri, null)

    fun uploadEvidence(requirementId: String, uri: Uri) = upload(uri, requirementId)

    private fun upload(uri: Uri, requirementId: String?) {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val id = content.draft?.summary?.id ?: return
        viewModelScope.launch {
            val payload = uploadReader.read(uri).getOrElse {
                mutate { it.copy(message = "تعذر قراءة الصورة") }
                return@launch
            }
            val result = if (requirementId == null) {
                save.image(id, payload).map { Unit }
            } else {
                save.evidence(id, requirementId, payload).map { Unit }
            }
            result.onSuccess {
                load.facility(id).onSuccess { updated ->
                    mutate {
                        it.copy(
                            draft = updated,
                            message = "تم رفع الملف",
                            step = if (requirementId == null) {
                                OnboardingStep.SPECIALIZED_FIELDS
                            } else it.step,
                        )
                    }
                }
            }.onFailure { failure ->
                mutate { it.copy(message = "تعذر رفع الملف: " + AppErrorText.of(failure.toAppError())) }
            }
        }
    }

    fun nextFromImages() = mutate {
        it.copy(step = OnboardingStep.SPECIALIZED_FIELDS)
    }

    fun nextFromSpecializedFields() = mutate {
        it.copy(step = OnboardingStep.VERIFICATION_EVIDENCE)
    }

    fun review() = mutate { it.copy(step = OnboardingStep.REVIEW) }

    fun submit() {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val id = content.draft?.summary?.id ?: return
        viewModelScope.launch {
            mutate { it.copy(step = OnboardingStep.SUBMIT, busy = true, message = null) }
            save.submit(id).onSuccess {
                load.facility(id).onSuccess { updated ->
                    mutate {
                        it.copy(
                            draft = updated,
                            step = OnboardingStep.STATUS,
                            busy = false,
                            message = "تم إرسال الطلب للمراجعة",
                            justSubmitted = true,
                        )
                    }
                }
            }.onFailure { failure ->
                mutate {
                    it.copy(
                        step = OnboardingStep.REVIEW,
                        busy = false,
                        message = "تعذر الإرسال: " + AppErrorText.of(failure.toAppError()),
                    )
                }
            }
        }
    }

    fun notificationPromptHandled() = mutate { it.copy(justSubmitted = false) }

    private fun mutate(change: (OnboardingUiState.Content) -> OnboardingUiState.Content) {
        val current = _state.value as? OnboardingUiState.Content ?: return
        _state.value = change(current)
    }
}
