package com.servacode.directory.feature.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSaverSuggestionTest {
    @Test fun `offered once, on a connection paid by the megabyte`() {
        assertTrue(DataSaverSuggestion.shouldOffer(online = true, unmetered = false, dataSaver = false, alreadySuggested = false))
    }

    @Test fun `not on Wi-Fi, not offline, not when on, not a second time`() {
        assertFalse(DataSaverSuggestion.shouldOffer(online = true, unmetered = true, dataSaver = false, alreadySuggested = false))
        assertFalse(DataSaverSuggestion.shouldOffer(online = false, unmetered = false, dataSaver = false, alreadySuggested = false))
        assertFalse(DataSaverSuggestion.shouldOffer(online = true, unmetered = false, dataSaver = true, alreadySuggested = false))
        assertFalse(DataSaverSuggestion.shouldOffer(online = true, unmetered = false, dataSaver = false, alreadySuggested = true))
    }
}
