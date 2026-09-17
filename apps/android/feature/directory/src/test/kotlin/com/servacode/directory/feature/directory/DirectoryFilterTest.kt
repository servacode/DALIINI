package com.servacode.directory.feature.directory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectoryFilterTest {
    @Test fun flagsAreIndependent() {
        val value = DirectoryFilter(openNow = true)
        assertTrue(value.openNow)
        assertFalse(value.dutyNow)
    }
}
