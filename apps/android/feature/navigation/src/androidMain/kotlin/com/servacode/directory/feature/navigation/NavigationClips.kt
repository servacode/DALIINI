package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.ManeuverModifier
import com.servacode.directory.core.maps.RouteManeuver

/**
 * Which recording says a thing, when one does.
 *
 * The app speaks with a recorded Arabic voice rather than the device's synthesiser, because the
 * synthesiser reads navigation Arabic badly — it mispronounces the imperative, it stumbles on
 * "يمينًا", and on many phones there is no Arabic voice installed at all, so guidance is silent
 * without anyone being told. The recordings are the same pack RahalGo's drivers have used on the
 * road; the file they came from and why each one exists is recorded in
 * `feature/navigation/voice-pack-index.json`.
 *
 * **The recordings carry no street names**, and cannot: there is one file per phrase, not one
 * per street in Syria. So the name stays on the screen, where it is read rather than heard,
 * and the voice says the turn and the distance. Speaking the turn with the good voice and the
 * street name with the synthesiser would be worse than either alone.
 *
 * Nothing here plays anything or knows what a `MediaPlayer` is. It answers one question: which
 * clip, if any, says this cue.
 */
internal object NavigationClips {
    /**
     * The distances the pack can say, which are not every fifty metres.
     *
     * A cue is snapped to the nearest of these, so "بعد 483 متر" is spoken as the recording for
     * five hundred rather than not spoken at all. Nothing above seven hundred exists here because
     * the planner's earliest warning is capped below it.
     */
    private val DISTANCES = intArrayOf(50, 100, 150, 200, 250, 300, 400, 500, 700)

    /** The two that are said once and plainly, whatever the distance. */
    private val PLAIN = setOf("depart", "arrived")

    /** The furthest exit the pack names; past it the phrase becomes "follow the roundabout". */
    private const val NAMED_EXITS = 12

    fun clipFor(cue: VoiceCue): String? = when (cue.kind) {
        VoiceCueKind.MANEUVER -> cue.maneuver?.let { maneuverClip(it, cue.stage, cue.distanceMeters) }
        VoiceCueKind.REROUTE -> "nav_recalculating_route"
        VoiceCueKind.REROUTE_FAILED -> "nav_reroute_failed"
        VoiceCueKind.ARRIVE -> "nav_arrived"
        VoiceCueKind.STARTED -> "nav_navigation_started"
    }

    private fun maneuverClip(
        maneuver: RouteManeuver,
        stage: VoiceStage,
        distanceMeters: Double?,
    ): String? {
        val base = baseFor(maneuver) ?: return null
        // Setting off and arriving are not approached from a distance. "After three hundred
        // metres, start the route" is not a sentence anyone says, and the pack has no recording
        // for it — rightly.
        if (base in PLAIN) return "nav_$base"
        return when (stage) {
            VoiceStage.NOW -> "nav_${base}_now"
            VoiceStage.EVENT -> "nav_$base"
            else -> {
                val metres = distanceMeters ?: return "nav_$base"
                "nav_${base}_in_${nearest(metres)}m"
            }
        }
    }

    /** The nearest distance the pack has a recording for. */
    internal fun nearest(meters: Double): Int =
        DISTANCES.minByOrNull { kotlin.math.abs(it - meters) } ?: DISTANCES.last()

    private fun baseFor(maneuver: RouteManeuver): String? = when (maneuver.kind) {
        ManeuverKind.DEPART -> "depart"
        ManeuverKind.ARRIVE -> "arrived"
        ManeuverKind.CONTINUE -> "continue_straight"
        ManeuverKind.UTURN -> "uturn"
        ManeuverKind.TURN -> when (maneuver.modifier) {
            ManeuverModifier.LEFT -> "turn_left"
            ManeuverModifier.RIGHT -> "turn_right"
            ManeuverModifier.SLIGHT_LEFT -> "slight_left"
            ManeuverModifier.SLIGHT_RIGHT -> "slight_right"
            ManeuverModifier.SHARP_LEFT -> "sharp_left"
            ManeuverModifier.SHARP_RIGHT -> "sharp_right"
            ManeuverModifier.UTURN -> "uturn"
            else -> "continue_straight"
        }
        ManeuverKind.MERGE -> when (maneuver.modifier) {
            ManeuverModifier.LEFT, ManeuverModifier.SLIGHT_LEFT, ManeuverModifier.SHARP_LEFT -> "merge_left"
            ManeuverModifier.RIGHT, ManeuverModifier.SLIGHT_RIGHT, ManeuverModifier.SHARP_RIGHT -> "merge_right"
            else -> "merge"
        }
        ManeuverKind.FORK -> when (maneuver.modifier) {
            ManeuverModifier.LEFT, ManeuverModifier.SLIGHT_LEFT, ManeuverModifier.SHARP_LEFT -> "fork_left"
            ManeuverModifier.RIGHT, ManeuverModifier.SLIGHT_RIGHT, ManeuverModifier.SHARP_RIGHT -> "fork_right"
            else -> "fork"
        }
        ManeuverKind.ROUNDABOUT -> {
            val exit = maneuver.roundaboutExit
            if (exit != null && exit in 1..NAMED_EXITS) "roundabout_exit_$exit" else "roundabout_continue"
        }
        // A turn nobody can name is not worth a sentence; the screen still shows the line.
        ManeuverKind.UNKNOWN -> null
    }
}
