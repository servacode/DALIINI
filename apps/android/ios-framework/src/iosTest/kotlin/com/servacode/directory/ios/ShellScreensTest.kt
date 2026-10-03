package com.servacode.directory.ios

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.Category
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.designsystem.generated.DirectoryTokens
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellScreensTest {
    private val pharmacy = Category("pharmacy", "صيدلية")
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
        assertEquals(HomeRow("d", "صيدلية d", "صيدلية"), sections.first().rows.single())
        assertEquals(HomeRow("pharmacy", "صيدلية", null), sections.last().rows.single())
    }

    @Test
    fun `a failure gets the word Android's design system gives it`() {
        assertEquals(ShellWord.ERROR_OFFLINE, failureWord(AppError(AppError.Kind.OFFLINE)))
        assertEquals(ShellWord.ERROR_MAINTENANCE, failureWord(AppError(AppError.Kind.SERVER, code = "MAINTENANCE")))
        assertEquals(ShellWord.ERROR_SERVER, failureWord(AppError(AppError.Kind.SERVER)))
        assertEquals(ShellWord.ERROR_UNEXPECTED, failureWord(AppError(AppError.Kind.UNEXPECTED)))
    }

    @Test
    fun `the brand's colours are read opaque from the tokens' hex`() {
        val hex = DirectoryTokens.ColorsPrimary
        val color = hex.toColor()

        assertEquals(hex.substring(1, 3).toInt(16) / 255f, color.red, 0.002f)
        assertEquals(hex.substring(3, 5).toInt(16) / 255f, color.green, 0.002f)
        assertEquals(hex.substring(5, 7).toInt(16) / 255f, color.blue, 0.002f)
        assertEquals(1f, color.alpha)
    }
}
