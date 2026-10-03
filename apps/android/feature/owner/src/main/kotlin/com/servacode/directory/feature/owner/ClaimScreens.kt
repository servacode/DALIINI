package com.servacode.directory.feature.owner

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryConfirmDialog
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryInlineLoading
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySearchField
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectorySection
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.designsystem.StatusChip
import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.model.ClaimRequirement
import com.servacode.directory.core.model.ClaimStatus
import com.servacode.directory.core.model.ClaimableFacility
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.FacilityClaim

/**
 * «هذه منشأتي»: finding one's own facility among those nobody owns yet (DECISION-064).
 *
 * A name is all it takes; the list fills as the owner types. A facility that is not there at all
 * is added rather than claimed, so the empty answer offers that instead of a dead end.
 */
@Composable
fun ClaimSearchScreen(
    onClaim: (claimId: String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    viewModel: ClaimSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val opened by viewModel.opened.collectAsStateWithLifecycle()
    LaunchedEffect(opened) {
        opened?.let { claimId ->
            viewModel.consumeOpened()
            onClaim(claimId)
        }
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = ClaimCopy.SEARCH_TITLE, onBack = onBack) },
    ) { padding ->
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
            item(key = "intro") {
                Text(
                    text = ClaimCopy.SEARCH_INTRO,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "field") {
                DirectorySearchField(
                    value = state.query,
                    onValueChange = viewModel::query,
                    placeholder = ClaimCopy.SEARCH_FIELD,
                )
            }
            val failure = state.failure
            val results = state.results
            when {
                state.searching -> item(key = "searching") { DirectoryInlineLoading(ClaimCopy.SEARCHING) }
                failure != null && results == null -> item(key = "error") {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Text(
                            text = ClaimCopy.SEARCH_ERROR,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Text(
                            text = appErrorText(failure),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DirectoryTextButton(OwnerCopy.RETRY, viewModel::retry)
                    }
                }
                results == null -> item(key = "short") {
                    Text(
                        text = ClaimCopy.SEARCH_SHORT,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                results.isEmpty() -> item(key = "none") {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        Text(
                            text = ClaimCopy.SEARCH_NONE,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        Text(
                            text = ClaimCopy.SEARCH_NONE_BODY,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DirectorySecondaryButton(
                            text = ClaimCopy.SEARCH_ADD,
                            onClick = onAdd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                else -> {
                    // A refused start (another open claim, too many claims) is said above the list.
                    failure?.let {
                        item(key = "failure") {
                            Text(
                                text = appErrorText(it),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    items(results, key = { it.id }) { facility ->
                        ClaimableCard(
                            facility = facility,
                            starting = state.starting == facility.id,
                            enabled = state.starting == null,
                            onClaim = { viewModel.start(facility.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClaimableCard(
    facility: ClaimableFacility,
    starting: Boolean,
    enabled: Boolean,
    onClaim: () -> Unit,
) {
    DirectoryCard {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(
                text = facility.nameAr,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = listOfNotNull(facility.categoryNameAr, facility.provinceNameAr, facility.cityNameAr)
                    .joinToString(" - "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            facility.addressAr?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DirectoryPrimaryButton(
                text = ClaimCopy.START,
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                loading = starting,
            )
        }
    }
}

/**
 * One claim: what it is for, where it stands, the documents the facility's category asks for,
 * and what can be done with it now. A draft takes documents and is sent; an open claim can be
 * withdrawn; a refused one says why and offers a new one.
 */
@Composable
fun ClaimScreen(
    onWithdrawn: () -> Unit,
    onReopened: (claimId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ClaimViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val withdrawn by viewModel.withdrawn.collectAsStateWithLifecycle()
    val reopened by viewModel.reopened.collectAsStateWithLifecycle()
    LaunchedEffect(withdrawn) { if (withdrawn) onWithdrawn() }
    LaunchedEffect(reopened) {
        reopened?.let { claimId ->
            viewModel.consumeReopened()
            onReopened(claimId)
        }
    }
    var requirementId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmWithdraw by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        val requirement = requirementId
        if (uri != null && requirement != null) viewModel.upload(requirement, uri)
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = ClaimCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            ClaimUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            is ClaimUiState.Error -> DirectoryErrorState(
                title = ClaimCopy.ERROR,
                modifier = Modifier.padding(padding),
                error = value.error,
                onRetry = viewModel::refresh,
            )
            is ClaimUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.screen, vertical = Space.base),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                val claim = value.claim
                ClaimSummaryCard(claim)
                if (value.unreadable || value.failure != null) {
                    Text(
                        text = value.failure?.let { appErrorText(it) } ?: ClaimCopy.UNREADABLE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (claim.status == ClaimStatus.DRAFT) {
                    DirectorySection(ClaimCopy.DOCUMENTS) {
                        if (claim.requirements.isEmpty()) {
                            Text(
                                text = ClaimCopy.DOCUMENTS_NONE,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = ClaimCopy.DOCUMENTS_PRIVATE,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        claim.requirements.forEachIndexed { index, requirement ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            RequirementRow(
                                claim = claim,
                                requirement = requirement,
                                uploading = value.uploading == requirement.id,
                                enabled = value.uploading == null && !value.busy,
                                onUpload = {
                                    requirementId = requirement.id
                                    picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                                },
                                onDelete = viewModel::deleteEvidence,
                            )
                        }
                    }
                    if (claim.missing.isNotEmpty()) {
                        Text(
                            text = ClaimCopy.missing(
                                claim.missing.joinToString(OwnerCopy.FIELD_SEPARATOR) { it.labelAr },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DirectoryPrimaryButton(
                        text = ClaimCopy.SUBMIT,
                        onClick = viewModel::submit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = claim.canSubmit && value.uploading == null,
                        loading = value.busy,
                    )
                }
                if (claim.status == ClaimStatus.REJECTED) {
                    DirectoryPrimaryButton(
                        text = ClaimCopy.AGAIN,
                        onClick = viewModel::startAgain,
                        modifier = Modifier.fillMaxWidth(),
                        loading = value.busy,
                    )
                }
                if (claim.canWithdraw) {
                    DirectorySecondaryButton(
                        text = ClaimCopy.WITHDRAW,
                        onClick = { confirmWithdraw = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !value.busy && value.uploading == null,
                    )
                }
                Spacer(Modifier.height(Space.lg))
            }
        }
    }

    if (confirmWithdraw) {
        DirectoryConfirmDialog(
            title = ClaimCopy.WITHDRAW_TITLE,
            body = ClaimCopy.WITHDRAW_BODY,
            confirm = ClaimCopy.WITHDRAW,
            onConfirm = {
                confirmWithdraw = false
                viewModel.withdraw()
            },
            onDismiss = { confirmWithdraw = false },
            destructive = true,
        )
    }
}

@Composable
private fun ClaimSummaryCard(claim: FacilityClaim) {
    DirectoryCard {
        Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                Text(
                    text = claim.facilityNameAr,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                StatusChip(ClaimCopy.status(claim.status), claim.status.tone())
            }
            Text(
                text = "${claim.categoryNameAr} - ${claim.provinceNameAr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            claim.addressAr?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                text = ClaimCopy.statusBody(claim),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (claim.status == ClaimStatus.REJECTED) {
                Text(
                    text = ClaimCopy.REJECTED_AGAIN,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One requirement: whether it is required, what is uploaded for it, and a way to add more. */
@Composable
private fun RequirementRow(
    claim: FacilityClaim,
    requirement: ClaimRequirement,
    uploading: Boolean,
    enabled: Boolean,
    onUpload: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val files = claim.evidence.filter { it.requirementId == requirement.id }
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                text = if (requirement.required) ClaimCopy.REQUIRED else ClaimCopy.OPTIONAL,
                tone = if (requirement.required) StatusTone.WARNING else StatusTone.NEUTRAL,
            )
        }
        Text(
            text = ClaimCopy.files(files.size, requirement.maxFiles),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        files.forEachIndexed { index, file ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = ClaimCopy.file(index + 1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                DirectoryTextButton(
                    text = ClaimCopy.FILE_DELETE,
                    onClick = { onDelete(file.id) },
                    enabled = enabled,
                )
            }
        }
        if (uploading) {
            DirectoryInlineLoading(ClaimCopy.UPLOADING)
        } else if (files.size < requirement.maxFiles) {
            DirectorySecondaryButton(
                text = ClaimCopy.UPLOAD,
                onClick = onUpload,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
            )
        }
    }
}

internal fun ClaimStatus.tone(): StatusTone = when (this) {
    ClaimStatus.DRAFT -> StatusTone.NEUTRAL
    ClaimStatus.SUBMITTED -> StatusTone.WARNING
    ClaimStatus.APPROVED -> StatusTone.POSITIVE
    ClaimStatus.REJECTED -> StatusTone.DANGER
}

object ClaimCopy {
    val SEARCH_TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_title)
    val SEARCH_INTRO: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_intro)
    val SEARCH_FIELD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_field)
    val SEARCH_SHORT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_short)
    val SEARCHING: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_searching)
    val SEARCH_NONE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_none)
    val SEARCH_NONE_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_none_body)
    val SEARCH_ADD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_add)
    val SEARCH_ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_search_error)
    val START: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_start)
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_title)
    val ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_error)
    val REJECTED_AGAIN: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_rejected_again)
    val DOCUMENTS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_documents)
    val DOCUMENTS_NONE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_documents_none)
    val DOCUMENTS_PRIVATE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_documents_private)
    val REQUIRED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_required)
    val OPTIONAL: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_optional)
    val UPLOAD: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_upload)
    val UPLOADING: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_uploading)
    val FILE_DELETE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_file_delete)
    val UNREADABLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_unreadable)
    val SUBMIT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_submit)
    val WITHDRAW: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_withdraw)
    val WITHDRAW_TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_withdraw_title)
    val WITHDRAW_BODY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_withdraw_body)
    val AGAIN: String @Composable @ReadOnlyComposable get() = stringResource(R.string.claim_again)

    @Composable @ReadOnlyComposable
    fun status(status: ClaimStatus): String = stringResource(
        when (status) {
            ClaimStatus.DRAFT -> R.string.claim_status_draft
            ClaimStatus.SUBMITTED -> R.string.claim_status_submitted
            ClaimStatus.APPROVED -> R.string.claim_status_approved
            ClaimStatus.REJECTED -> R.string.claim_status_rejected
        },
    )

    /** What the status means for the owner, in a sentence. */
    @Composable @ReadOnlyComposable
    fun statusBody(claim: FacilityClaim): String = when (claim.status) {
        ClaimStatus.DRAFT -> stringResource(R.string.claim_draft_body)
        ClaimStatus.SUBMITTED -> stringResource(
            R.string.claim_submitted_body,
            claim.submittedAtEpochMillis?.let(DamascusTime::format).orEmpty(),
        )
        ClaimStatus.APPROVED -> stringResource(R.string.claim_approved_body)
        ClaimStatus.REJECTED -> stringResource(R.string.claim_rejected_body, claim.rejectionReason.orEmpty())
    }

    @Composable @ReadOnlyComposable
    fun files(count: Int, max: Int): String = stringResource(R.string.claim_files, count, max)

    @Composable @ReadOnlyComposable
    fun file(number: Int): String = stringResource(R.string.claim_file, number)

    @Composable @ReadOnlyComposable
    fun missing(labels: String): String = stringResource(R.string.claim_missing, labels)
}
