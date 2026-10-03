package com.servacode.directory.feature.navigation

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.servacode.directory.core.maps.RouteManeuver

/**
 * Guidance in words, read from the module's own resources.
 *
 * This is the only place in navigation that knows what a turn sounds like. It takes a
 * [ManeuverPhrase] — a decision with no words in it — and returns the sentence for the reader's
 * language, which is why a second language is a second `strings.xml` and not a second copy of
 * the routing code.
 *
 * It is built on `Resources` rather than on composition because the same sentences are spoken
 * aloud, and the speaker is not a screen: [AndroidNavigationVoice] holds one of these over the
 * application's resources, while the screen reads the same sentences through
 * [maneuverSentence].
 */
internal class NavigationWords(private val resources: Resources) {
    /** A turn, and the street it leads to when the street has a name worth saying. */
    fun sentence(phrase: ManeuverPhrase): String {
        val base = when (phrase.kind) {
            ManeuverPhraseKind.ROUNDABOUT_EXIT -> resources.getString(
                R.string.nav_maneuver_roundabout_exit,
                phrase.roundaboutExit ?: 1,
            )
            else -> resources.getString(phrase.kind.resource())
        }
        val street = phrase.street ?: return base
        return resources.getString(R.string.nav_maneuver_to, base, street)
    }

    /**
     * The same sentence as the voice says it: at the turn, or from a distance.
     *
     * Setting off is not approached from a distance — "after three hundred metres, start the
     * route" is not a sentence anyone says — so it keeps its own words.
     */
    fun spoken(cue: VoiceCue): String = when (cue.kind) {
        VoiceCueKind.REROUTE -> resources.getString(R.string.nav_voice_rerouting)
        VoiceCueKind.REROUTE_FAILED -> resources.getString(R.string.nav_warning_reroute_failed)
        VoiceCueKind.ARRIVE -> resources.getString(R.string.nav_voice_arrived)
        VoiceCueKind.STARTED -> resources.getString(R.string.nav_maneuver_depart)
        VoiceCueKind.MANEUVER -> maneuverCue(cue)
    }

    private fun maneuverCue(cue: VoiceCue): String {
        val maneuver = cue.maneuver ?: return resources.getString(R.string.nav_maneuver_unknown)
        val phrase = ManeuverPhrases.of(maneuver)
        val instruction = sentence(phrase)
        if (phrase.kind == ManeuverPhraseKind.DEPART) return instruction
        if (cue.stage == VoiceStage.NOW) {
            return resources.getString(R.string.nav_voice_now, instruction)
        }
        val distance = cue.distanceMeters ?: return instruction
        return when (val said = spokenDistance(distance)) {
            is SpokenDistance.Metres -> resources.getString(
                R.string.nav_voice_after_metres,
                said.value,
                instruction,
            )
            is SpokenDistance.Kilometres -> resources.getString(
                R.string.nav_voice_after_kilometres,
                kilometres(said.halves),
                instruction,
            )
        }
    }

    /** One and a half, rather than one point five zero: a whole number stays whole. */
    private fun kilometres(halves: Int): String {
        val whole = halves / 2
        return if (halves % 2 == 0) whole.toString() else "$whole.5"
    }
}

private fun ManeuverPhraseKind.resource(): Int = when (this) {
    ManeuverPhraseKind.DEPART -> R.string.nav_maneuver_depart
    ManeuverPhraseKind.ARRIVE -> R.string.nav_maneuver_arrive
    ManeuverPhraseKind.UTURN -> R.string.nav_maneuver_uturn
    ManeuverPhraseKind.MERGE -> R.string.nav_maneuver_merge
    ManeuverPhraseKind.CONTINUE -> R.string.nav_maneuver_continue
    ManeuverPhraseKind.UNKNOWN -> R.string.nav_maneuver_unknown
    ManeuverPhraseKind.TURN_LEFT -> R.string.nav_maneuver_turn_left
    ManeuverPhraseKind.TURN_RIGHT -> R.string.nav_maneuver_turn_right
    ManeuverPhraseKind.SLIGHT_LEFT -> R.string.nav_maneuver_slight_left
    ManeuverPhraseKind.SLIGHT_RIGHT -> R.string.nav_maneuver_slight_right
    ManeuverPhraseKind.SHARP_LEFT -> R.string.nav_maneuver_sharp_left
    ManeuverPhraseKind.SHARP_RIGHT -> R.string.nav_maneuver_sharp_right
    ManeuverPhraseKind.STRAIGHT -> R.string.nav_maneuver_straight
    ManeuverPhraseKind.FORK_LEFT -> R.string.nav_maneuver_fork_left
    ManeuverPhraseKind.FORK_RIGHT -> R.string.nav_maneuver_fork_right
    ManeuverPhraseKind.FORK -> R.string.nav_maneuver_fork
    ManeuverPhraseKind.ROUNDABOUT -> R.string.nav_maneuver_roundabout
    // Its own sentence carries the exit number, so it is never looked up without one.
    ManeuverPhraseKind.ROUNDABOUT_EXIT -> R.string.nav_maneuver_roundabout
}

/** The turn on the screen, in the reader's language. */
@Composable
@ReadOnlyComposable
internal fun maneuverSentence(maneuver: RouteManeuver): String =
    NavigationWords(LocalContext.current.resources).sentence(ManeuverPhrases.of(maneuver))
