package com.servacode.directory.feature.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSaverSuggestionTest {
    @Test fun `offered once, on a connection paid by the megabyte`() {
        assertTrue(offer(online = true, unmetered = false, dataSaver = false, alreadySuggested = false))
    }

    @Test fun `not on Wi-Fi, not offline, not when on, not a second time`() {
        assertFalse(offer(online = true, unmetered = true, dataSaver = false, alreadySuggested = false))
        assertFalse(offer(online = false, unmetered = false, dataSaver = false, alreadySuggested = false))
        assertFalse(offer(online = true, unmetered = false, dataSaver = true, alreadySuggested = false))
        assertFalse(offer(online = true, unmetered = false, dataSaver = false, alreadySuggested = true))
    }

    private fun offer(online: Boolean, unmetered: Boolean, dataSaver: Boolean, alreadySuggested: Boolean) =
        DataSaverSuggestion.shouldOffer(online, unmetered, dataSaver, alreadySuggested)
}
