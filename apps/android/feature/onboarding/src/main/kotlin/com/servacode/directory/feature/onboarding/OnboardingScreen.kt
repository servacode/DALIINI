package com.servacode.directory.feature.onboarding

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.servacode.directory.core.designsystem.IconSize
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StatusPill
import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.designsystem.StepIndicator
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerLabels
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
                        value.message?.let {
                            Text(
                                text = it,
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
        DirectoryTextField(value.form.phone, viewModel::updatePhone, OnboardingCopy.PHONE)
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
                            text = OwnerLabels.status(state),
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
            MetaRow(DirectoryIcons.info, OwnerLabels.requiredAction(action))
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
                        title = OnboardingCopy.DAY_NAMES[weekday],
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

private fun stepLabel(step: OnboardingStep): String = when (step) {
    OnboardingStep.PROVINCE_CATEGORY -> "المحافظة والتصنيف"
    OnboardingStep.BASIC_INFO -> "البيانات الأساسية"
    OnboardingStep.MAP_POINT -> "الموقع"
    OnboardingStep.HOURS -> "ساعات العمل"
    OnboardingStep.PUBLIC_IMAGES -> "الصور العامة"
    OnboardingStep.SPECIALIZED_FIELDS -> "الحقول المتخصصة"
    OnboardingStep.VERIFICATION_EVIDENCE -> "إثباتات التحقق"
    OnboardingStep.REVIEW -> "المراجعة"
    OnboardingStep.SUBMIT -> "الإرسال"
    OnboardingStep.STATUS -> "الحالة"
}

/** The words of the owner's registration, provisional until product copy is approved. */
object OnboardingCopy {
    const val TITLE = "إضافة / تعديل منشأة"
    val PHASES = listOf("المنشأة", "الإثباتات", "المراجعة")
    val DAY_NAMES = listOf(
        "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد",
    )
    const val PROVINCE_REQUIRED = "اختر المحافظة قبل إضافة منشأة"
    const val PROVINCE_CHOOSE = "اختيار المحافظة"
    const val CONFIG_ERROR = "تعذر تحميل إعدادات التسجيل"
    const val CONFIG_ERROR_BODY = "أعد المحاولة بعد قليل."
    const val NAME_AR = "اسم المنشأة"
    const val NAME_EN = "الاسم بالإنكليزية - اختياري"
    const val DESCRIPTION = "الوصف"
    const val PHONE = "الهاتف"
    const val ADDRESS = "العنوان"
    const val MAP_NOTE = "حدد نقطة المنشأة على الخريطة أو استخدم موقعك الحالي."
    const val MAP_WAITING = "جارٍ تجهيز الخريطة…"
    const val MAP_SAVE = "حفظ هذا الموقع"
    const val MAP_CURRENT = "استخدام موقعي الحالي"
    const val HOURS_NOTE = "يمكن إضافة أكثر من فترة في اليوم. إذا كان وقت الإغلاق قبل وقت الفتح " +
        "فالفترة تمتد بعد منتصف الليل."
    const val HOURS_SAVE = "حفظ الساعات والمتابعة"
    const val OPENS_AT = "يفتح HH:mm"
    const val CLOSES_AT = "يغلق HH:mm"
    const val SPAN_ADD = "إضافة فترة"
    const val SPAN_REMOVE = "حذف الفترة"
    const val IMAGES_NOTE = "هذه الصور تظهر للجميع في صفحة المنشأة."
    const val IMAGES_PICK = "اختيار صورة"
    const val SPECIALIZED_NOTE = "الحقول المتخصصة تُعرض حسب إمكانيات التصنيف."
    const val EVIDENCE_NOTE = "الإثباتات خاصة بالمراجعة ولا تظهر في صفحة المنشأة."
    const val EVIDENCE_UPLOAD = "رفع ملف"
    const val REQUIRED = "مطلوب"
    const val OPTIONAL = "اختياري"
    const val REVIEW_NOTE = "الإرسال يعيد التحقق من سياسة التسجيل والإثباتات الحالية."
    const val REJECTION = "سبب الرفض"
    const val SUBMITTING = "جارٍ الإرسال…"
    const val NEXT = "التالي"
    const val TO_REVIEW = "مراجعة الطلب"
    const val SUBMIT = "إرسال للمراجعة"
    const val BACK_TO_FACILITIES = "العودة إلى منشآتي"

    fun saved(point: MapPoint): String = "الموقع المحفوظ: ${point.latitude}، ${point.longitude}"

    fun marked(point: MapPoint): String = "النقطة المختارة: ${point.latitude}، ${point.longitude}"
}
