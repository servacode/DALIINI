package com.servacode.directory.feature.home

/**
 * When Home offers «توفير البيانات»: once, on a connection that is charged by the megabyte, to a
 * reader who has not turned it on and has not been asked before. Asking again after a "no" would
 * be nagging; asking on Wi-Fi would be asking the wrong question.
 */
object DataSaverSuggestion {
    fun shouldOffer(online: Boolean, unmetered: Boolean, dataSaver: Boolean, alreadySuggested: Boolean): Boolean =
        online && !unmetered && !dataSaver && !alreadySuggested
}
