package com.servacode.directory.feature.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DateTimeField
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryConfirmDialog
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.SectionHeader
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StatusPill
import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.model.ClosureText
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary
import com.servacode.directory.core.model.OwnerLabels

/**
 * Screen 18. What the owner has, where each one stands, and what is being asked of them.
 *
 * The references show counts of views and reviews beside each facility; the owner API returns
 * no such figures, so none are shown rather than invented.
 */
@Composable
fun MyFacilitiesScreen(
    onAdd: () -> Unit,
    onManage: (String) -> Unit,
    onDuty: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MyFacilitiesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = OwnerCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            MyFacilitiesUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is MyFacilitiesUiState.Error -> DirectoryErrorState(
                title = OwnerCopy.LIST_ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is MyFacilitiesUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = Space.xxl),
            ) {
                item(key = "add") {
                    DirectoryPrimaryButton(
                        text = OwnerCopy.ADD,
                        onClick = onAdd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space.screen, vertical = Space.sm),
                    )
                }
                if (value.items.isEmpty()) {
                    item(key = "empty") {
                        DirectoryEmptyState(
                            title = OwnerCopy.EMPTY,
                            body = OwnerCopy.EMPTY_BODY,
                            icon = DirectoryIcons.hospital,
                            modifier = Modifier.padding(top = Space.xl),
                        )
                    }
                }
                items(value.items, key = { it.id }) { item ->
                    OwnerFacilityCard(
                        item = item,
                        onManage = { onManage(item.id) },
                        onDuty = { onDuty(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerFacilityCard(
    item: OwnerFacilitySummary,
    onManage: () -> Unit,
    onDuty: () -> Unit,
) {
    DirectoryCard(
        modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                Text(
                    text = item.nameAr,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                StatusPill(OwnerLabels.status(item.status), item.status.tone())
            }
            Text(
                text = "${item.category.nameAr} • ${item.province.nameAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.requiredAction?.let { action ->
                MetaRow(DirectoryIcons.info, OwnerLabels.requiredAction(action))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                DirectoryPrimaryButton(OwnerCopy.MANAGE, onManage)
                if (OwnerCapabilities.supportsDuty(item)) {
                    DirectorySecondaryButton(OwnerCopy.DUTY, onDuty)
                }
            }
        }
    }
}

/**
 * The owner's own screen for one facility: what it is going through, its temporary closures,
 * and who else may manage it.
 */
@Composable
fun ManageFacilityScreen(
    onEdit: (String) -> Unit,
    onDuty: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ManageFacilityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var managerId by remember { mutableStateOf("") }
    var closureStart by remember { mutableStateOf<Long?>(null) }
    var closureEnd by remember { mutableStateOf<Long?>(null) }
    var closureReason by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<String?>(null) }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = OwnerCopy.MANAGE_TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            ManageFacilityUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is ManageFacilityUiState.Error -> DirectoryErrorState(
                title = OwnerCopy.MANAGE_ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is ManageFacilityUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Space.screen)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                val summary = value.facility.summary
                Text(
                    text = summary.nameAr,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Space.sm).semantics { heading() },
                )
                StatusPill(OwnerLabels.status(summary.status), summary.status.tone())
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    DirectoryPrimaryButton(OwnerCopy.EDIT, { onEdit(summary.id) })
                    if (OwnerCapabilities.supportsDuty(summary)) {
                        DirectorySecondaryButton(OwnerCopy.DUTY, { onDuty(summary.id) })
                    }
                }
                value.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (OwnerCapabilities.supportsTemporaryClosure(summary)) {
                    SectionHeader(OwnerCopy.CLOSURES)
                    DateTimeField(OwnerCopy.CLOSURE_START, closureStart, { closureStart = it }, Modifier.fillMaxWidth())
                    DateTimeField(OwnerCopy.CLOSURE_END, closureEnd, { closureEnd = it }, Modifier.fillMaxWidth())
                    DirectoryTextField(
                        value = closureReason,
                        onValueChange = { closureReason = it },
                        label = OwnerCopy.CLOSURE_REASON,
                    )
                    DirectoryPrimaryButton(
                        text = OwnerCopy.CLOSURE_ADD,
                        onClick = {
                            val start = closureStart ?: return@DirectoryPrimaryButton
                            val end = closureEnd ?: return@DirectoryPrimaryButton
                            viewModel.createTemporaryClosure(
                                start,
                                end,
                                closureReason.trim().ifBlank { null },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = closureStart != null && closureEnd != null,
                    )
                    value.closures.forEach { closure ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = ClosureText.of(closure),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            DirectoryTextButton(
                                text = OwnerCopy.DELETE,
                                onClick = { viewModel.deleteTemporaryClosure(closure.id) },
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                SectionHeader(OwnerCopy.MEMBERS)
                value.members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = OwnerLabels.role(member.role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (member.role == FacilityMemberRole.MANAGER) {
                            DirectoryTextButton(
                                text = OwnerCopy.REMOVE,
                                onClick = { removing = member.userId },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                // INT-084: a manager is still added by raw account id, because the API has no
                // lookup by phone. The field says so rather than pretending otherwise.
                DirectoryTextField(
                    value = managerId,
                    onValueChange = { managerId = it },
                    label = OwnerCopy.MANAGER_ID,
                    placeholder = OwnerCopy.MANAGER_ID_HINT,
                )
                DirectoryPrimaryButton(
                    text = OwnerCopy.MANAGER_ADD,
                    onClick = {
                        viewModel.addManager(managerId)
                        managerId = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = managerId.isNotBlank(),
                )
                Text(
                    text = OwnerCopy.MANAGER_NOTE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Space.xxl),
                )
            }
        }
    }

    removing?.let { userId ->
        DirectoryConfirmDialog(
            title = OwnerCopy.REMOVE_TITLE,
            body = OwnerCopy.REMOVE_BODY,
            confirm = OwnerCopy.REMOVE,
            onConfirm = {
                removing = null
                viewModel.removeMember(userId)
            },
            onDismiss = { removing = null },
            destructive = true,
        )
    }
}

/** The colour a status is read in; the word itself is the backend's. */
internal fun OwnerFacilityStatus.tone(): StatusTone = when (this) {
    OwnerFacilityStatus.ACTIVE -> StatusTone.POSITIVE
    OwnerFacilityStatus.SUBMITTED -> StatusTone.PENDING
    OwnerFacilityStatus.REVERIFICATION_REQUIRED -> StatusTone.PENDING
    OwnerFacilityStatus.SUSPENDED -> StatusTone.DANGER
    OwnerFacilityStatus.DRAFT -> StatusTone.NEUTRAL
    OwnerFacilityStatus.CLOSED -> StatusTone.NEUTRAL
}

/** The words of the owner's screens, provisional until product copy is approved. */
object OwnerCopy {
    const val TITLE = "منشآتي"
    const val ADD = "إضافة منشأة جديدة"
    const val MANAGE = "إدارة"
    const val DUTY = "المناوبة"
    const val EDIT = "تعديل البيانات"
    const val LIST_ERROR = "تعذر تحميل المنشآت"
    const val EMPTY = "لا توجد منشآت"
    const val EMPTY_BODY = "أضف منشأتك لتظهر في الدليل بعد المراجعة."
    const val MANAGE_TITLE = "إدارة المنشأة"
    const val MANAGE_ERROR = "تعذر تحميل إدارة المنشأة"
    const val CLOSURES = "الإغلاقات المؤقتة"
    const val CLOSURE_START = "بداية الإغلاق"
    const val CLOSURE_END = "نهاية الإغلاق"
    const val CLOSURE_REASON = "سبب الإغلاق - اختياري"
    const val CLOSURE_ADD = "إضافة إغلاق مؤقت"
    const val DELETE = "حذف"
    const val MEMBERS = "المدراء"
    const val REMOVE = "إزالة"
    const val REMOVE_TITLE = "إزالة المدير؟"
    const val REMOVE_BODY = "لن يعود بإمكانه إدارة هذه المنشأة."
    const val MANAGER_ID = "معرف حساب المدير"
    const val MANAGER_ID_HINT = "معرف الحساب كما يظهر للمستخدم"
    const val MANAGER_ADD = "إضافة مدير"
    const val MANAGER_NOTE = "تتم الإضافة بمعرف الحساب فقط؛ لا يوفر الخادم بحثًا برقم الهاتف."
}
