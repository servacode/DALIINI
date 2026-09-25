package com.servacode.directory.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A closure shows its period, and a reason only where there is one.
 *
 * The line the two are joined into is a sentence and lives in the design system's resources; what
 * is asserted here is that nothing is joined to a reason made of spaces.
 */
class ClosureTextTest {
    // 2026-09-20 08:00 to 16:00 on Damascus clocks (UTC+3).
    private val start = 1_789_880_400_000L
    private val end = 1_789_909_200_000L

    private fun closure(reason: String?) = TemporaryClosure("c1", start, end, reason)

    @Test fun `no reason, an empty one or one of spaces is not shown`() {
        assertNull(closure(null).shownReason())
        assertNull(closure("").shownReason())
        assertNull(closure("   ").shownReason())
    }

    @Test fun `a real reason is shown, trimmed`() {
        assertEquals("سبب فعلي", closure("  سبب فعلي  ").shownReason())
    }
}
