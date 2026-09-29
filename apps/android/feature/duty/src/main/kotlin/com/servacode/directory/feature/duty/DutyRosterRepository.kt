package com.servacode.directory.feature.duty

import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.DamascusTime
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.network.PublicApiBoundary
import kotlinx.coroutines.flow.first
import javax.inject.Inject

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
    /** Null when no province has been chosen yet: there is no roster to show. */
    suspend fun load(range: RosterRange, today: java.time.LocalDate = DamascusTime.now().toLocalDate()): Result<List<DutyDay>>? {
        val provinceId = preferences.values.first().selectedProvinceId ?: return null
        val from = today.plusDays(range.offsetDays).toString()
        return runCatching { api.dutyRoster(provinceId, from, range.days) }
    }
}
