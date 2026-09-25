package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.RouteManeuver

/**
 * Which sentence a turn deserves, named without saying it.
 *
 * The words live in the module's `strings.xml` and are put together by [NavigationWords]. What is
 * left here is the decision, which is the same in every language: a right turn is a right turn,
 * and a roundabout's third exit is its third exit. Keeping the two apart is what lets this be
 * compiled and tested without Android — and what stops a test from asserting Arabic prose, which
 * breaks every time the wording is improved.
 */
internal enum class ManeuverPhraseKind {
    DEPART,
    ARRIVE,
    UTURN,
    MERGE,
    CONTINUE,
    UNKNOWN,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    STRAIGHT,
    FORK_LEFT,
    FORK_RIGHT,
    FORK,
    ROUNDABOUT,
    ROUNDABOUT_EXIT,
}

/**
 * A turn as it will be said: which sentence, which exit number if it has one, and the street it
 * leads to when that street has a name a voice can read.
 */
internal data class ManeuverPhrase(
    val kind: ManeuverPhraseKind,
    val roundaboutExit: Int? = null,
    val street: String? = null,
)

internal object ManeuverPhrases {
    /** What this maneuver is, in terms the words can be looked up by. */
    fun of(maneuver: RouteManeuver): ManeuverPhrase {
        val kind = when (maneuver.kind) {
            ManeuverKind.DEPART -> ManeuverPhraseKind.DEPART
            ManeuverKind.ARRIVE -> ManeuverPhraseKind.ARRIVE
            ManeuverKind.ROUNDABOUT -> roundaboutKind(maneuver.roundaboutExit)
            ManeuverKind.UTURN -> ManeuverPhraseKind.UTURN
            ManeuverKind.TURN -> turnKind(maneuver.modifier)
            ManeuverKind.MERGE -> ManeuverPhraseKind.MERGE
            ManeuverKind.FORK -> forkKind(maneuver.modifier)
            ManeuverKind.CONTINUE -> ManeuverPhraseKind.CONTINUE
            ManeuverKind.UNKNOWN -> ManeuverPhraseKind.UNKNOWN
        }
        // Arriving names the destination, which the reader already chose; a street name on top of
        // it says nothing.
        val street = if (kind == ManeuverPhraseKind.ARRIVE) null else spokenStreetName(maneuver.streetName)
        return ManeuverPhrase(
            kind = kind,
            roundaboutExit = maneuver.roundaboutExit.takeIf { kind == ManeuverPhraseKind.ROUNDABOUT_EXIT },
            street = street,
        )
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
     *
     * This much is about the language being spoken rather than the route, and a second spoken
     * language would need its own rule here. It stays in Kotlin because it is a judgement about
     * characters, not a sentence: a translator cannot express it in a string.
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

    private fun turnKind(modifier: ManeuverModifier): ManeuverPhraseKind = when (modifier) {
        ManeuverModifier.LEFT -> ManeuverPhraseKind.TURN_LEFT
        ManeuverModifier.RIGHT -> ManeuverPhraseKind.TURN_RIGHT
        ManeuverModifier.SLIGHT_LEFT -> ManeuverPhraseKind.SLIGHT_LEFT
        ManeuverModifier.SLIGHT_RIGHT -> ManeuverPhraseKind.SLIGHT_RIGHT
        ManeuverModifier.SHARP_LEFT -> ManeuverPhraseKind.SHARP_LEFT
        ManeuverModifier.SHARP_RIGHT -> ManeuverPhraseKind.SHARP_RIGHT
        ManeuverModifier.UTURN -> ManeuverPhraseKind.UTURN
        ManeuverModifier.STRAIGHT -> ManeuverPhraseKind.STRAIGHT
        ManeuverModifier.UNKNOWN -> ManeuverPhraseKind.UNKNOWN
    }

    private fun forkKind(modifier: ManeuverModifier): ManeuverPhraseKind = when (modifier) {
        ManeuverModifier.LEFT,
        ManeuverModifier.SLIGHT_LEFT,
        ManeuverModifier.SHARP_LEFT,
        -> ManeuverPhraseKind.FORK_LEFT
        ManeuverModifier.RIGHT,
        ManeuverModifier.SLIGHT_RIGHT,
        ManeuverModifier.SHARP_RIGHT,
        -> ManeuverPhraseKind.FORK_RIGHT
        else -> ManeuverPhraseKind.FORK
    }

    private fun roundaboutKind(exit: Int?): ManeuverPhraseKind =
        if (exit == null || exit <= 0) ManeuverPhraseKind.ROUNDABOUT else ManeuverPhraseKind.ROUNDABOUT_EXIT
}
