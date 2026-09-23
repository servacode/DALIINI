package com.servacode.directory.feature.duty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DateTimeField
import com.servacode.directory.core.designsystem.DirectoryCard
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryPrimaryButton
import com.servacode.directory.core.designsystem.DirectorySecondaryButton
import com.servacode.directory.core.designsystem.DirectoryTextButton
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.SectionHeader
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.DamascusTime

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
    var start by remember { mutableStateOf<Long?>(null) }
    var end by remember { mutableStateOf<Long?>(null) }

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
                value.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.sm),
                    )
                }
                SectionHeader(DutyCopy.NEW_SHIFT)
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

                if (value.shifts.isNotEmpty()) SectionHeader(DutyCopy.SHIFTS)
                value.shifts.forEach { shift ->
                    DirectoryCard(modifier = Modifier.padding(vertical = Space.xs)) {
                        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            Text(
                                text = DamascusTime.period(
                                    shift.startsAtEpochMillis,
                                    shift.endsAtEpochMillis,
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                            ) {
                                DirectoryTextButton(DutyCopy.END_EARLY, { viewModel.endEarly(shift) })
                                DirectoryTextButton(DutyCopy.CANCEL, { viewModel.cancel(shift.id) })
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
    const val TITLE = "إدارة المناوبة"
    const val ERROR = "تعذر تحميل المناوبات"
    const val ERROR_BODY = "قد لا يدعم تصنيف هذه المنشأة المناوبة."
    const val NEW_SHIFT = "مناوبة جديدة"
    const val START = "وقت البداية"
    const val END = "وقت النهاية"
    const val SCHEDULE = "جدولة"
    const val START_NOW = "بدء الآن حتى وقت النهاية"
    const val SHIFTS = "المناوبات"
    const val END_EARLY = "إنهاء مبكر"
    const val CANCEL = "إلغاء"
}
