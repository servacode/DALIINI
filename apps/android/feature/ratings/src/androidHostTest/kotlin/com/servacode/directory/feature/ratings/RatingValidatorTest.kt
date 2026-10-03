package com.servacode.directory.feature.ratings

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingValidatorTest {
    @Test fun acceptsBoundaryValues() {
        assertEquals(1, RatingValidator.requireValid(1))
        assertEquals(5, RatingValidator.requireValid(5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeValue() {
        RatingValidator.requireValid(0)
    }
}
