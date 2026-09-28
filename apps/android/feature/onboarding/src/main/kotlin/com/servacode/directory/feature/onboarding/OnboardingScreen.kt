package com.servacode.directory.feature.onboarding

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.DirectoryActionBar
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectorySectionLabel
import com.servacode.directory.core.designsystem.DirectorySwitchRow
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.DirectoryWords
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.OwnerWords
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StatusPill
import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.designsystem.StepIndicator
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.network.NotificationPermissionPolicy

/**
 * Screens 19 to 22: an owner's facility from its first field to the review it is waiting on,
 * including where it stands on the map (Screen 20) and what proves it is real (Screen 21).
 *
 * The steps, the fields and the order are the backend's own registration flow, unchanged. The
 * location is still saved only when the owner says so, and a piece of evidence is still named
 * by the requirement it answers — never by where it was stored.
 */
@Composable
fun OnboardingScreen(
    styleUrl: String,
    onChooseProvince: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.useCurrentLocation() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::uploadPublicImage)
    }
    var evidenceRequirementId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // Either answer is fine: a refusal only means the review decision arrives without a notice.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val justSubmitted = (state as? OnboardingUiState.Content)?.justSubmitted == true
    LaunchedEffect(justSubmitted) {
        if (!justSubmitted) return@LaunchedEffect
        viewModel.notificationPromptHandled()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            if (NotificationPermissionPolicy.shouldRequest(Build.VERSION.SDK_INT, granted, viewModel.pushEnabled)) {
                notificationPermission.launch(permission)
            }
        }
    }
    val evidencePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val requirementId = evidenceRequirementId
        if (uri != null && requirementId != null) viewModel.uploadEvidence(requirementId, uri)
    }

    val content = state as? OnboardingUiState.Content
    DirectoryPage(
        topBar = { DirectoryTopBar(title = OnboardingCopy.TITLE, onBack = onBack) },
        bottomBar = {
            if (content != null) {
                StepAction(
                    value = content,
                    onSaveBasic = viewModel::saveBasicAndContinue,
                    onImagesDone = viewModel::nextFromImages,
                    onSpecializedDone = viewModel::nextFromSpecializedFields,
                    onReview = viewModel::review,
                    onSubmit = viewModel::submit,
                    onDone = onDone,
                )
            }
        },
    ) { padding ->
        when (val value = state) {
            OnboardingUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            OnboardingUiState.ProvinceRequired -> DirectoryEmptyState(
                title = OnboardingCopy.PROVINCE_REQUIRED,
                modifier = Modifier.padding(padding),
                icon = DirectoryIcons.pin,
                action = OnboardingCopy.PROVINCE_CHOOSE,
                onAction = onChooseProvince,
            )
            OnboardingUiState.Error -> DirectoryErrorState(
                title = OnboardingCopy.CONFIG_ERROR,
                modifier = Modifier.padding(padding),
                body = OnboardingCopy.CONFIG_ERROR_BODY,
            )
            is OnboardingUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Space.screen,
                    end = Space.screen,
                    bottom = Space.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                item(key = "phase") {
                    Column {
                        StepIndicator(
                            labels = OnboardingCopy.PHASES,
                            current = value.step.phase(),
                        )
                        Text(
                            text = stepLabel(value.step),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() },
                        )
                        value.message?.let { message ->
                            Text(
                                text = noticeText(message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Space.xs),
                            )
                        }
                    }
                }
                item(key = "step") {
                    when (value.step) {
                        OnboardingStep.PROVINCE_CATEGORY -> CategoryStep(value, viewModel::selectCategory)
                        OnboardingStep.BASIC_INFO -> BasicInfoStep(value, viewModel)
                        OnboardingStep.MAP_POINT -> MapPointStep(
                            value = value,
                            styleUrl = styleUrl,
                            onTap = viewModel::markMapPoint,
                            onConfirm = viewModel::confirmMapPoint,
                            onUseCurrent = {
                                locationPermission.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    ),
                                )
                            },
                        )
                        OnboardingStep.HOURS -> HoursEditor(
                            initial = value.draft?.hours.orEmpty(),
                            onSave = viewModel::saveHours,
                        )
                        OnboardingStep.PUBLIC_IMAGES -> PublicImagesStep(
                            onPick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        OnboardingStep.SPECIALIZED_FIELDS -> Text(
                            text = OnboardingCopy.SPECIALIZED_NOTE,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OnboardingStep.VERIFICATION_EVIDENCE -> EvidenceStep(
                            value = value,
                            onUpload = { requirementId ->
                                evidenceRequirementId = requirementId
                                evidencePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        OnboardingStep.REVIEW -> ReviewStep(value)
                        OnboardingStep.SUBMIT -> DirectoryInlineLoading(OnboardingCopy.SUBMITTING)
                        OnboardingStep.STATUS -> StatusStep(value)
                    }
                }
            }
        }
    }
}

/** The category decides what the rest of the form may even ask for, so it is chosen first. */
@Composable
private fun CategoryStep(value: OnboardingUiState.Content, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = value.config.province.nameAr,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        value.config.categories.forEach { config ->
            DirectorySecondaryButton(
                text = config.category.nameAr,
                onClick = { onSelect(config.category.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BasicInfoStep(value: OnboardingUiState.Content, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        DirectoryTextField(value.form.nameAr, viewModel::updateNameAr, OnboardingCopy.NAME_AR)
        DirectoryTextField(value.form.nameEn, viewModel::updateNameEn, OnboardingCopy.NAME_EN)
        DirectoryTextField(
            value = value.form.descriptionAr,
            onValueChange = viewModel::updateDescriptionAr,
            label = OnboardingCopy.DESCRIPTION,
            singleLine = false,
            minLines = 3,
        )
        // The backend names the fields it refused; each says so under itself, in Arabic.
        val refused = OnboardingField.named(value.message?.error)
        DirectoryTextField(
            value = value.form.phone,
            onValueChange = viewModel::updatePhone,
            label = OnboardingCopy.PHONE,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            error = if (OnboardingField.PHONE in refused) OnboardingCopy.PHONE_INVALID else null,
        )
        DirectoryTextField(
            value = value.form.whatsapp,
            onValueChange = viewModel::updateWhatsapp,
            label = OnboardingCopy.WHATSAPP,
            placeholder = OnboardingCopy.WHATSAPP_HINT,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            error = if (OnboardingField.WHATSAPP in refused) OnboardingCopy.WHATSAPP_INVALID else null,
        )
        DirectoryTextField(value.form.addressAr, viewModel::updateAddressAr, OnboardingCopy.ADDRESS)
    }
}

/** Screen 20. A tap only marks the point; saving it is a separate thing the owner asks for. */
@Composable
private fun MapPointStep(
    value: OnboardingUiState.Content,
    styleUrl: String,
    onTap: (MapPoint) -> Unit,
    onConfirm: () -> Unit,
    onUseCurrent: () -> Unit,
) {
    val saved = value.draft?.latitude?.let { latitude ->
        value.draft.longitude?.let { longitude -> MapPoint(latitude, longitude) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = OnboardingCopy.MAP_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (value.pickerReady) {
            OnboardingMapPicker(
                styleUrl = styleUrl,
                camera = value.pickerCamera,
                point = value.pendingPoint ?: saved,
                onTap = onTap,
            )
        } else {
            DirectoryInlineLoading(OnboardingCopy.MAP_WAITING)
        }
        saved?.let { MetaRow(DirectoryIcons.pin, OnboardingCopy.saved(it)) }
        value.pendingPoint?.let { point ->
            MetaRow(DirectoryIcons.plus, OnboardingCopy.marked(point))
            DirectoryPrimaryButton(
                text = OnboardingCopy.MAP_SAVE,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DirectorySecondaryButton(
            text = OnboardingCopy.MAP_CURRENT,
            onClick = onUseCurrent,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PublicImagesStep(onPick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = OnboardingCopy.IMAGES_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DirectorySecondaryButton(
            text = OnboardingCopy.IMAGES_PICK,
            onClick = onPick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Screen 21. Each requirement the category asks for, and a way to answer it. Nothing here shows
 * where a file was stored or links to it: this evidence is private to the review.
 */
@Composable
private fun EvidenceStep(value: OnboardingUiState.Content, onUpload: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = OnboardingCopy.EVIDENCE_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        value.selectedCategory?.verificationRequirements?.forEach { requirement ->
            DirectoryCard {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        DirectoryIcon(
                            icon = DirectoryIcons.document,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = requirement.labelAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        StatusPill(
                            text = if (requirement.required) {
                                OnboardingCopy.REQUIRED
                            } else {
                                OnboardingCopy.OPTIONAL
                            },
                            tone = if (requirement.required) StatusTone.PENDING else StatusTone.NEUTRAL,
                        )
                    }
                    requirement.instructionsAr?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DirectorySecondaryButton(
                        text = OnboardingCopy.EVIDENCE_UPLOAD,
                        onClick = { onUpload(requirement.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(value: OnboardingUiState.Content) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        DirectoryCard {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(
                    text = value.draft?.summary?.nameAr.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                value.selectedCategory?.category?.nameAr?.let {
                    MetaRow(DirectoryIcons.grid, it)
                }
                value.draft?.latitude?.let { latitude ->
                    value.draft.longitude?.let { longitude ->
                        MetaRow(DirectoryIcons.pin, OnboardingCopy.saved(MapPoint(latitude, longitude)))
                    }
                }
            }
        }
        Text(
            text = OnboardingCopy.REVIEW_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Screen 22. Where the application stands, in the backend's own words, and what follows. */
@Composable
private fun StatusStep(value: OnboardingUiState.Content) {
    val status = value.draft?.summary?.status
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        DirectoryCard {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                SUBMISSION_STATES.forEach { state ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.md),
                    ) {
                        val here = state == status
                        DirectoryIcon(
                            icon = if (here) DirectoryIcons.check else DirectoryIcons.clock,
                            contentDescription = null,
                            size = IconSize.small,
                            tint = if (here) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                        Text(
                            text = OwnerWords.status(state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (here) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        value.draft?.application?.rejectionReason?.let { reason ->
            DirectorySectionLabel(OnboardingCopy.REJECTION)
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        value.draft?.summary?.requiredAction?.let { action ->
            MetaRow(DirectoryIcons.info, OwnerWords.requiredAction(action))
        }
    }
}

/** The one thing the step in view is for, kept in sight at the bottom of the screen. */
@Composable
private fun StepAction(
    value: OnboardingUiState.Content,
    onSaveBasic: () -> Unit,
    onImagesDone: () -> Unit,
    onSpecializedDone: () -> Unit,
    onReview: () -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
) {
    val action = when (value.step) {
        OnboardingStep.PROVINCE_CATEGORY -> null
        OnboardingStep.BASIC_INFO -> OnboardingCopy.NEXT to onSaveBasic
        OnboardingStep.MAP_POINT -> null
        OnboardingStep.HOURS -> null
        OnboardingStep.PUBLIC_IMAGES -> OnboardingCopy.NEXT to onImagesDone
        OnboardingStep.SPECIALIZED_FIELDS -> OnboardingCopy.NEXT to onSpecializedDone
        OnboardingStep.VERIFICATION_EVIDENCE -> OnboardingCopy.TO_REVIEW to onReview
        OnboardingStep.REVIEW -> OnboardingCopy.SUBMIT to onSubmit
        OnboardingStep.SUBMIT -> null
        OnboardingStep.STATUS -> OnboardingCopy.BACK_TO_FACILITIES to onDone
    } ?: return
    DirectoryActionBar {
        DirectoryPrimaryButton(
            text = action.first,
            onClick = action.second,
            modifier = Modifier.fillMaxWidth(),
            loading = value.busy,
        )
    }
}

@Composable
private fun HoursEditor(
    initial: List<BusinessHour>,
    onSave: (List<BusinessHour>) -> Unit,
) {
    val days = remember(initial) {
        mutableStateListOf<List<HourSpan>>().apply { addAll(HoursForm.fromHours(initial)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = OnboardingCopy.HOURS_NOTE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        days.forEachIndexed { weekday, spans ->
            DirectoryCard {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    DirectorySwitchRow(
                        title = DirectoryWords.weekday(weekday),
                        checked = spans.isNotEmpty(),
                        onCheckedChange = { open ->
                            days[weekday] = if (open) listOf(HourSpan("09:00", "17:00")) else emptyList()
                        },
                    )
                    spans.forEachIndexed { index, span ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            DirectoryTextField(
                                value = span.opensAt,
                                onValueChange = { value ->
                                    days[weekday] = spans.toMutableList()
                                        .also { it[index] = span.copy(opensAt = value) }
                                },
                                label = OnboardingCopy.OPENS_AT,
                                modifier = Modifier.weight(1f),
                            )
                            DirectoryTextField(
                                value = span.closesAt,
                                onValueChange = { value ->
                                    days[weekday] = spans.toMutableList()
                                        .also { it[index] = span.copy(closesAt = value) }
                                },
                                label = OnboardingCopy.CLOSES_AT,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (spans.size > 1) {
                            DirectoryTextButton(
                                text = OnboardingCopy.SPAN_REMOVE,
                                onClick = {
                                    days[weekday] = spans.toMutableList().also { it.removeAt(index) }
                                },
                            )
                        }
                    }
                    if (spans.isNotEmpty()) {
                        DirectoryTextButton(
                            text = OnboardingCopy.SPAN_ADD,
                            onClick = { days[weekday] = spans + HourSpan("16:00", "22:00") },
                        )
                    }
                }
            }
        }
        DirectoryPrimaryButton(
            text = OnboardingCopy.HOURS_SAVE,
            onClick = { onSave(HoursForm.toHours(days)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Which of the three phases a step belongs to. */
private fun OnboardingStep.phase(): Int = when (this) {
    OnboardingStep.PROVINCE_CATEGORY,
    OnboardingStep.BASIC_INFO,
    OnboardingStep.MAP_POINT,
    OnboardingStep.HOURS,
    OnboardingStep.PUBLIC_IMAGES,
    OnboardingStep.SPECIALIZED_FIELDS,
    -> 0
    OnboardingStep.VERIFICATION_EVIDENCE -> 1
    OnboardingStep.REVIEW,
    OnboardingStep.SUBMIT,
    OnboardingStep.STATUS,
    -> 2
}

/** The states an application passes through, in the order a reviewer moves it. */
private val SUBMISSION_STATES = listOf(
    OwnerFacilityStatus.DRAFT,
    OwnerFacilityStatus.SUBMITTED,
    OwnerFacilityStatus.ACTIVE,
)

@Composable
@ReadOnlyComposable
private fun stepLabel(step: OnboardingStep): String = stringResource(
    when (step) {
        OnboardingStep.PROVINCE_CATEGORY -> R.string.onboarding_step_province_category
        OnboardingStep.BASIC_INFO -> R.string.onboarding_step_basic_info
        OnboardingStep.MAP_POINT -> R.string.onboarding_step_map_point
        OnboardingStep.HOURS -> R.string.onboarding_step_hours
        OnboardingStep.PUBLIC_IMAGES -> R.string.onboarding_step_public_images
        OnboardingStep.SPECIALIZED_FIELDS -> R.string.onboarding_step_specialized_fields
        OnboardingStep.VERIFICATION_EVIDENCE -> R.string.onboarding_step_verification_evidence
        OnboardingStep.REVIEW -> R.string.onboarding_step_review
        OnboardingStep.SUBMIT -> R.string.onboarding_step_submit
        OnboardingStep.STATUS -> R.string.onboarding_step_status
    },
)

/**
 * What the owner is told, in words.
 *
 * The view model named it; this turns the name into the reader's language. A failure carries the
 * error's own sentence as well, because "could not save" alone does not say what to do next.
 */
@Composable
@ReadOnlyComposable
private fun noticeText(message: OnboardingMessage): String = when (message.notice) {
    OnboardingNotice.DRAFT_SAVED -> stringResource(R.string.onboarding_notice_draft_saved)
    OnboardingNotice.FILE_UPLOADED -> stringResource(R.string.onboarding_notice_file_uploaded)
    OnboardingNotice.SUBMITTED -> stringResource(R.string.onboarding_notice_submitted)
    OnboardingNotice.LOCATION_PERMISSION ->
        stringResource(R.string.onboarding_notice_location_permission)
    OnboardingNotice.LOCATION_UNAVAILABLE ->
        stringResource(R.string.onboarding_notice_location_unavailable)
    OnboardingNotice.IMAGE_UNREADABLE -> stringResource(R.string.onboarding_notice_image_unreadable)
    OnboardingNotice.DRAFT_SAVE_FAILED ->
        stringResource(R.string.onboarding_notice_draft_save_failed, message.reason())
    OnboardingNotice.LOCATION_SAVE_FAILED ->
        stringResource(R.string.onboarding_notice_location_save_failed, message.reason())
    OnboardingNotice.HOURS_SAVE_FAILED ->
        stringResource(R.string.onboarding_notice_hours_save_failed, message.reason())
    OnboardingNotice.UPLOAD_FAILED ->
        stringResource(R.string.onboarding_notice_upload_failed, message.reason())
    OnboardingNotice.SUBMIT_FAILED ->
        stringResource(R.string.onboarding_notice_submit_failed, message.reason())
}

/** The error's own sentence, or the one for an error nobody named. */
@Composable
@ReadOnlyComposable
private fun OnboardingMessage.reason(): String =
    appErrorText(error ?: AppError(AppError.Kind.UNEXPECTED))

/**
 * The words of the owner's registration, read from the module's own resources.
 *
 * See `HomeCopy` for why each member is read in composition. The days of the week are not here:
 * they are the app's words rather than this screen's, so they come from `DirectoryWords`.
 */
object OnboardingCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_title)

    /** The three phases the steps are grouped into. */
    val PHASES: List<String>
        @Composable @ReadOnlyComposable
        get() = stringArrayResource(R.array.onboarding_phases).toList()

    val PROVINCE_REQUIRED: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_province_required)
    val PROVINCE_CHOOSE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_province_choose)
    val CONFIG_ERROR: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_config_error)
    val CONFIG_ERROR_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_config_error_body)
    val NAME_AR: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_name_ar)
    val NAME_EN: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_name_en)
    val DESCRIPTION: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_description)
    val PHONE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_phone)
    val PHONE_INVALID: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_phone_invalid)
    val WHATSAPP: String @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_whatsapp)
    val WHATSAPP_HINT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_whatsapp_hint)
    val WHATSAPP_INVALID: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_whatsapp_invalid)
    val ADDRESS: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_address)
    val MAP_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_map_note)
    val MAP_WAITING: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_map_waiting)
    val MAP_SAVE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_map_save)
    val MAP_CURRENT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_map_current)
    val HOURS_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_hours_note)
    val HOURS_SAVE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_hours_save)
    val OPENS_AT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_opens_at)
    val CLOSES_AT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_closes_at)
    val SPAN_ADD: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_span_add)
    val SPAN_REMOVE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_span_remove)
    val IMAGES_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_images_note)
    val IMAGES_PICK: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_images_pick)
    val SPECIALIZED_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_specialized_note)
    val EVIDENCE_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_evidence_note)
    val EVIDENCE_UPLOAD: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_evidence_upload)
    val REQUIRED: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_required)
    val OPTIONAL: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_optional)
    val REVIEW_NOTE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_review_note)
    val REJECTION: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_rejection)
    val SUBMITTING: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_submitting)
    val NEXT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_next)
    val TO_REVIEW: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_to_review)
    val SUBMIT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.onboarding_submit)
    val BACK_TO_FACILITIES: String
        @Composable @ReadOnlyComposable
        get() = stringResource(R.string.onboarding_back_to_facilities)

    @Composable
    @ReadOnlyComposable
    fun saved(point: MapPoint): String =
        stringResource(R.string.onboarding_saved_point, point.latitude, point.longitude)

    @Composable
    @ReadOnlyComposable
    fun marked(point: MapPoint): String =
        stringResource(R.string.onboarding_marked_point, point.latitude, point.longitude)
}
