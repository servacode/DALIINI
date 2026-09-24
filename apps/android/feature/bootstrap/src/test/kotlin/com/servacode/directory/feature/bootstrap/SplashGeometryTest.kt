package com.servacode.directory.feature.bootstrap

import org.junit.Assert.assertEquals
import org.junit.Test

/** The splash's mark lands where the system splash drew it: the centre of the whole window. */
class SplashGeometryTest {
    private val mark = 315 // 120 dp at 2.625

    @Test fun `below the status bar, the mark sits lower in the splash by the bars' difference`() {
        // Galaxy A52: a 2400 px window, content from 88 px (status bar) to 2274 px (navigation bar).
        val centre = SplashGeometry.markCentreY(windowHeight = 2400, contentTop = 88, contentHeight = 2186,
            markHeight = mark)

        assertEquals(1112, centre)
        assertEquals("the window's centre", 1200, 88 + centre)
    }

    @Test fun `drawn edge to edge, the window's centre is the splash's own`() {
        assertEquals(1200, SplashGeometry.markCentreY(2400, contentTop = 0, contentHeight = 2400, markHeight = mark))
    }

    @Test fun `before the window is measured it uses its own centre`() {
        assertEquals(1093, SplashGeometry.markCentreY(0, contentTop = 0, contentHeight = 2186, markHeight = mark))
    }

    @Test fun `the mark never leaves the splash`() {
        assertEquals(mark / 2, SplashGeometry.markCentreY(400, contentTop = 900, contentHeight = 800,
            markHeight = mark))
        assertEquals(800 - mark / 2, SplashGeometry.markCentreY(4000, contentTop = 0, contentHeight = 800,
            markHeight = mark))
    }
}
