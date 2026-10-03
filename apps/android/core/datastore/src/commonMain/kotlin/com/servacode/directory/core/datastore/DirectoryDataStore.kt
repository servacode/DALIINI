package com.servacode.directory.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * The one preferences DataStore the device keeps: the reader's preferences and the anonymous id
 * live in it, each under its own keys.
 *
 * DataStore allows one instance per file in a process, so each platform makes it exactly once
 * and hands the same holder to both: Android from its Context, the iPhone in its Application
 * Support folder (DECISION-092). The file has the same name on both.
 */
class DirectoryDataStore(val store: DataStore<Preferences>) {
    companion object {
        /** The DataStore's name; Android adds `.preferences_pb` and the iPhone does the same. */
        const val NAME = "directory_preferences"
    }
}
