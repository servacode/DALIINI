package com.servacode.directory.feature.navigation

import com.servacode.directory.core.maps.ManeuverKind
import com.servacode.directory.core.maps.RouteManeuver
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * When a thing is said, and what — never how it is said.
 *
 * Nothing here knows about `TextToSpeech`, an audio focus or a context: it is fed navigation
 * states and returns the sentences that are now due, so a whole trip can be replayed through it
 * and its guidance read as a list with no speaker in the room.
 *
 * The app used to speak once per maneuver, the moment the engine advanced onto it — which is
 * within 35 metres of the turn. That is a sentence that finishes after the junction. Guidance is
 * three sentences, not one: one far enough ahead to change lane, one to slow, and one at the
 * turn. This is what decides which of the three is due.
 *
 * Taken from what RahalGo's driver navigation learned over real roads, in this app's own types
 * and vocabulary; see `docs/design/RAHALGO-NAVIGATION-AUDIT.md`.
 */
enum class VoiceStage {
    /** Far enough ahead to change lane. */
    PREPARE,

    /** Close enough to slow down. */
    APPROACH,

    /** At the turn. */
    NOW,

    /** Something happened; distance has nothing to do with it. */
    EVENT,
}

/** What a cue is about, which is what decides which recording says it. */
enum class VoiceCueKind { MANEUVER, REROUTE, REROUTE_FAILED, ARRIVE, STARTED }

data class VoiceCue(
    val stage: VoiceStage,
    val text: String,
    /** What makes this cue this cue, so the same one is never said twice. */
    val key: String,
    val kind: VoiceCueKind = VoiceCueKind.MANEUVER,
    /** The turn being announced, for the recording that names it. Null for an event. */
    val maneuver: RouteManeuver? = null,
    /** How far it is, as the cue was fired. Null when the stage carries no distance. */
    val distanceMeters: Double? = null,
)

/**
 * The numbers guidance is timed by.
 *
 * **Seconds, not metres.** Two hundred metres is forty-eight seconds at 15 km/h — a warning so
 * early it is forgotten before the turn — and ten seconds at 70 km/h, which is too late. One
 * number, two opposite results. So each stage is a number of seconds, converted to a distance by
 * the speed actually being travelled, and clamped between two distances that keep it sensible:
 * someone stopped is not warned two kilometres out, and someone quick is not warned after the
 * junction.
 */
internal data class VoiceTuning(
    val prepareSeconds: Double = 45.0,
    val approachSeconds: Double = 15.0,
    val nowSeconds: Double = 5.0,
    val prepareMinMeters: Double = 150.0,
    val prepareMaxMeters: Double = 800.0,
    val approachMinMeters: Double = 60.0,
    val approachMaxMeters: Double = 300.0,
    val nowMinMeters: Double = 15.0,
    val nowMaxMeters: Double = 120.0,
    /**
     * What is assumed when the reading gives no usable speed.
     *
     * Eight metres a second is about 29 km/h: a city street. It neither warns far too early nor
     * far too late, which is what an assumption should do.
     */
    val assumedSpeedMps: Double = 8.0,
    val maxAssumedSpeedMps: Double = 25.0,
    /** Below this, a device's own speed is noise rather than movement. */
    val minTrustedSpeedMps: Double = 2.0,
    /** Past this from the line, an instruction from the route is an instruction about a road
     *  the person is no longer on. */
    val offRouteMeters: Double = 55.0,
)

internal class NavigationVoicePlanner(private val tuning: VoiceTuning = VoiceTuning()) {
    /** The maneuver the last reading was measuring towards, and how far it then was. */
    private var lastManeuverKey: String? = null
    private var lastDistanceMeters: Double = Double.NaN

    /** Said once per episode, not once per reading. */
    private var rerouteAnnounced = false
    private var arrivalAnnounced = false

    fun reset() {
        lastManeuverKey = null
        lastDistanceMeters = Double.NaN
        rerouteAnnounced = false
        arrivalAnnounced = false
    }

    /**
     * What is due now, given where the trip stands and how fast the person is moving.
     *
     * A list rather than one cue: arriving and finishing a recomputation can fall in the same
     * reading. [speedMetersPerSecond] is null when the reading carries no speed.
     */
    fun onState(state: NavigationState, speedMetersPerSecond: Float? = null): List<VoiceCue> {
        val cues = mutableListOf<VoiceCue>()
        when (state) {
            is NavigationState.Rerouting -> {
                if (!rerouteAnnounced) {
                    rerouteAnnounced = true
                    cues += VoiceCue(
                        stage = VoiceStage.EVENT,
                        text = NavigationVoiceCopy.REROUTING,
                        key = "reroute",
                        kind = VoiceCueKind.REROUTE,
                    )
                }
                // Nothing about turns while the way itself is in question.
                forgetManeuver()
            }

            is NavigationState.Arrived -> {
                if (!arrivalAnnounced) {
                    arrivalAnnounced = true
                    cues += VoiceCue(
                        stage = VoiceStage.EVENT,
                        text = NavigationVoiceCopy.ARRIVED,
                        key = "arrived",
                        kind = VoiceCueKind.ARRIVE,
                    )
                }
            }

            is NavigationState.Navigating -> {
                rerouteAnnounced = false
                cues += maneuverCue(state.progress, speedMetersPerSecond)
            }

            NavigationState.Idle, NavigationState.Routing, is NavigationState.Error -> Unit
        }
        return cues
    }

    private fun maneuverCue(
        progress: NavigationProgress,
        speedMetersPerSecond: Float?,
    ): List<VoiceCue> {
        // An instruction taken from a route the person has left is worse than silence.
        if (progress.offRouteDistanceMeters > tuning.offRouteMeters) {
            forgetManeuver()
            return emptyList()
        }
        val maneuver = progress.maneuver ?: return emptyList()
        // Arriving belongs to the engine's own arrival, which knows the destination rather than
        // the end of a drawn line. Saying it twice, once per owner, is worse than saying it once.
        if (maneuver.kind == ManeuverKind.ARRIVE) return emptyList()

        val key = maneuver.key()
        if (key != lastManeuverKey) {
            lastManeuverKey = key
            lastDistanceMeters = Double.NaN
        }
        val previous = lastDistanceMeters
        val distance = progress.distanceToManeuverMeters
        lastDistanceMeters = distance

        val speed = trustedSpeed(speedMetersPerSecond)
        val stage = stageFor(previous, distance, speed) ?: return emptyList()
        return listOf(
            VoiceCue(
                stage = stage,
                text = phrase(stage, maneuver, distance),
                key = "$key:$stage",
                kind = VoiceCueKind.MANEUVER,
                maneuver = maneuver,
                distanceMeters = if (stage == VoiceStage.NOW) null else distance,
            ),
        )
    }

    /**
     * The stage whose threshold was crossed between the last reading and this one.
     *
     * **Crossed, not reached.** Two readings a second apart at 70 km/h are nineteen metres apart,
     * so a threshold is stepped over and never landed on; waiting for the distance to equal it
     * means the sentence is never said. The nearest stage wins, so a trip that starts already
     * inside the approach radius is told to approach rather than to prepare.
     */
    private fun stageFor(previousMeters: Double, nowMeters: Double, speedMps: Double): VoiceStage? =
        when {
            crosses(previousMeters, nowMeters, triggerMeters(VoiceStage.NOW, speedMps)) -> VoiceStage.NOW
            crosses(previousMeters, nowMeters, triggerMeters(VoiceStage.APPROACH, speedMps)) -> VoiceStage.APPROACH
            crosses(previousMeters, nowMeters, triggerMeters(VoiceStage.PREPARE, speedMps)) -> VoiceStage.PREPARE
            else -> null
        }

    private fun crosses(previousMeters: Double, nowMeters: Double, triggerMeters: Double): Boolean {
        if (nowMeters > triggerMeters) return false
        // The first reading against a maneuver has nothing behind it, so being inside the
        // threshold is itself the crossing.
        if (previousMeters.isNaN()) return true
        return previousMeters > triggerMeters
    }

    internal fun triggerMeters(stage: VoiceStage, speedMps: Double): Double {
        val (seconds, minimum, maximum) = when (stage) {
            VoiceStage.NOW -> Triple(tuning.nowSeconds, tuning.nowMinMeters, tuning.nowMaxMeters)
            VoiceStage.APPROACH ->
                Triple(tuning.approachSeconds, tuning.approachMinMeters, tuning.approachMaxMeters)
            else -> Triple(tuning.prepareSeconds, tuning.prepareMinMeters, tuning.prepareMaxMeters)
        }
        return (seconds * speedMps).coerceIn(minimum, maximum)
    }

    private fun trustedSpeed(speedMetersPerSecond: Float?): Double {
        val reported = speedMetersPerSecond?.toDouble() ?: return tuning.assumedSpeedMps
        if (!reported.isFinite() || reported < tuning.minTrustedSpeedMps) return tuning.assumedSpeedMps
        return min(reported, tuning.maxAssumedSpeedMps)
    }

    private fun forgetManeuver() {
        lastManeuverKey = null
        lastDistanceMeters = Double.NaN
    }

    private fun phrase(stage: VoiceStage, maneuver: RouteManeuver, distanceMeters: Double): String {
        val instruction = ArabicManeuverPhraseBuilder.phrase(maneuver)
        // Setting off is not approached from a distance either, so it keeps its own words.
        if (maneuver.kind == ManeuverKind.DEPART) return instruction
        return when (stage) {
            VoiceStage.NOW -> "$instruction ${NavigationVoiceCopy.NOW}"
            else -> "${NavigationVoiceCopy.after(distanceMeters)} $instruction"
        }
    }

    private fun RouteManeuver.key(): String =
        "$kind:$modifier:${point.latitude}:${point.longitude}"
}

/** The words guidance uses, in one place, provisional until product copy is approved. */
internal object NavigationVoiceCopy {
    const val REROUTING = "يُعاد حساب الطريق"
    const val ARRIVED = "لقد وصلت إلى وجهتك"
    const val NOW = "الآن"

    /**
     * A distance as it is spoken, not as it is measured.
     *
     * "After 483 metres" is a number nobody drives by. Rounded to fifty below a kilometre and to
     * a half above it, which is how the distance is said out loud anyway.
     */
    fun after(meters: Double): String {
        if (meters >= 1000) {
            val kilometres = (meters / 500.0).roundToInt() / 2.0
            val said = if (kilometres % 1.0 == 0.0) kilometres.toInt().toString() else kilometres.toString()
            return "بعد $said كم"
        }
        val rounded = ((meters / 50.0).roundToInt() * 50).coerceAtLeast(50)
        return "بعد $rounded متر"
    }
}
