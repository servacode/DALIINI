package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The duty roster without a connection: what was read before is shown, marked as kept, and only
 * for the province and the day it was read for.
 */
class KeptRosterTest {
    private val api = ScriptedPublicApi()
    private val today = LocalDate.parse("2026-10-10")
    private val offline: (String, String?, Int) -> List<DutyDay> = { _, _, _ -> throw java.io.IOException("offline") }

    @Test fun `what was read is shown again when it cannot be read`() = runTest {
        val preferences = FakePreferences("raqqa")
        val repository = DutyRosterRepository(api, preferences)
        api.rosterAnswer = { _, date, _ -> listOf(DutyDay(date!!, emptyList(), emptyList())) }
        val fresh = repository.load(RosterRange.TODAY, today)!!.getOrThrow()

        api.rosterAnswer = offline
        val kept = repository.load(RosterRange.TODAY, today)!!.getOrThrow()

        assertFalse(fresh.kept)
        assertTrue(kept.kept)
        assertEquals(fresh.days, kept.days)
    }

    @Test fun `a roster kept for another day is not shown`() = runTest {
        val preferences = FakePreferences("raqqa")
        val repository = DutyRosterRepository(api, preferences)
        api.rosterAnswer = { _, date, _ -> listOf(DutyDay(date!!, emptyList(), emptyList())) }
        repository.load(RosterRange.TODAY, today)

        api.rosterAnswer = offline
        val tomorrow = repository.load(RosterRange.TODAY, LocalDate.parse("2026-10-11"))!!

        assertTrue(tomorrow.isFailure)
    }

    @Test fun `nothing kept is a failure as before`() = runTest {
        api.rosterAnswer = offline

        val result = DutyRosterRepository(api, FakePreferences("raqqa")).load(RosterRange.WEEK, today)!!

        assertTrue(result.isFailure)
    }
}
