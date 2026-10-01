package com.servacode.directory.feature.duty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIllustrations
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.Space
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * «المناوبات»: who is on duty in the province today, tomorrow or over the week — what the site's
 * `/duty` link opens. The roster is the backend's; this only picks the days and draws them.
 */
@Composable
fun DutyRosterScreen(
    onFacility: (String) -> Unit,
    onProvince: () -> Unit,
    onBack: () -> Unit,
    viewModel: DutyRosterViewModel = hiltViewModel(),
) {
    val range by viewModel.range.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    DirectoryPage(
        topBar = { DirectoryTopBar(title = RosterCopy.TITLE, onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            item(key = "range") {
                DirectoryChipRow(
                    Modifier
                        .padding(horizontal = Space.screen, vertical = Space.sm)
                        .semantics { selectableGroup() },
                ) {
                    RosterRange.entries.forEach { option ->
                        DirectoryFilterChip(
                            text = RosterCopy.range(option),
                            selected = range == option,
                            onClick = { viewModel.select(option) },
                        )
                    }
                }
            }
            when (val value = state) {
                DutyRosterUiState.Loading -> item(key = "loading") {
                    DirectoryLoading(Modifier.padding(top = Space.xxl))
                }
                DutyRosterUiState.ProvinceRequired -> item(key = "province") {
                    DirectoryEmptyState(
                        title = RosterCopy.PROVINCE,
                        illustration = DirectoryIllustrations.location,
                        action = RosterCopy.PROVINCE_ACTION,
                        onAction = onProvince,
                        modifier = Modifier.padding(top = Space.xxl),
                    )
                }
                is DutyRosterUiState.Error -> item(key = "error") {
                    DirectoryErrorState(
                        title = RosterCopy.ERROR,
                        error = value.error,
                        onRetry = viewModel::refresh,
                        modifier = Modifier.padding(top = Space.xxl),
                    )
                }
                is DutyRosterUiState.Content -> if (value.isEmpty) {
                    item(key = "empty") {
                        DirectoryEmptyState(
                            title = RosterCopy.EMPTY,
                            illustration = DirectoryIllustrations.noResults,
                            modifier = Modifier.padding(top = Space.xxl),
                        )
                    }
                } else {
                    value.days.filter { it.facilities.isNotEmpty() }.forEach { day ->
                        // A heading per day when the week is shown; one day needs none.
                        if (value.days.size > 1) {
                            item(key = "day-" + day.date) {
                                Text(
                                    text = RosterCopy.day(day.date),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Space.screen, vertical = Space.sm)
                                        .semantics { heading() },
                                )
                            }
                        }
                        items(day.facilities, key = { day.date + "-" + it.id }) { facility ->
                            FacilityCard(
                                facility = facility,
                                onClick = { onFacility(facility.id) },
                                modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                            )
                        }
                    }
                }
            }
        }
    }
}

object RosterCopy {
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.roster_title)
    val ERROR: String @Composable @ReadOnlyComposable get() = stringResource(R.string.roster_error)
    val EMPTY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.roster_empty)
    val PROVINCE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.roster_province)
    val PROVINCE_ACTION: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.roster_province_action)

    @Composable
    @ReadOnlyComposable
    fun range(range: RosterRange): String = stringResource(
        when (range) {
            RosterRange.TODAY -> R.string.roster_today
            RosterRange.TOMORROW -> R.string.roster_tomorrow
            RosterRange.WEEK -> R.string.roster_week
        },
    )

    /** "الثلاثاء 29/09": the weekday in the reader's language, the date in digits. */
    fun day(date: String): String = runCatching {
        val day = LocalDate.parse(date)
        val name = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ar"))
        "%s %02d/%02d".format(Locale.ROOT, name, day.dayOfMonth, day.monthValue)
    }.getOrDefault(date)
}
