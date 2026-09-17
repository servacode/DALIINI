package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.RouteManeuver

internal object ArabicManeuverPhraseBuilder {
    fun phrase(maneuver: RouteManeuver): String {
        val base = when (maneuver.kind) {
            ManeuverKind.DEPART -> "ابدأ المسار"
            ManeuverKind.ARRIVE -> "لقد وصلت إلى وجهتك"
            ManeuverKind.ROUNDABOUT -> roundaboutPhrase(maneuver.roundaboutExit)
            ManeuverKind.UTURN -> "قم بالاستدارة والعودة"
            ManeuverKind.TURN -> turnPhrase(maneuver.modifier)
            ManeuverKind.MERGE -> "اندمج مع الطريق"
            ManeuverKind.FORK -> forkPhrase(maneuver.modifier)
            ManeuverKind.CONTINUE -> "تابع السير"
            ManeuverKind.UNKNOWN -> "تابع المسار"
        }
        val street = maneuver.streetName?.trim().orEmpty()
        return if (street.isBlank() || maneuver.kind == ManeuverKind.ARRIVE) {
            base
        } else {
            "$base إلى $street"
        }
    }

    private fun turnPhrase(modifier: ManeuverModifier): String = when (modifier) {
        ManeuverModifier.LEFT -> "انعطف يسارًا"
        ManeuverModifier.RIGHT -> "انعطف يمينًا"
        ManeuverModifier.SLIGHT_LEFT -> "اتجه قليلًا إلى اليسار"
        ManeuverModifier.SLIGHT_RIGHT -> "اتجه قليلًا إلى اليمين"
        ManeuverModifier.SHARP_LEFT -> "انعطف بحدة إلى اليسار"
        ManeuverModifier.SHARP_RIGHT -> "انعطف بحدة إلى اليمين"
        ManeuverModifier.UTURN -> "قم بالاستدارة والعودة"
        ManeuverModifier.STRAIGHT -> "تابع مباشرة"
        ManeuverModifier.UNKNOWN -> "تابع المسار"
    }

    private fun forkPhrase(modifier: ManeuverModifier): String = when (modifier) {
        ManeuverModifier.LEFT,
        ManeuverModifier.SLIGHT_LEFT,
        ManeuverModifier.SHARP_LEFT,
        -> "خذ التفرع الأيسر"
        ManeuverModifier.RIGHT,
        ManeuverModifier.SLIGHT_RIGHT,
        ManeuverModifier.SHARP_RIGHT,
        -> "خذ التفرع الأيمن"
        else -> "تابع عبر التفرع"
    }

    private fun roundaboutPhrase(exit: Int?): String = if (exit == null || exit <= 0) {
        "ادخل الدوار واتبع المسار"
    } else {
        "ادخل الدوار واخرج من المخرج رقم $exit"
    }
}
