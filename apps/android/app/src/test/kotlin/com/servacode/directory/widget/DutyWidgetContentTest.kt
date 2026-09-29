package com.servacode.directory.widget

import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilitySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DutyWidgetContentTest {
    private val now = 1_790_589_600_000L
    private val pharmacy = Category("c", "صيدليات")

    private fun onDuty(id: String, distance: Double?) =
        FacilitySummary(id = id, nameAr = "صيدلية $id", category = pharmacy, distanceMeters = distance)

    @Test fun `nearest first, the unknown distances after, the platform's order kept`() {
        val rows = DutyWidgetContent.from(
            "الرقة",
            listOf(onDuty("a", null), onDuty("b", 900.0), onDuty("c", 150.0), onDuty("d", null)),
            phones = mapOf("c" to "+963933000000", "b" to " "),
            nowEpochMillis = now,
        )

        assertEquals(listOf("c", "b", "a"), rows.facilities.map { it.id })
        assertEquals("+963933000000", rows.facilities[0].phone)
        // A blank number is no number: no call button.
        assertNull(rows.facilities[1].phone)
        assertEquals(DutyWidgetKind.LIST, rows.kind)
        assertEquals(now, rows.refreshedAtEpochMillis)
    }

    @Test fun `each state the widget can be in`() {
        assertEquals(DutyWidgetKind.NO_PROVINCE, DutyWidgetContent.NO_PROVINCE.kind)
        assertEquals(DutyWidgetKind.LOADING, DutyWidgetState(provinceChosen = true).kind)
        assertEquals(DutyWidgetKind.EMPTY, DutyWidgetContent.from("الرقة", emptyList(), emptyMap(), now).kind)
        assertEquals(DutyWidgetKind.OFFLINE, DutyWidgetContent.offline(null).kind)
    }

    @Test fun `offline keeps the last answer and says so`() {
        val last = DutyWidgetContent.from("الرقة", listOf(onDuty("a", 10.0)), emptyMap(), now)

        val offline = DutyWidgetContent.offline(last)

        assertTrue(offline.offline)
        assertEquals(last.facilities, offline.facilities)
        assertEquals(DutyWidgetKind.LIST, offline.kind)
        assertEquals(DutyWidgetContent.NO_PROVINCE, DutyWidgetContent.offline(last, provinceChosen = false))
    }

    @Test fun `the stored state survives a round trip, and a bad one is none`() {
        val state = DutyWidgetContent.from("الرقة", listOf(onDuty("a", 10.0)), mapOf("a" to "0933"), now)

        assertEquals(state, DutyWidgetState.decode(state.encode()))
        assertNull(DutyWidgetState.decode("{not json"))
        assertNull(DutyWidgetState.decode(null))
    }

    @Test fun `a small widget shows one row, a tall one three`() {
        assertEquals(1, DutyWidgetContent.rowsFor(80f))
        assertEquals(2, DutyWidgetContent.rowsFor(150f))
        assertEquals(3, DutyWidgetContent.rowsFor(400f))
    }
}
