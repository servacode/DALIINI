package com.servacode.directory.ios

import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellScreensTest {
    private val pharmacy = Category("pharmacy", "صيدلية", iconKey = "pharmacy")
    private fun facility(id: String) = FacilitySummary(id = id, nameAr = "صيدلية $id", category = pharmacy)

    @Test
    fun `the home lists duty first and leaves out what is empty`() {
        val snapshot = HomeSnapshot(
            province = Province("damascus", "دمشق"),
            categories = listOf(pharmacy),
            nearby = listOf(facility("a"), facility("b")),
            refreshedAtEpochMillis = 0,
            dutyNow = listOf(facility("d")),
        )

        val sections = homeSections(snapshot)

        assertEquals(listOf(ShellWord.DUTY_NOW, ShellWord.NEARBY, ShellWord.CATEGORIES), sections.map { it.title })
        assertEquals(HomeRow("d", "صيدلية d", "صيدلية", DirectoryIcons.pharmacy), sections.first().rows.single())
        assertEquals(HomeRow("pharmacy", "صيدلية", null, DirectoryIcons.pharmacy), sections.last().rows.single())
    }
}
