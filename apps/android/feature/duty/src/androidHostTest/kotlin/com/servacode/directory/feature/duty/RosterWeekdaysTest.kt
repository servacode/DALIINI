package com.servacode.directory.feature.duty

import java.io.File
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

/**
 * The roster's day names were Java's Arabic ones until the screen moved to shared code, where
 * there is no Java (DECISION-095). The words now written in the resources are those same names.
 */
class RosterWeekdaysTest {
    private val words: Map<String, String> by lazy {
        val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/commonMain/composeResources/values/strings.xml"))
            .getElementsByTagName("string")
        (0 until strings.length).map { strings.item(it) as Element }
            .associate { it.getAttribute("name") to it.textContent }
    }

    @Test fun `each weekday reads as Java's Arabic name for it did`() {
        DayOfWeek.entries.forEach { day ->
            assertEquals(
                day.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ar")),
                words.getValue("roster_" + day.name.lowercase()),
            )
        }
    }
}
