package com.servacode.directory.feature.duty

import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.model.KeptRoster
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** The three ways the roster is read: today, tomorrow, the coming week. */
enum class RosterRange(val offsetDays: Long, val days: Int) {
    TODAY(0, 1),
    TOMORROW(1, 1),
    WEEK(0, 7),
}

/**
 * Who is on duty in the reader's province, by day, from the backend's public roster
 * (`GET public/duty/`). The days are the backend's, on Damascus dates; the app only picks which.
 */
class DutyRosterRepository @Inject constructor(
    private val api: PublicApiBoundary,
    private val preferences: DirectoryPreferencesStore,
) {
    /**
     * Null when no province has been chosen yet: there is no roster to show.
     *
     * What is read is kept; when it cannot be read, what was kept for the same province and the
     * same first day is answered instead, marked [RosterLoad.kept] for the screen to say so.
     */
    suspend fun load(
        range: RosterRange,
        today: LocalDate = DamascusTime.now().date,
    ): Result<RosterLoad>? {
        val provinceId = preferences.values.first().selectedProvinceId ?: return null
        val from = today.plus(range.offsetDays, DateTimeUnit.DAY).toString()
        return runCatching { api.dutyRoster(provinceId, from, range.days) }.fold(
            onSuccess = { days ->
                runCatching { preferences.keepRoster(range.name, KeptRoster(provinceId, from, days).encode()) }
                Result.success(RosterLoad(days, kept = false))
            },
            onFailure = { failure ->
                val kept = KeptRoster.decode(runCatching { preferences.keptRoster(range.name) }.getOrNull())
                    ?.takeIf { it.provinceId == provinceId && it.from == from }
                if (kept != null) Result.success(RosterLoad(kept.days, kept = true)) else Result.failure(failure)
            },
        )
    }
}

/** The roster's days, and whether they are the ones kept from an earlier read. */
data class RosterLoad(val days: List<DutyDay>, val kept: Boolean)
