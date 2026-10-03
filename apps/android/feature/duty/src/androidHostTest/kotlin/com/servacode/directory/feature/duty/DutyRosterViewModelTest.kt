package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.MainDispatcherRule
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DutyRosterViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val api = ScriptedPublicApi()

    @Test fun `today first, then the week when it is picked`() = runTest(main.dispatcher) {
        api.rosterAnswer = { _, date, days -> List(days) { DutyDay("$date+$it", emptyList(), emptyList()) } }
        val model = DutyRosterViewModel(DutyRosterRepository(api, FakePreferences("raqqa")))
        advanceUntilIdle()
        assertEquals(1, (model.state.value as DutyRosterUiState.Content).days.size)

        model.select(RosterRange.WEEK)
        advanceUntilIdle()

        assertEquals(RosterRange.WEEK, model.range.value)
        val week = model.state.value as DutyRosterUiState.Content
        assertEquals(7, week.days.size)
        // Nobody on duty on any of the seven days is said as such, not as seven empty headings.
        assertTrue(week.isEmpty)
    }

    @Test fun `no province asks for one, a failure offers to retry`() = runTest(main.dispatcher) {
        val none = DutyRosterViewModel(DutyRosterRepository(api, FakePreferences(null)))
        advanceUntilIdle()
        assertEquals(DutyRosterUiState.ProvinceRequired, none.state.value)

        val offline = DutyRosterViewModel(DutyRosterRepository(api, FakePreferences("raqqa")))
        advanceUntilIdle()
        assertEquals(AppError.Kind.OFFLINE, (offline.state.value as DutyRosterUiState.Error).error.kind)
    }
}
