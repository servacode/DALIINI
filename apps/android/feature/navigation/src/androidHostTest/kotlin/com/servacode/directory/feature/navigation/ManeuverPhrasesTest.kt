package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What a turn is, without what it says.
 *
 * The sentences themselves are in `res/values/strings.xml` and put together by `NavigationWords`,
 * so nothing here asserts Arabic prose — a better wording is then a change to one file and not a
 * test failure. What is asserted is the part that must never drift: which sentence a turn asks
 * for, and which street names a voice can be trusted with.
 */
class ManeuverPhrasesTest {
    @Test
    fun `a right turn asks for the right turn sentence, and carries its street`() {
        val phrase = ManeuverPhrases.of(maneuver())
        assertEquals(ManeuverPhraseKind.TURN_RIGHT, phrase.kind)
        assertEquals("شارع النور", phrase.street)
        assertNull(phrase.roundaboutExit)
    }

    @Test
    fun `a roundabout carries its exit, and one without an exit does not`() {
        val numbered = maneuver().copy(kind = ManeuverKind.ROUNDABOUT, roundaboutExit = 2)
        assertEquals(ManeuverPhraseKind.ROUNDABOUT_EXIT, ManeuverPhrases.of(numbered).kind)
        assertEquals(2, ManeuverPhrases.of(numbered).roundaboutExit)

        val unnumbered = maneuver().copy(kind = ManeuverKind.ROUNDABOUT, roundaboutExit = null)
        assertEquals(ManeuverPhraseKind.ROUNDABOUT, ManeuverPhrases.of(unnumbered).kind)
        assertNull(ManeuverPhrases.of(unnumbered).roundaboutExit)
    }

    @Test
    fun `arriving names no street, because the destination is the one the reader chose`() {
        val arrive = maneuver().copy(kind = ManeuverKind.ARRIVE)
        assertEquals(ManeuverPhraseKind.ARRIVE, ManeuverPhrases.of(arrive).kind)
        assertNull(ManeuverPhrases.of(arrive).street)
    }

    // Every name below came off one Damascus–Homs route from our own engine over Syrian OSM.

    @Test
    fun `a motorway number is not said, because an arabic voice cannot read it`() {
        listOf("M1", "M5", "M45").forEach {
            assertNull(it, ManeuverPhrases.spokenStreetName(it))
            assertNull(it, ManeuverPhrases.of(maneuver().copy(streetName = it)).street)
        }
    }

    @Test
    fun `half a name is not a name`() {
        // "شارع Aleppo Road" read by an Arabic synthesiser is worse than "شارع" alone, so the
        // Latin part does not get cleaned out — the whole name goes.
        assertNull(ManeuverPhrases.spokenStreetName("شارع Aleppo Road"))
    }

    @Test
    fun `an invisible mark does not cost a good name`() {
        // This one is real: it begins with U+202B, a right-to-left embedding. Judging before
        // stripping it would throw away a perfectly good name for a character nobody can see.
        assertEquals("طريق الشام", ManeuverPhrases.spokenStreetName("‫طريق الشام"))
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
        ).forEach { assertEquals(it, ManeuverPhrases.spokenStreetName(it)) }
    }

    @Test
    fun `nothing, a fragment or a sentence is not said`() {
        assertNull(ManeuverPhrases.spokenStreetName(null))
        assertNull(ManeuverPhrases.spokenStreetName("   "))
        assertNull(ManeuverPhrases.spokenStreetName("شا"))
        assertNull(ManeuverPhrases.spokenStreetName("ش".repeat(40)))
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
