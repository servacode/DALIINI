package com.servacode.directory.feature.owner

import com.servacode.directory.core.designsystem.StatusTone
import com.servacode.directory.core.model.OwnerFacilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The colour a status is read in: the tone the shared vocabulary gives it
 * (`packages/design-tokens/vocabulary.json`, `facilityStatus`), so the owner's app and the
 * operators' console colour a facility the same way. The word beside it always carries the
 * meaning; the tone must never contradict it.
 */
class OwnerStatusToneTest {
    @Test
    fun `a live facility reads as settled`() {
        assertEquals(StatusTone.POSITIVE, OwnerFacilityStatus.ACTIVE.tone())
    }

    @Test
    fun `waiting on a reviewer is information, a request to reverify is a warning`() {
        assertEquals(StatusTone.INFO, OwnerFacilityStatus.SUBMITTED.tone())
        assertEquals(StatusTone.WARNING, OwnerFacilityStatus.REVERIFICATION_REQUIRED.tone())
    }

    @Test
    fun `a suspension warns, a closure is final, a draft is a fact`() {
        assertEquals(StatusTone.WARNING, OwnerFacilityStatus.SUSPENDED.tone())
        assertEquals(StatusTone.DANGER, OwnerFacilityStatus.CLOSED.tone())
        assertEquals(StatusTone.NEUTRAL, OwnerFacilityStatus.DRAFT.tone())
    }

    @Test
    fun `every status the backend can send has a tone`() {
        val tones = OwnerFacilityStatus.entries.map { it.tone() }
        assertEquals(OwnerFacilityStatus.entries.size, tones.size)
    }
}
