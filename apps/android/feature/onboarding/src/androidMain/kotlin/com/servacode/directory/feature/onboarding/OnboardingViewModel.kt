package com.servacode.directory.feature.onboarding

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.fixWithoutPrompt
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.maps.MapCamera
import com.servacode.directory.core.maps.MapCameraPolicy
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.toMapPoint
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.DirectoryRoute
import com.servacode.directory.core.model.OwnerCategoryConfig
import com.servacode.directory.core.model.OwnerConfig
import com.servacode.directory.core.model.OwnerFacilityDetail
import com.servacode.directory.core.model.OwnerFacilityImage
import com.servacode.directory.core.model.toAppError
import com.servacode.directory.core.network.OwnerFacilityDraftInput
import com.servacode.directory.core.network.PushAvailability
import com.servacode.directory.core.network.UploadReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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
        val message: OnboardingMessage? = null,
        /** Set once, on the submission that just succeeded; the screen may then ask for notifications. */
        val justSubmitted: Boolean = false,
        /** Where the location picker looks; null until known, and the picker waits for it. */
        val pickerCamera: MapCamera? = null,
        val pickerReady: Boolean = false,
        /** The point the owner marked and has not saved yet. */
        val pendingPoint: MapPoint? = null,
        /** «التالي» was pressed with no name; the field says so until one is typed. */
        val nameMissing: Boolean = false,
        /** The public photos already on the facility, shown on the photos step. */
        val images: List<OwnerFacilityImage> = emptyList(),
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
    private val uploadReader: UploadReader,
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
                    whatsapp = existing?.whatsapp.orEmpty(),
                    addressAr = existing?.addressAr.orEmpty(),
                ),
            )
            val pickerCamera = MapCameraPolicy.forPicker(
                user = { locationProvider.fixWithoutPrompt()?.let { MapPoint(it.latitude, it.longitude) } },
                province = { config.province.mapCenter?.toMapPoint() },
            )
            mutate { it.copy(pickerCamera = pickerCamera, pickerReady = true) }
        }
    }

    fun selectCategory(categoryId: String) = mutate { current ->
        current.copy(
            selectedCategory = current.config.categories.firstOrNull { it.category.id == categoryId },
            form = current.form.copy(categoryId = categoryId),
            step = OnboardingStep.BASIC_INFO,
        )
    }

    fun updateNameAr(value: String) {
        mutate { it.copy(nameMissing = false) }
        updateForm { copy(nameAr = value) }
    }
    fun updateNameEn(value: String) = updateForm { copy(nameEn = value) }
    fun updateDescriptionAr(value: String) = updateForm { copy(descriptionAr = value) }
    fun updatePhone(value: String) = updateForm { copy(phone = value) }
    fun updateWhatsapp(value: String) = updateForm { copy(whatsapp = value) }
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
        // A nameless facility cannot be saved; «التالي» used to do nothing at all, silently.
        val current = _state.value as? OnboardingUiState.Content
        if (current != null && current.form.nameAr.isBlank()) {
            mutate { it.copy(nameMissing = true) }
            return
        }
        viewModelScope.launch { saveCore(advance = true) }
    }

    private suspend fun saveCore(advance: Boolean) {
        val current = _state.value as? OnboardingUiState.Content ?: return
        val categoryId = current.form.categoryId ?: return
        if (current.form.nameAr.isBlank()) return
        val result = if (current.draft == null) {
            val created = save.create(
                OwnerFacilityDraftInput(
                    provinceId = current.config.province.id,
                    categoryId = categoryId,
                    nameAr = current.form.nameAr.trim(),
                    nameEn = current.form.nameEn.trim().ifBlank { null },
                )
            )
            // Creating a draft takes only its name and category. Everything else typed before
            // the first «التالي» — description, phone, WhatsApp, address — went nowhere, because
            // autosave starts only once a draft exists: a pharmacy reached review with no phone.
            // So the rest follows at once, in the same save.
            val draft = created.getOrNull()
            if (draft != null && current.form.hasDetails) {
                save.patch(draft.summary.id, current.form.toPatch())
            } else {
                created
            }
        } else {
            save.patch(current.draft.summary.id, current.form.toPatch())
        }
        result.onSuccess { draft ->
            mutate {
                it.copy(
                    draft = draft,
                    step = if (advance) OnboardingStep.MAP_POINT else it.step,
                    message = if (advance) null else OnboardingMessage(OnboardingNotice.DRAFT_SAVED),
                )
            }
        }.onFailure { failure ->
                mutate {
                    it.copy(
                        message = OnboardingMessage(
                            OnboardingNotice.DRAFT_SAVE_FAILED,
                            failure.toAppError(),
                        ),
                    )
                }
            }
    }

    /** A tap on the picker marks the point; nothing is saved until [confirmMapPoint]. */
    fun markMapPoint(point: MapPoint) = mutate { it.copy(pendingPoint = point, message = null) }

    /** Saves the point the owner marked and can see on the map. */
    fun confirmMapPoint() {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val draft = content.draft ?: return
        val point = content.pendingPoint ?: return
        viewModelScope.launch {
            save.location(draft.summary.id, point.latitude, point.longitude).onSuccess { updated ->
                mutate {
                    it.copy(draft = updated, pendingPoint = null, step = OnboardingStep.HOURS, message = null)
                }
            }.onFailure { failure ->
                mutate {
                    it.copy(
                        message = OnboardingMessage(
                            OnboardingNotice.LOCATION_SAVE_FAILED,
                            failure.toAppError(),
                        ),
                    )
                }
            }
        }
    }

    /** Marks where the owner is and brings the picker there; saving stays their decision. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            when (val result = locationProvider.current()) {
                is LocationResult.Available -> {
                    val point = MapPoint(result.fix.latitude, result.fix.longitude)
                    mutate {
                        it.copy(
                            pendingPoint = point,
                            pickerCamera = MapCamera(point, MapCameraPolicy.FACILITY_ZOOM),
                            message = null,
                        )
                    }
                }
                LocationResult.PermissionDenied ->
                    mutate { it.copy(message = OnboardingMessage(OnboardingNotice.LOCATION_PERMISSION)) }
                LocationResult.Unavailable ->
                    mutate { it.copy(message = OnboardingMessage(OnboardingNotice.LOCATION_UNAVAILABLE)) }
            }
        }
    }

    fun saveHours(rows: List<BusinessHour>) {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val id = content.draft?.summary?.id ?: return
        viewModelScope.launch {
            save.hours(id, rows).onSuccess {
                mutate { it.copy(step = OnboardingStep.PUBLIC_IMAGES, message = null) }
                refreshImages(id)
            }.onFailure { failure ->
                mutate {
                    it.copy(
                        message = OnboardingMessage(
                            OnboardingNotice.HOURS_SAVE_FAILED,
                            failure.toAppError(),
                        ),
                    )
                }
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
                mutate { it.copy(message = OnboardingMessage(OnboardingNotice.IMAGE_UNREADABLE)) }
                return@launch
            }
            val result = if (requirementId == null) {
                save.image(id, payload).map { Unit }
            } else {
                save.evidence(id, requirementId, payload).map { Unit }
            }
            result.onSuccess {
                if (requirementId == null) refreshImages(id)
                load.facility(id).onSuccess { updated ->
                    mutate {
                        it.copy(
                            draft = updated,
                            message = OnboardingMessage(OnboardingNotice.FILE_UPLOADED),
                            // A photo keeps the owner on the photos step, so a second one can
                            // follow; «التالي» moves on.
                            step = it.step,
                        )
                    }
                }
            }.onFailure { failure ->
                mutate {
                    it.copy(
                        message = OnboardingMessage(
                            OnboardingNotice.UPLOAD_FAILED,
                            failure.toAppError(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * The photos as the backend holds them. The step said «تم رفع الملف» and showed nothing, so
     * the owner could not tell which went up, nor take one back.
     */
    private suspend fun refreshImages(id: String) {
        load.images(id).onSuccess { images -> mutate { it.copy(images = images) } }
    }

    fun deleteImage(imageId: String) {
        val content = _state.value as? OnboardingUiState.Content ?: return
        val id = content.draft?.summary?.id ?: return
        viewModelScope.launch {
            save.deleteImage(id, imageId).onSuccess { refreshImages(id) }.onFailure { failure ->
                mutate {
                    it.copy(message = OnboardingMessage(OnboardingNotice.UPLOAD_FAILED, failure.toAppError()))
                }
            }
        }
    }

    // Straight to the documents: the specialised-fields step had nothing on it but a sentence.
    // Specialties and services are chosen on the facility's own page once it exists.
    fun nextFromMap() = mutate {
        it.copy(step = OnboardingStep.HOURS, message = null)
    }

    fun nextFromImages() = mutate {
        // The photos step's «تم رفع الملف» is about the photos; it does not follow to the next.
        it.copy(step = OnboardingStep.VERIFICATION_EVIDENCE, message = null)
    }

    fun nextFromSpecializedFields() = mutate {
        it.copy(step = OnboardingStep.VERIFICATION_EVIDENCE)
    }

    fun review() {
        mutate { it.copy(step = OnboardingStep.REVIEW) }
        // The review counts the photos; an owner who came back to the request has none read yet.
        val id = (_state.value as? OnboardingUiState.Content)?.draft?.summary?.id ?: return
        viewModelScope.launch { refreshImages(id) }
    }

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
                            message = OnboardingMessage(OnboardingNotice.SUBMITTED),
                            justSubmitted = true,
                        )
                    }
                }
            }.onFailure { failure ->
                mutate {
                    it.copy(
                        step = OnboardingStep.REVIEW,
                        busy = false,
                        message = OnboardingMessage(
                            OnboardingNotice.SUBMIT_FAILED,
                            failure.toAppError(),
                        ),
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
