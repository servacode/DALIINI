package com.servacode.directory.feature.owner

import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.model.OwnerFacilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The colour a status is read in. The word beside it is always the backend's own label, so the
 * tone may never be the only thing carrying the meaning — but it must never contradict it.
 */
class OwnerStatusToneTest {
    @Test
    fun `a live facility reads as settled`() {
        assertEquals(StatusTone.POSITIVE, OwnerFacilityStatus.ACTIVE.tone())
    }

    @Test
    fun `waiting on a reviewer reads as pending`() {
        assertEquals(StatusTone.PENDING, OwnerFacilityStatus.SUBMITTED.tone())
        assertEquals(StatusTone.PENDING, OwnerFacilityStatus.REVERIFICATION_REQUIRED.tone())
    }

    @Test
    fun `only a suspension reads as wrong`() {
        assertEquals(StatusTone.DANGER, OwnerFacilityStatus.SUSPENDED.tone())
        assertEquals(StatusTone.NEUTRAL, OwnerFacilityStatus.CLOSED.tone())
        assertEquals(StatusTone.NEUTRAL, OwnerFacilityStatus.DRAFT.tone())
    }

    @Test
    fun `every status the backend can send has a tone`() {
        val tones = OwnerFacilityStatus.entries.map { it.tone() }
        assertEquals(OwnerFacilityStatus.entries.size, tones.size)
    }
}
