package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.model.DirectoryBrand

/** Where the splash draws the mark: where the system splash drew it, so the hand-over does not jump. */
object SplashGeometry {
    /**
     * The mark's vertical centre in the splash's own coordinates.
     *
     * The system splash centres its icon in the whole window. The app's splash may start below
     * the status bar and end above the navigation bar, so the window's centre is not its own.
     * Before the window is measured it falls back to its own centre, and it never puts the mark
     * outside the splash.
     */
    fun markCentreY(windowHeight: Int, contentTop: Int, contentHeight: Int, markHeight: Int): Int {
        if (windowHeight <= 0) return contentHeight / 2
        val lowest = markHeight / 2
        val highest = (contentHeight - markHeight / 2).coerceAtLeast(lowest)
        return (windowHeight / 2 - contentTop).coerceIn(lowest, highest)
    }
}

/**
 * The splash's words, in one place. No product copy has been approved yet: the tagline is the
 * wording proposed for the redesign and changes here when copy is approved.
 *
 * A third line named the launch province. It is gone: the app opens in more than one province
 * and the province a reader is in is written on Home, where it can be changed.
 */
object SplashCopy {
    const val NAME = DirectoryBrand.NAME
    const val TAGLINE = DirectoryBrand.TAGLINE
}
