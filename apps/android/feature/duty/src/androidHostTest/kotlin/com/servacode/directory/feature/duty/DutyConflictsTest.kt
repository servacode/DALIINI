package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.TemporaryClosure
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.datetime.LocalDate

class DutyConflictsTest {
    private val hour = 3_600_000L
    private val now = 100 * hour
    private val shift = DutyShift("s-1", now + 10 * hour, now + 20 * hour)
    private val closure = TemporaryClosure("c-1", now + 30 * hour, now + 40 * hour)

    private fun check(start: Long, end: Long) = DutyConflicts.check(start, end, listOf(shift), listOf(closure), now)

    @Test fun `a free future window is fine`() {
        assertNull(check(now + 21 * hour, now + 29 * hour))
    }

    @Test fun `touching an existing shift is not overlapping it`() {
        assertNull(check(now + 20 * hour, now + 25 * hour))
        assertNull(check(now + 5 * hour, now + 10 * hour))
    }

    @Test fun `each clash is named`() {
        assertEquals(DutyProblem.InvalidRange, check(now + 5 * hour, now + 5 * hour))
        assertEquals(DutyProblem.InPast, check(now - 10 * hour, now - 1))
        assertEquals(DutyProblem.Overlaps(shift), check(now + 19 * hour, now + 22 * hour))
        assertEquals(DutyProblem.DuringClosure(closure), check(now + 35 * hour, now + 36 * hour))
    }

    @Test fun `a shift already under way still counts as current`() {
        assertNull(DutyConflicts.check(now - hour, now + hour, emptyList(), emptyList(), now))
    }
}

class DutyRosterRepositoryTest {
    private val api = ScriptedPublicApi()
    private val today = LocalDate(2026, 9, 28)

    @Test fun `today, tomorrow and the week ask for the right days`() = runTest {
        api.rosterAnswer = { _, date, _ -> listOf(DutyDay(date!!, emptyList(), emptyList())) }
        val repository = DutyRosterRepository(api, FakePreferences("raqqa"))

        repository.load(RosterRange.TODAY, today)
        repository.load(RosterRange.TOMORROW, today)
        repository.load(RosterRange.WEEK, today)

        assertEquals(
            listOf("roster:raqqa:2026-09-28:1", "roster:raqqa:2026-09-29:1", "roster:raqqa:2026-09-28:7"),
            api.calls,
        )
    }

    @Test fun `without a province there is no roster to ask for`() = runTest {
        assertNull(DutyRosterRepository(api, FakePreferences(null)).load(RosterRange.TODAY, today))
    }
}
