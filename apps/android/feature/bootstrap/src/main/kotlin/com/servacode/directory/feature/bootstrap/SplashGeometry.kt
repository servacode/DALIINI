package com.servacode.directory.feature.bootstrap

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
 * The splash's words, in one place. No product copy has been approved yet: the tagline and the
 * footer are the wording proposed for the redesign and change here when copy is approved. The
 * footer names the launch province as secondary text, and goes when more provinces open.
 */
object SplashCopy {
    const val NAME = "الدليل"
    const val TAGLINE = "أقرب الخدمات الصحية إليك"
    const val FOOTER = "محافظة الرقة"
}
