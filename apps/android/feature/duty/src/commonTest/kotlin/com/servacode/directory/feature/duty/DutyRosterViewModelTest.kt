package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.DutyDay
import com.servacode.directory.core.testing.FakePreferences
import com.servacode.directory.core.testing.runMainTest
import com.servacode.directory.core.testing.ScriptedPublicApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class DutyRosterViewModelTest {
    private val api = ScriptedPublicApi()

    @Test fun `today first and then the week when it is picked`() = runMainTest {
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

    @Test fun `no province asks for one and a failure offers to retry`() = runMainTest {
        val none = DutyRosterViewModel(DutyRosterRepository(api, FakePreferences(null)))
        advanceUntilIdle()
        assertEquals(DutyRosterUiState.ProvinceRequired, none.state.value)

        val offline = DutyRosterViewModel(DutyRosterRepository(api, FakePreferences("raqqa")))
        advanceUntilIdle()
        assertEquals(AppError.Kind.OFFLINE, (offline.state.value as DutyRosterUiState.Error).error.kind)
    }
}
