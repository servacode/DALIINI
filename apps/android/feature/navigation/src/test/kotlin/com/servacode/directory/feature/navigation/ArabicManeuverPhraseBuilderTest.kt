package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArabicManeuverPhraseBuilderTest {
    @Test
    fun `right turn phrase is deterministic arabic`() {
        assertEquals("انعطف يمينًا إلى شارع النور", ArabicManeuverPhraseBuilder.phrase(maneuver()))
    }

    @Test
    fun `roundabout phrase includes exit`() {
        val value = maneuver().copy(kind = ManeuverKind.ROUNDABOUT, roundaboutExit = 2)
        assertEquals("ادخل الدوار واخرج من المخرج رقم 2 إلى شارع النور", ArabicManeuverPhraseBuilder.phrase(value))
    }

    // Every name below came off one Damascus–Homs route from our own engine over Syrian OSM.

    @Test
    fun `a motorway number is not said, because an arabic voice cannot read it`() {
        listOf("M1", "M5", "M45").forEach {
            assertNull(it, ArabicManeuverPhraseBuilder.spokenStreetName(it))
            assertEquals("انعطف يمينًا", ArabicManeuverPhraseBuilder.phrase(maneuver().copy(streetName = it)))
        }
    }

    @Test
    fun `half a name is not a name`() {
        // "شارع Aleppo Road" read by an Arabic synthesiser is worse than "شارع" alone, so the
        // Latin part does not get cleaned out — the whole name goes.
        assertNull(ArabicManeuverPhraseBuilder.spokenStreetName("شارع Aleppo Road"))
    }

    @Test
    fun `an invisible mark does not cost a good name`() {
        // This one is real: it begins with U+202B, a right-to-left embedding. Judging before
        // stripping it would throw away a perfectly good name for a character nobody can see.
        assertEquals("طريق الشام", ArabicManeuverPhraseBuilder.spokenStreetName("‫طريق الشام"))
    }

    @Test
    fun `the arabic names on that route are all said`() {
        listOf(
            "طريق بيروت",
            "ساحة الامويين",
            "شارع شكري القوتلي",
            "شارع الثورة",
            "شارع السادس من تشرين",
            "طريق حلب دمشق الدولي",
            "شارع هاشم الأتاسي",
            "شارع أبو العلاء المعري",
        ).forEach { assertEquals(it, ArabicManeuverPhraseBuilder.spokenStreetName(it)) }
    }

    @Test
    fun `nothing, a fragment or a sentence is not said`() {
        assertNull(ArabicManeuverPhraseBuilder.spokenStreetName(null))
        assertNull(ArabicManeuverPhraseBuilder.spokenStreetName("   "))
        assertNull(ArabicManeuverPhraseBuilder.spokenStreetName("شا"))
        assertNull(ArabicManeuverPhraseBuilder.spokenStreetName("ش".repeat(40)))
    }

    private fun maneuver() = RouteManeuver(
        kind = ManeuverKind.TURN,
        modifier = ManeuverModifier.RIGHT,
        point = MapPoint(35.95, 39.01),
        streetName = "شارع النور",
        distanceMeters = 120.0,
        durationSeconds = 20.0,
    )
}
