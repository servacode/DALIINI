package com.servacode.directory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.ThemePreference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.designsystem.LocalDataSaver
import com.servacode.directory.core.network.MaintenanceCoordinator
import com.servacode.directory.core.network.MaintenanceState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var session: SessionCoordinator
    @Inject lateinit var preferences: DirectoryPreferencesStore

    /** The reader's theme choice, read once per activity rather than rebuilt on each frame. */
    private val themePreference by lazy { preferences.values.map { it.themePreference } }

    /** «توفير البيانات», for every screen that would fetch a picture. */
    private val dataSaver by lazy { preferences.values.map { it.dataSaver }.distinctUntilChanged() }
    @Inject lateinit var maintenance: MaintenanceState
    @Inject lateinit var maintenanceRetry: MaintenanceCoordinator

    /**
     * Where the app was asked to go — an App Link, a tapped notice, the widget — held until the
     * navigation graph can take it. The latest wins: two taps in a row mean the second.
     */
    private val entryChannel = Channel<AppEntry>(Channel.CONFLATED)
    private val entries: Flow<AppEntry> = entryChannel.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super: the system splash (Android 12+, or the AndroidX emulation below it) has
        // shown the brand mark from Theme.Directory.Starting; this moves the window on to
        // Theme.Directory. The app's own splash continues from the same mark. Nothing waits here.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Only on a real start: an activity recreated for a rotation has already gone there.
        if (savedInstanceState == null) AppEntries.from(intent)?.let(entryChannel::trySend)
        setContent {
            // The reader's choice in Settings; until it is read, the phone's own setting.
            val theme by themePreference.collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            val saving by dataSaver.collectAsStateWithLifecycle(initialValue = false)
            DirectoryTheme(darkTheme = theme.isDark(isSystemInDarkTheme())) {
                CompositionLocalProvider(LocalDataSaver provides saving) {
                    MaintenanceGate(maintenance.status, maintenanceRetry) {
                        DirectoryApp(session.state, entries)
                    }
                }
            }
        }
    }

    /** A link or a notice while the app is already open: the same activity goes there. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppEntries.from(intent)?.let(entryChannel::trySend)
    }
}
