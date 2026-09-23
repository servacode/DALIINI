package com.servacode.directory.core.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

/**
 * How one screen gives way to another, in one place.
 *
 * The platform's default for this app scaled a whole screen down into a small square and let it
 * vanish — read as a rendering fault rather than as navigation. What replaces it is the motion
 * the content itself implies: a screen that was pushed slides in from the end of the layout and
 * fades slightly; going back reverses exactly that. Because it is expressed as Start and End
 * rather than left and right, it mirrors itself for Arabic without a second set of values.
 *
 * Switching between the app's main places is not a push at all — nothing is stacked, so nothing
 * should slide. Those cross-fade.
 *
 * No screen writes its own transition. A screen that animated itself would drift from the rest
 * within a release, and the one thing a user notices about motion is inconsistency.
 */
object DirectoryMotion {
    /** Long enough to be read as movement, short enough never to be waited on. */
    const val PUSH_MILLIS = 220
    const val FADE_MILLIS = 180
    const val TAB_MILLIS = 140

    val push: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(PUSH_MILLIS),
        ) + fadeIn(tween(FADE_MILLIS))
    }

    val pushAway: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(PUSH_MILLIS),
        ) + fadeOut(tween(FADE_MILLIS))
    }

    val popBack: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(PUSH_MILLIS),
        ) + fadeIn(tween(FADE_MILLIS))
    }

    val popAway: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(PUSH_MILLIS),
        ) + fadeOut(tween(FADE_MILLIS))
    }

    /** The app's main places: a change of view, not a step deeper. */
    val tabIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(TAB_MILLIS))
    }

    val tabOut: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(TAB_MILLIS))
    }
}
