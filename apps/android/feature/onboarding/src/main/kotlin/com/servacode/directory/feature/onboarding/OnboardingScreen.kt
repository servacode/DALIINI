package com.servacode.directory.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.model.BusinessHour
import com.servacode.directory.core.model.OwnerLabels
import com.servacode.directory.core.network.NotificationPermissionPolicy

@Composable
fun OnboardingScreen(
    styleUrl: String,
    onChooseProvince: () -> Unit,
    onDone: () -> Unit,
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

    when (val value = state) {
        OnboardingUiState.Loading -> CircularProgressIndicator(Modifier.padding(24.dp))
        OnboardingUiState.ProvinceRequired -> Column(Modifier.padding(24.dp)) {
            Text("اختر المحافظة قبل إضافة منشأة")
            Button(onClick = onChooseProvince) { Text("اختيار المحافظة") }
        }
        OnboardingUiState.Error -> Text("تعذر تحميل إعدادات التسجيل", Modifier.padding(24.dp))
        is OnboardingUiState.Content -> LazyColumn(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("إضافة / تعديل منشأة", style = MaterialTheme.typography.headlineLarge)
                Text("الخطوة: ${stepLabel(value.step)}")
                value.message?.let { Text(it) }
            }
            when (value.step) {
                OnboardingStep.PROVINCE_CATEGORY -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${value.config.province.nameAr} • اختر التصنيف")
                        value.config.categories.forEach { config ->
                            Button(
                                onClick = { viewModel.selectCategory(config.category.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(config.category.nameAr) }
                        }
                    }
                }
                OnboardingStep.BASIC_INFO -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = value.form.nameAr,
                            onValueChange = viewModel::updateNameAr,
                            label = { Text("اسم المنشأة") },
                        )
                        OutlinedTextField(
                            value = value.form.nameEn,
                            onValueChange = viewModel::updateNameEn,
                            label = { Text("الاسم بالإنكليزية - اختياري") },
                        )
                        OutlinedTextField(
                            value = value.form.descriptionAr,
                            onValueChange = viewModel::updateDescriptionAr,
                            label = { Text("الوصف") },
                        )
                        OutlinedTextField(
                            value = value.form.phone,
                            onValueChange = viewModel::updatePhone,
                            label = { Text("الهاتف") },
                        )
                        OutlinedTextField(
                            value = value.form.addressAr,
                            onValueChange = viewModel::updateAddressAr,
                            label = { Text("العنوان") },
                        )
                        Button(onClick = viewModel::saveBasicAndContinue) { Text("حفظ ومتابعة") }
                    }
                }
                OnboardingStep.MAP_POINT -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("حدد نقطة المنشأة على الخريطة أو استخدم موقعك الحالي.")
                        OnboardingMapPicker(
                            styleUrl = styleUrl,
                            current = value.draft?.latitude?.let { latitude ->
                                value.draft.longitude?.let { longitude -> MapPoint(latitude, longitude) }
                            },
                            onSelected = { point ->
                                viewModel.selectMapPoint(point.latitude, point.longitude)
                            },
                        )
                        value.draft?.latitude?.let { Text("الموقع الحالي: $it, ${value.draft.longitude}") }
                        Button(
                            onClick = {
                                locationPermission.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                    )
                                )
                            },
                        ) { Text("استخدام موقعي الحالي") }
                    }
                }
                OnboardingStep.HOURS -> item {
                    HoursEditor(
                        initial = value.draft?.hours.orEmpty(),
                        onSave = viewModel::saveHours,
                    )
                }
                OnboardingStep.PUBLIC_IMAGES -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الصور العامة")
                        Button(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        ) { Text("اختيار صورة") }
                        OutlinedButton(onClick = viewModel::nextFromImages) {
                            Text("متابعة")
                        }
                    }
                }
                OnboardingStep.SPECIALIZED_FIELDS -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الحقول المتخصصة تُعرض حسب إمكانيات التصنيف.")
                        Button(onClick = viewModel::nextFromSpecializedFields) { Text("متابعة") }
                    }
                }
                OnboardingStep.VERIFICATION_EVIDENCE -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("إثباتات التحقق")
                        value.selectedCategory?.verificationRequirements?.forEach { requirement ->
                            OutlinedButton(
                                onClick = {
                                    evidenceRequirementId = requirement.id
                                    evidencePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            ) { Text("رفع: ${requirement.labelAr}") }
                        }
                        Button(onClick = viewModel::review) { Text("مراجعة الطلب") }
                    }
                }
                OnboardingStep.REVIEW -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("راجع البيانات قبل الإرسال.")
                        Text(value.draft?.summary?.nameAr.orEmpty())
                        Text("الإرسال يعيد التحقق من سياسة التسجيل والإثباتات الحالية.")
                        Button(onClick = viewModel::submit) { Text("إرسال للمراجعة") }
                    }
                }
                OnboardingStep.SUBMIT -> item { CircularProgressIndicator() }
                OnboardingStep.STATUS -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        value.draft?.summary?.status?.let { Text("الحالة: ${OwnerLabels.status(it)}") }
                        value.draft?.application?.rejectionReason?.let { Text("سبب الرفض: $it") }
                        Button(onClick = onDone) { Text("العودة إلى منشآتي") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoursEditor(
    initial: List<BusinessHour>,
    onSave: (List<BusinessHour>) -> Unit,
) {
    val dayNames = listOf(
        "الاثنين",
        "الثلاثاء",
        "الأربعاء",
        "الخميس",
        "الجمعة",
        "السبت",
        "الأحد",
    )
    val days = remember(initial) {
        mutableStateListOf<List<HourSpan>>().apply { addAll(HoursForm.fromHours(initial)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ساعات العمل")
        Text(
            "يمكن إضافة أكثر من فترة في اليوم. إذا كان وقت الإغلاق قبل وقت الفتح فالفترة تمتد بعد منتصف الليل.",
            style = MaterialTheme.typography.bodySmall,
        )
        days.forEachIndexed { weekday, spans ->
            Column(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = spans.isNotEmpty(),
                        onCheckedChange = { open ->
                            days[weekday] = if (open) listOf(HourSpan("09:00", "17:00")) else emptyList()
                        },
                    )
                    Text(dayNames[weekday], style = MaterialTheme.typography.titleMedium)
                }
                spans.forEachIndexed { index, span ->
                    OutlinedTextField(
                        value = span.opensAt,
                        onValueChange = { value ->
                            days[weekday] = spans.toMutableList().also { it[index] = span.copy(opensAt = value) }
                        },
                        label = { Text("يفتح HH:mm") },
                    )
                    OutlinedTextField(
                        value = span.closesAt,
                        onValueChange = { value ->
                            days[weekday] = spans.toMutableList().also { it[index] = span.copy(closesAt = value) }
                        },
                        label = { Text("يغلق HH:mm") },
                    )
                    if (spans.size > 1) {
                        OutlinedButton(
                            onClick = { days[weekday] = spans.toMutableList().also { it.removeAt(index) } },
                        ) { Text("حذف الفترة") }
                    }
                }
                if (spans.isNotEmpty()) {
                    OutlinedButton(onClick = { days[weekday] = spans + HourSpan("16:00", "22:00") }) {
                        Text("إضافة فترة")
                    }
                }
            }
        }
        Button(onClick = { onSave(HoursForm.toHours(days)) }) { Text("حفظ الساعات والمتابعة") }
    }
}

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
