package com.servacode.directory.feature.duty

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DutyValidatorTest {
    @Test fun acceptsPositiveRange() {
        assertTrue(DutyValidator.isValid(1_000L, 2_000L))
    }

    @Test fun rejectsZeroOrReverseRange() {
        assertFalse(DutyValidator.isValid(0L, 2_000L))
        assertFalse(DutyValidator.isValid(2_000L, 2_000L))
        assertFalse(DutyValidator.isValid(3_000L, 2_000L))
    }
}
