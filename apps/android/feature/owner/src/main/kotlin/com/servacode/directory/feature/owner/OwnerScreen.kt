package com.servacode.directory.feature.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.servacode.directory.core.designsystem.DirectorySectionLabel
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.Radius
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
 * One card per facility, because a facility is one subject: its name and how it stands on the
 * first line, what it is under that, anything the platform is waiting for after that, and last
 * the two things its owner does with it. Adding another is a quieter button under the list —
 * an owner opens this page to tend what they have far more often than to add.
 *
 * The references show counts of views and reviews beside each facility; the owner API returns
 * no such figures, so none are shown rather than invented.
 */
@Composable
fun MyFacilitiesScreen(
    onAdd: () -> Unit,
    onManage: (String) -> Unit,
    onDuty: (String) -> Unit,
    /**
     * Null where this is a place in the bar rather than a screen on top of one.
     *
     * An owner reaches their facilities from the bar, and the root of a tab has nothing behind
     * it; opened from the profile it still does.
     */
    onBack: (() -> Unit)? = null,
    /** The app's own bar, where the owner has a place in it. */
    bottomBar: @Composable () -> Unit = {},
    viewModel: MyFacilitiesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DirectoryPage(
        topBar = { DirectoryTopBar(title = OwnerCopy.TITLE, onBack = onBack) },
        bottomBar = bottomBar,
    ) { padding ->
        when (val value = state) {
            MyFacilitiesUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is MyFacilitiesUiState.Error -> DirectoryErrorState(
                title = OwnerCopy.LIST_ERROR,
                modifier = Modifier.padding(padding),
                body = value.message,
                onRetry = viewModel::refresh,
            )
            is MyFacilitiesUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = OwnerCopy.EMPTY,
                    modifier = Modifier.padding(padding),
                    body = OwnerCopy.EMPTY_BODY,
                    icon = DirectoryIcons.hospital,
                    action = OwnerCopy.ADD,
                    onAction = onAdd,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        start = Space.screen,
                        end = Space.screen,
                        top = Space.base,
                        bottom = Space.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    items(value.items, key = { it.id }) { item ->
                        OwnerFacilityCard(
                            item = item,
                            onManage = { onManage(item.id) },
                            onDuty = { onDuty(item.id) },
                        )
                    }
                    item(key = "add") {
                        DirectorySecondaryButton(
                            text = OwnerCopy.ADD,
                            onClick = onAdd,
                            modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                        )
                    }
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
    DirectoryCard {
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
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
                text = "${item.category.nameAr} - ${item.province.nameAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // What the platform is waiting for, if anything: the one line on this card that is
            // work rather than description, so it sits apart on the brand's own soft green.
            item.requiredAction?.let { action ->
                Surface(
                    shape = RoundedCornerShape(Radius.medium),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    MetaRow(
                        icon = DirectoryIcons.info,
                        text = OwnerLabels.requiredAction(action),
                        modifier = Modifier.padding(horizontal = Space.md, vertical = Space.sm),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                // Two doors into the same facility, drawn alike: one filled and one outlined
                // says one of them is the real one, and neither is.
                DirectoryPrimaryButton(
                    text = OwnerCopy.MANAGE,
                    onClick = onManage,
                    modifier = Modifier.weight(1f),
                )
                if (OwnerCapabilities.supportsDuty(item)) {
                    DirectoryPrimaryButton(
                        text = OwnerCopy.DUTY,
                        onClick = onDuty,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The owner's own screen for one facility: what it is going through, its temporary closures,
 * and who else may manage it.
 *
 * Three subjects, three labelled cards. It was one long column where a date field, a list of
 * closures, a list of people and a field for an account id followed each other with nothing
 * between them, so nothing said where one job ended and the next began.
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screen, vertical = Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                val summary = value.facility.summary
                DirectoryCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.md),
                        ) {
                            Text(
                                text = summary.nameAr,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f).semantics { heading() },
                            )
                            StatusPill(OwnerLabels.status(summary.status), summary.status.tone())
                        }
                        Text(
                            text = "${summary.category.nameAr} - ${summary.province.nameAr}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        value.message?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        ) {
                            DirectoryPrimaryButton(
                                text = OwnerCopy.EDIT,
                                onClick = { onEdit(summary.id) },
                                modifier = Modifier.weight(1f),
                            )
                            if (OwnerCapabilities.supportsDuty(summary)) {
                                DirectoryPrimaryButton(
                                    text = OwnerCopy.DUTY,
                                    onClick = { onDuty(summary.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                if (OwnerCapabilities.supportsTemporaryClosure(summary)) {
                    Section(OwnerCopy.CLOSURES) {
                        if (value.closures.isEmpty()) {
                            Text(
                                text = OwnerCopy.CLOSURES_NONE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        value.closures.forEachIndexed { index, closure ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
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
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = OwnerCopy.CLOSURE_ADD,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        DateTimeField(
                            OwnerCopy.CLOSURE_START,
                            closureStart,
                            { closureStart = it },
                            Modifier.fillMaxWidth(),
                        )
                        DateTimeField(
                            OwnerCopy.CLOSURE_END,
                            closureEnd,
                            { closureEnd = it },
                            Modifier.fillMaxWidth(),
                        )
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
                    }
                }

                Section(OwnerCopy.MEMBERS) {
                    value.members.forEachIndexed { index, member ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = OwnerCopy.MANAGER_ADD,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // INT-084: a manager is still added by raw account id, because the API has
                    // no lookup by phone. The field says so rather than pretending otherwise.
                    DirectoryTextField(
                        value = managerId,
                        onValueChange = { managerId = it },
                        label = OwnerCopy.MANAGER_ID,
                        placeholder = OwnerCopy.MANAGER_ID_HINT,
                    )
                    Text(
                        text = OwnerCopy.MANAGER_NOTE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                }
                Spacer(Modifier.height(Space.lg))
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

/** A label and the card under it, the shape the rest of the app's pages are read in. */
@Composable
private fun Section(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        DirectorySectionLabel(label)
        DirectoryCard {
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm), content = content)
        }
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
    const val CLOSURES_NONE = "لا يوجد إغلاق مؤقت."
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
    const val MANAGER_NOTE = "تتم الإضافة بمعرف الحساب فقط؛ " +
        "لا يوفر الخادم بحثًا برقم الهاتف."
}
