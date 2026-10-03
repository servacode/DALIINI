package com.servacode.directory.feature.navigation

import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Keeps a trip going when the screen is off (DECISION-078).
 *
 * Android stops sending an app's location every second once nothing of it is on screen, and may
 * end the process altogether. Guidance that falls silent the moment the phone is locked or put
 * in a pocket is not guidance. While a trip is under way the app therefore runs a foreground
 * service with an ongoing notice in the shade that says where the trip stands, and the readings
 * and the voice carry on.
 *
 * This module only says when a trip starts, how it is going and when it ends; the app holds the
 * service, because the notice is drawn with the app's own icon and colours.
 */
interface NavigationKeepAlive {
    /** A trip is under way, or has moved on: keep running, and show [notice]. */
    fun show(notice: GuidanceNotice)

    /** The trip has ended, failed or been left: nothing to keep running for. */
    fun stop()
}

/**
 * What the ongoing notice says, rounded as it is read, so that a reading every second does not
 * redraw the shade every second: only a change someone could see makes a new notice.
 */
data class GuidanceNotice(
    /** What is left, rounded to fifty metres. */
    val remainingMeters: Long,
    /** Minutes left, rounded up, so the last half minute is still one. */
    val remainingMinutes: Int,
    /** True while a new way is being found after leaving the route. */
    val rerouting: Boolean,
)

/** The notice for a trip as the engine has it, or null when there is no trip to keep alive. */
internal fun guidanceNotice(state: NavigationState): GuidanceNotice? {
    val progress = when (state) {
        is NavigationState.Navigating -> state.progress
        is NavigationState.Rerouting -> state.progress
        else -> return null
    }
    return GuidanceNotice(
        remainingMeters = (progress.remainingDistanceMeters.coerceAtLeast(0.0) / NOTICE_STEP_METERS)
            .roundToLong() * NOTICE_STEP_METERS,
        remainingMinutes = ceil(progress.remainingDurationSeconds.coerceAtLeast(0.0) / 60.0).toInt(),
        rerouting = state is NavigationState.Rerouting,
    )
}

private const val NOTICE_STEP_METERS = 50L
