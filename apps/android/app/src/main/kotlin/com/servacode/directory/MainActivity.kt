package com.servacode.directory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.ThemePreference
import kotlinx.coroutines.flow.map
import com.servacode.directory.core.designsystem.DirectoryTheme
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
    @Inject lateinit var maintenance: MaintenanceState
    @Inject lateinit var maintenanceRetry: MaintenanceCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super: the system splash (Android 12+, or the AndroidX emulation below it) has
        // shown the brand mark from Theme.Directory.Starting; this moves the window on to
        // Theme.Directory. The app's own splash continues from the same mark. Nothing waits here.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            // The reader's choice in Settings; until it is read, the phone's own setting.
            val theme by themePreference.collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            DirectoryTheme(darkTheme = theme.isDark(isSystemInDarkTheme())) {
                MaintenanceGate(maintenance.status, maintenanceRetry) {
                    DirectoryApp(session.state)
                }
            }
        }
    }
}
