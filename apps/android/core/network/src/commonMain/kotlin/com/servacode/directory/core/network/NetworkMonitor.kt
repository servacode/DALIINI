package com.servacode.directory.core.network

import kotlinx.coroutines.flow.Flow

interface NetworkMonitor {
    val online: Flow<Boolean>

    /**
     * True on a connection nobody is charged by the megabyte for.
     *
     * Downloading a province's map is tens of megabytes. On a Syrian mobile bundle that is a real
     * cost, and spending it without being asked is not something an app gets to do; on the home
     * or shop Wi-Fi it is free and the trip that survives a cut connection is worth having. So
     * the app waits for this, and a reader who wants it sooner asks for it in settings.
     */
    val unmetered: Flow<Boolean>
}
