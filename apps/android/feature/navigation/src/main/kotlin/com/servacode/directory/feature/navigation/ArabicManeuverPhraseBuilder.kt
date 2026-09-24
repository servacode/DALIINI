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
        val street = spokenStreetName(maneuver.streetName)
        return if (street == null || maneuver.kind == ManeuverKind.ARRIVE) {
            base
        } else {
            "$base إلى $street"
        }
    }

    /**
     * The name as it can be said aloud, or nothing at all.
     *
     * Syrian OpenStreetMap carries names an Arabic voice cannot read. On one Damascus–Homs route
     * the engine returned twelve names, and three of them were the motorway numbers `M1`, `M5`
     * and `M45`: read out by an Arabic synthesiser those are noise, and "اتجه إلى M45" is worse
     * than "اتجه" alone. So a Latin letter or a digit drops the whole name rather than being
     * cleaned out of it — half a name is not a name.
     *
     * The invisible characters come first, though. One name on that same route was
     * `‫طريق الشام`, which begins with a right-to-left embedding mark. Judging before stripping
     * those would throw away a perfectly good Arabic name for carrying a character nobody can
     * see. RahalGo measured five of twenty-five names lost exactly that way.
     */
    internal fun spokenStreetName(raw: String?): String? {
        val name = raw
            ?.filterNot { it.category == CharCategory.FORMAT || it.category == CharCategory.CONTROL }
            ?.trim()
            ?: return null
        if (name.length < MIN_NAME_CHARS || name.length > MAX_NAME_CHARS) return null
        var arabic = 0
        for (character in name) {
            when {
                character.isArabic() -> arabic += 1
                character in 'a'..'z' || character in 'A'..'Z' -> return null
                character.isDigit() -> return null
                character == ' ' || character == '-' || character == '،' -> Unit
                else -> return null
            }
        }
        return if (arabic >= MIN_NAME_CHARS) name else null
    }

    /** Two characters do not make a name. */
    private const val MIN_NAME_CHARS = 3

    /**
     * Longer than this is not a name but a sentence, and a sentence read where a name belongs
     * runs past the junction. The longest measured on a real Syrian route was twenty-one.
     */
    private const val MAX_NAME_CHARS = 32

    private fun Char.isArabic(): Boolean = this in '؀'..'ۿ' ||
        this in 'ݐ'..'ݿ' ||
        this in 'ﭐ'..'ﻼ'

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
