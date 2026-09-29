package com.servacode.directory.feature.duty

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.appErrorText
import com.servacode.directory.core.designsystem.DateTimeField
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectorySectionLabel
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.DirectoryWords
import com.servacode.directory.core.designsystem.Space

/**
 * The owner's duty shifts. Not one of the numbered screens, and not a screen to remove either:
 * "on duty now" is what the whole directory turns on for a pharmacy, and this is where an owner
 * says when theirs is.
 */
@Composable
fun DutyScreen(
    onBack: () -> Unit,
    viewModel: DutyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    var start by remember { mutableStateOf<Long?>(null) }
    var end by remember { mutableStateOf<Long?>(null) }
    // A preset or a gap nudge fills the form; the owner still confirms with «جدولة».
    LaunchedEffect(draft) {
        draft?.let {
            start = it.startsAt
            end = it.endsAt
            viewModel.draftShown()
        }
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = DutyCopy.TITLE, onBack = onBack) },
    ) { padding ->
        when (val value = state) {
            DutyUiState.Loading -> DirectoryLoading(Modifier.padding(padding))
            DutyUiState.Error -> DirectoryErrorState(
                title = DutyCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = DutyCopy.ERROR_BODY,
                onRetry = viewModel::refresh,
            )
            is DutyUiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Space.screen)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                value.problem?.let { problem ->
                    Text(
                        text = DutyCopy.problem(problem),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = Space.sm)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                value.failure?.let {
                    Text(
                        text = appErrorText(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = Space.sm)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                DirectorySectionLabel(DutyCopy.NEW_SHIFT)
                // The two shifts owners schedule most, one tap each, into the form below.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    DirectorySecondaryButton(
                        text = DutyCopy.TONIGHT,
                        onClick = viewModel::presetTonight,
                        modifier = Modifier.weight(1f),
                    )
                    DirectorySecondaryButton(
                        text = DutyCopy.TOMORROW,
                        onClick = viewModel::presetTomorrow,
                        modifier = Modifier.weight(1f),
                    )
                }
                DateTimeField(DutyCopy.START, start, { start = it }, Modifier.fillMaxWidth())
                DateTimeField(DutyCopy.END, end, { end = it }, Modifier.fillMaxWidth())
                DirectoryPrimaryButton(
                    text = DutyCopy.SCHEDULE,
                    onClick = {
                        viewModel.schedule(
                            start ?: return@DirectoryPrimaryButton,
                            end ?: return@DirectoryPrimaryButton,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = start != null && end != null,
                )
                DirectorySecondaryButton(
                    text = DutyCopy.START_NOW,
                    onClick = { viewModel.startNow(end ?: return@DirectorySecondaryButton) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = end != null,
                )

                if (value.shifts.isNotEmpty()) DirectorySectionLabel(DutyCopy.SHIFTS)
                value.shifts.forEach { shift ->
                    DirectoryCard(modifier = Modifier.padding(vertical = Space.xs)) {
                        val period = DirectoryWords.period(shift.startsAtEpochMillis, shift.endsAtEpochMillis)
                        val endEarly = DutyCopy.END_EARLY
                        val cancel = DutyCopy.CANCEL
                        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            Text(
                                text = period,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            ) {
                                // Every shift has the same two buttons; each says which shift it acts on.
                                DirectoryTextButton(
                                    endEarly,
                                    { viewModel.endEarly(shift) },
                                    Modifier.semantics { contentDescription = "$endEarly: $period" },
                                )
                                DirectoryTextButton(
                                    cancel,
                                    { viewModel.cancel(shift.id) },
                                    Modifier.semantics { contentDescription = "$cancel: $period" },
                                )
                            }
                        }
                    }
                }
                Column(Modifier.padding(bottom = Space.xxl)) { }
            }
        }
    }
}

/** The words of the duty screen, provisional until product copy is approved. */
object DutyCopy {
    val INVALID_TIMES: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_invalid_times)
    val TONIGHT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_tonight)
    val TOMORROW: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_tomorrow)

    /** The app's own objection to the times, naming the shift or closure it clashes with. */
    @Composable
    @ReadOnlyComposable
    fun problem(problem: DutyProblem): String = when (problem) {
        DutyProblem.InvalidRange -> stringResource(R.string.duty_invalid_times)
        DutyProblem.InPast -> stringResource(R.string.duty_in_past)
        is DutyProblem.Overlaps -> stringResource(
            R.string.duty_overlaps,
            DirectoryWords.period(problem.shift.startsAtEpochMillis, problem.shift.endsAtEpochMillis),
        )
        is DutyProblem.DuringClosure -> stringResource(
            R.string.duty_during_closure,
            DirectoryWords.period(problem.closure.startsAtEpochMillis, problem.closure.endsAtEpochMillis),
        )
    }

    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_title)
    val ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_error)
    val ERROR_BODY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_error_body)
    val NEW_SHIFT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_new_shift)
    val START: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_start)
    val END: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_end)
    val SCHEDULE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_schedule)
    val START_NOW: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_start_now)
    val SHIFTS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_shifts)
    val END_EARLY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_end_early)
    val CANCEL: String @Composable @ReadOnlyComposable get() = stringResource(R.string.duty_cancel)
}
