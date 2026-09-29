package com.servacode.directory.feature.owner

import com.servacode.directory.core.designsystem.StatusTones
import com.servacode.directory.core.designsystem.StatusChip
import androidx.annotation.DrawableRes
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.IconSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.closureText
import com.servacode.directory.core.designsystem.DateTimeField
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryConfirmDialog
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPill
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectorySection
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTextField
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.MetaRow
import com.servacode.directory.core.designsystem.OwnerWords
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.model.FacilityMemberRole
import com.servacode.directory.core.model.FacilityTag
import com.servacode.directory.core.model.OwnerFacilityStatus
import com.servacode.directory.core.model.OwnerFacilitySummary

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
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is MyFacilitiesUiState.Content -> if (value.items.isEmpty()) {
                DirectoryEmptyState(
                    title = OwnerCopy.EMPTY,
                    modifier = Modifier.padding(padding),
                    body = OwnerCopy.EMPTY_BODY,
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
                StatusChip(OwnerWords.status(item.status), item.status.tone())
            }
            Text(
                text = "${item.category.nameAr} - ${item.province.nameAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // What the platform is waiting for, if anything: the one line on this card that is
            // work rather than description, so it sits apart on the brand's own soft green.
            item.requiredAction?.let { action ->
                DirectoryPill(brand = true) {
                    MetaRow(
                        icon = DirectoryIcons.info,
                        text = OwnerWords.requiredAction(action),
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
    insightsViewModel: OwnerInsightsViewModel = hiltViewModel(),
    hoursViewModel: HoursConfirmationViewModel = hiltViewModel(),
    tagsViewModel: FacilityTagsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val insights by insightsViewModel.state.collectAsStateWithLifecycle()
    val hours by hoursViewModel.state.collectAsStateWithLifecycle()
    val tags by tagsViewModel.state.collectAsStateWithLifecycle()
    // Saved choices are an edit like any other: the facility is read again, and whatever the
    // backend made of it — a review, for one already published — shows on its card above.
    val tagsSaved = (tags as? FacilityTagsUiState.Content)?.saved == true
    LaunchedEffect(tagsSaved) { if (tagsSaved) viewModel.refresh() }
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
                error = value.error,
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
                            StatusChip(OwnerWords.status(summary.status), summary.status.tone())
                        }
                        Text(
                            text = "${summary.category.nameAr} - ${summary.province.nameAr}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        value.failure?.let {
                            Text(
                                text = appErrorText(it),
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

                LaunchedEffect(value.facility) { hoursViewModel.show(value.facility) }
                HoursConfirmationSection(
                    state = hours,
                    onConfirm = hoursViewModel::confirm,
                    onEdit = { onEdit(summary.id) },
                )

                LaunchedEffect(summary.id) { insightsViewModel.show(summary.id) }
                InsightsSection(insights, onRetry = insightsViewModel::refresh)

                LaunchedEffect(value.facility) { tagsViewModel.show(value.facility) }
                FacilityTagsSection(
                    state = tags,
                    onToggleSpecialty = tagsViewModel::toggleSpecialty,
                    onToggleService = tagsViewModel::toggleService,
                    onSave = tagsViewModel::save,
                    onRetry = tagsViewModel::retry,
                )

                if (OwnerCapabilities.supportsTemporaryClosure(summary)) {
                    DirectorySection(OwnerCopy.CLOSURES) {
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
                                    text = closureText(closure),
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

                DirectorySection(OwnerCopy.MEMBERS) {
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
                                    text = OwnerWords.role(member.role),
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

/** The colour a status is read in: the shared vocabulary's tone for it. */
internal fun OwnerFacilityStatus.tone(): StatusTone = StatusTones.facilityStatus(this)

/** The words of the owner's screens, provisional until product copy is approved. */
/**
 * Views, calls and directions over the last 30 days: what the listing has done for the owner.
 * Loading, failure and an empty window each say so in their own words.
 */
@Composable
private fun InsightsSection(state: OwnerInsightsUiState, onRetry: () -> Unit) {
    DirectorySection(OwnerCopy.INSIGHTS) {
        when (state) {
            OwnerInsightsUiState.Loading -> DirectoryInlineLoading(OwnerCopy.INSIGHTS_LOADING)
            is OwnerInsightsUiState.Error -> {
                Text(
                    text = OwnerCopy.INSIGHTS_ERROR,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                DirectoryTextButton(OwnerCopy.RETRY, onRetry)
            }
            is OwnerInsightsUiState.Empty -> Text(
                text = OwnerCopy.insightsEmpty(state.windowDays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is OwnerInsightsUiState.Content -> {
                Text(
                    text = OwnerCopy.insightsWindow(state.insights.windowDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    InsightFigure(state.insights.views, OwnerCopy.INSIGHTS_VIEWS, DirectoryIcons.eye)
                    InsightFigure(state.insights.calls, OwnerCopy.INSIGHTS_CALLS, DirectoryIcons.phone)
                    InsightFigure(state.insights.directions, OwnerCopy.INSIGHTS_DIRECTIONS, DirectoryIcons.route)
                }
            }
        }
    }
}

/**
 * «تأكيد أوقات الدوام», once a week: one tap says the hours are still right, and the public page's
 * «آخر تأكيد للمعلومات» moves with it. Hours that changed are edited instead.
 */
@Composable
private fun HoursConfirmationSection(
    state: HoursConfirmationUiState,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
) {
    when (state) {
        HoursConfirmationUiState.Hidden -> Unit
        HoursConfirmationUiState.Confirmed -> Text(
            text = OwnerCopy.HOURS_CONFIRMED,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        is HoursConfirmationUiState.Due -> DirectorySection(OwnerCopy.HOURS_CONFIRM_TITLE) {
            Text(
                text = OwnerCopy.HOURS_CONFIRM_BODY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.failure?.let {
                Text(
                    text = appErrorText(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            Spacer(Modifier.height(Space.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                DirectoryPrimaryButton(
                    text = OwnerCopy.HOURS_CONFIRM_ACTION,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    loading = state.sending,
                )
                DirectorySecondaryButton(
                    text = OwnerCopy.HOURS_CONFIRM_EDIT,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !state.sending,
                )
            }
        }
    }
}

/**
 * «التخصصات والخدمات»: what the facility offers, ticked from what its category lets owners pick,
 * so that people narrowing a list by a specialty or a service find it.
 *
 * Its own card with its own states, like the statistics: loading, a failure with a retry, no
 * connection (the ticks stay in sight and cannot change), and saving. A group without choices is
 * not drawn, and neither is the card when the category offers none.
 */
@Composable
private fun FacilityTagsSection(
    state: FacilityTagsUiState,
    onToggleSpecialty: (String) -> Unit,
    onToggleService: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        FacilityTagsUiState.Hidden -> Unit
        FacilityTagsUiState.Loading -> DirectorySection(OwnerCopy.TAGS) {
            DirectoryInlineLoading(OwnerCopy.TAGS_LOADING)
        }
        is FacilityTagsUiState.Error -> DirectorySection(OwnerCopy.TAGS) {
            Text(
                text = OwnerCopy.TAGS_ERROR,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                text = appErrorText(state.error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DirectoryTextButton(OwnerCopy.RETRY, onRetry)
        }
        is FacilityTagsUiState.Content -> DirectorySection(OwnerCopy.TAGS) {
            val form = state.form
            Text(
                text = OwnerCopy.TAGS_HINT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.offline) {
                DirectoryOfflineNotice(text = OwnerCopy.TAGS_OFFLINE)
            }
            if (form.choices.specialties.isNotEmpty()) {
                TagChoices(
                    label = OwnerCopy.SPECIALTIES,
                    choices = form.choices.specialties,
                    ticked = form.specialties,
                    enabled = state.editable,
                    onToggle = onToggleSpecialty,
                )
            }
            if (form.choices.services.isNotEmpty()) {
                TagChoices(
                    label = OwnerCopy.SERVICES,
                    choices = form.choices.services,
                    ticked = form.services,
                    enabled = state.editable,
                    onToggle = onToggleService,
                )
            }
            TagsStatus(state, onRefresh = onRetry)
            DirectoryPrimaryButton(
                text = OwnerCopy.TAGS_SAVE,
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSave,
                loading = state.saving,
            )
        }
    }
}

/** One group, every choice in sight and each ticked on its own, as a checklist is. */
@Composable
private fun TagChoices(
    label: String,
    choices: List<FacilityTag>,
    ticked: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        DirectoryChipRow {
            choices.forEach { choice ->
                DirectoryFilterChip(
                    text = choice.nameAr,
                    selected = choice.id in ticked,
                    onClick = { onToggle(choice.id) },
                    enabled = enabled,
                )
            }
        }
    }
}

/**
 * The one line that says how the save went: under way, done, or refused and why. A screen reader
 * hears it when it changes, because the button it follows says nothing while it spins.
 */
@Composable
private fun TagsStatus(state: FacilityTagsUiState.Content, onRefresh: () -> Unit) {
    val failure = state.failure
    val (text, color) = when {
        state.saving -> OwnerCopy.TAGS_SAVING to MaterialTheme.colorScheme.onSurfaceVariant
        failure == FacilityTagsFailure.ChoicesOutdated -> OwnerCopy.TAGS_OUTDATED to MaterialTheme.colorScheme.error
        failure is FacilityTagsFailure.Failed ->
            OwnerCopy.tagsSaveFailed(appErrorText(failure.error)) to MaterialTheme.colorScheme.error
        state.saved && !state.form.changed -> OwnerCopy.TAGS_SAVED to MaterialTheme.colorScheme.primary
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    if (failure == FacilityTagsFailure.ChoicesOutdated) {
        DirectoryTextButton(OwnerCopy.TAGS_REFRESH, onRefresh)
    }
}

@Composable
private fun InsightFigure(count: Int, label: String, @DrawableRes icon: Int) {
    Column(
        // "١٢ مشاهدة" as one statement, not a number and a word read apart.
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        DirectoryIcon(
            icon = icon,
            contentDescription = null,
            size = IconSize.medium,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = OwnerCopy.count(count),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

object OwnerCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_title)
    val ADD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_add)
    val MANAGE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manage)
    val DUTY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_duty)
    val EDIT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_edit)
    val INSIGHTS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_insights)
    val HOURS_CONFIRM_TITLE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_hours_confirm_title)
    val HOURS_CONFIRM_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_hours_confirm_body)
    val HOURS_CONFIRM_ACTION: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_hours_confirm_action)
    val HOURS_CONFIRM_EDIT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_hours_confirm_edit)
    val HOURS_CONFIRMED: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_hours_confirmed)
    val INSIGHTS_LOADING: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_insights_loading)
    val INSIGHTS_ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_insights_error)
    val INSIGHTS_VIEWS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_insights_views)
    val INSIGHTS_CALLS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_insights_calls)
    val INSIGHTS_DIRECTIONS: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_insights_directions)
    val RETRY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_retry)
    val TAGS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags)
    val TAGS_HINT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_hint)
    val SPECIALTIES: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_specialties)
    val SERVICES: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_services)
    val TAGS_LOADING: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_loading)
    val TAGS_ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_error)
    val TAGS_OFFLINE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_offline)
    val TAGS_SAVE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_save)
    val TAGS_SAVING: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_saving)
    val TAGS_SAVED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_saved)
    val TAGS_OUTDATED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_outdated)
    val TAGS_REFRESH: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_tags_refresh)

    /** A refused save, with the error's own sentence after it. */
    @Composable @ReadOnlyComposable
    fun tagsSaveFailed(reason: String): String = stringResource(R.string.owner_tags_save_failed, reason)

    @Composable @ReadOnlyComposable
    fun insightsWindow(days: Int): String = stringResource(R.string.owner_insights_window, days)

    @Composable @ReadOnlyComposable
    fun insightsEmpty(days: Int): String = stringResource(R.string.owner_insights_empty, days)

    /** A count in the reader's own digits. */
    @Composable @ReadOnlyComposable
    fun count(value: Int): String = stringResource(R.string.owner_count, value)
    val LIST_ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_list_error)
    val EMPTY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_empty)
    val EMPTY_BODY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_empty_body)
    val MANAGE_TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manage_title)
    val MANAGE_ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manage_error)
    val CLOSURES: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_closures)
    val CLOSURES_NONE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_closures_none)
    val CLOSURE_START: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_closure_start)
    val CLOSURE_END: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_closure_end)
    val CLOSURE_REASON: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_closure_reason)
    val CLOSURE_ADD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_closure_add)
    val DELETE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_delete)
    val MEMBERS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_members)
    val REMOVE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_remove)
    val REMOVE_TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_remove_title)
    val REMOVE_BODY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_remove_body)
    val MANAGER_ID: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manager_id)
    val MANAGER_ID_HINT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manager_id_hint)
    val MANAGER_ADD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manager_add)
    val MANAGER_NOTE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.owner_manager_note)
}
