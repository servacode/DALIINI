package com.servacode.directory.feature.duty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryChipRow
import com.servacode.directory.core.designsystem.DirectoryEmptyState
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryFilterChip
import com.servacode.directory.core.designsystem.DirectoryIllustrations
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.FacilityCard
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.DamascusTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * «المناوبات»: who is on duty in the province today, tomorrow or over the week — what the site's
 * `/duty` link opens. The roster is the backend's; this only picks the days and draws them.
 */
@Composable
fun DutyRosterScreen(
    viewModel: DutyRosterViewModel,
    onFacility: (String) -> Unit,
    onProvince: () -> Unit,
    onBack: () -> Unit,
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
                    if (value.kept) {
                        item(key = "kept") {
                            DirectoryOfflineNotice(
                                modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                                onRetry = viewModel::refresh,
                            )
                        }
                    }
                    // Every day of a week, the empty ones too: a Wednesday left out of the list
                    // read as nothing at all, when it is the one day nobody is on duty.
                    value.days.filter { it.facilities.isNotEmpty() || value.days.size > 1 }.forEach { day ->
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
                        if (day.facilities.isEmpty()) {
                            item(key = "none-" + day.date) {
                                Text(
                                    text = RosterCopy.NO_DUTY_DAY,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                                )
                            }
                        }
                        items(day.facilities, key = { day.date + "-" + it.id }) { facility ->
                            Column {
                                FacilityCard(
                                    facility = facility,
                                    onClick = { onFacility(facility.id) },
                                    modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                                    // Today's own status, under today only; other days have their hours.
                                    showStatusNow = day.date == RosterCopy.today(),
                                )
                                val spans = RosterSpans.forFacility(day.shifts, facility.id, day.date)
                                if (spans.isNotEmpty()) {
                                    Text(
                                        text = RosterCopy.spans(spans),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(horizontal = Space.screen)
                                            .padding(bottom = Space.sm),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object RosterCopy {
    val TITLE: String @Composable get() = stringResource(Res.string.roster_title)
    val ERROR: String @Composable get() = stringResource(Res.string.roster_error)
    val EMPTY: String @Composable get() = stringResource(Res.string.roster_empty)
    val NO_DUTY_DAY: String @Composable get() = stringResource(Res.string.roster_no_duty_day)

    /** Today in Damascus, as the roster numbers its days. */
    fun today(): String = DamascusTime.now().date.toString()

    /** «منذ اليوم السابق حتى 08:00، ومن 22:00 إلى 08:00 من اليوم التالي», as the site says it. */
    @Composable
    fun spans(spans: List<RosterSpan>): String = spans.map { span ->
        val from = span.from
        when {
            from == null && span.nextDay -> stringResource(Res.string.roster_span_all_day)
            from == null -> stringResource(Res.string.roster_span_since, span.to)
            span.nextDay -> stringResource(Res.string.roster_span_overnight, from, span.to)
            else -> stringResource(Res.string.roster_span_same, from, span.to)
        }
    }.joinToString(stringResource(Res.string.roster_span_joiner))
    val PROVINCE: String @Composable get() = stringResource(Res.string.roster_province)
    val PROVINCE_ACTION: String @Composable get() = stringResource(Res.string.roster_province_action)

    @Composable
    fun range(range: RosterRange): String = stringResource(
        when (range) {
            RosterRange.TODAY -> Res.string.roster_today
            RosterRange.TOMORROW -> Res.string.roster_tomorrow
            RosterRange.WEEK -> Res.string.roster_week
        },
    )

    /** "الثلاثاء 29/09": the weekday in the reader's language, the date in digits. */
    @Composable
    fun day(date: String): String {
        val day = RosterDay.of(date) ?: return date
        return stringResource(weekday(day.weekday)) + " " + day.digits
    }

    private fun weekday(day: DayOfWeek): StringResource = when (day) {
        DayOfWeek.MONDAY -> Res.string.roster_monday
        DayOfWeek.TUESDAY -> Res.string.roster_tuesday
        DayOfWeek.WEDNESDAY -> Res.string.roster_wednesday
        DayOfWeek.THURSDAY -> Res.string.roster_thursday
        DayOfWeek.FRIDAY -> Res.string.roster_friday
        DayOfWeek.SATURDAY -> Res.string.roster_saturday
        DayOfWeek.SUNDAY -> Res.string.roster_sunday
    }
}

/** A roster day as its heading writes it: the weekday, and the date as "dd/MM" in digits. */
internal class RosterDay(val weekday: DayOfWeek, val digits: String) {
    companion object {
        /** The backend's ISO date; null for anything else, which the heading shows as it came. */
        fun of(date: String): RosterDay? {
            val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
            return RosterDay(day.dayOfWeek, two(day.day) + "/" + two(day.month.number))
        }

        private fun two(value: Int) = value.toString().padStart(2, '0')
    }
}
