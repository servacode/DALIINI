package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.RouteManeuver
import org.junit.Assert.assertEquals
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

    private fun maneuver() = RouteManeuver(
        kind = ManeuverKind.TURN,
        modifier = ManeuverModifier.RIGHT,
        point = MapPoint(35.95, 39.01),
        streetName = "شارع النور",
        distanceMeters = 120.0,
        durationSeconds = 20.0,
    )
}
