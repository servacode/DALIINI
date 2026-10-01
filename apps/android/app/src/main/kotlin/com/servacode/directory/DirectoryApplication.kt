package com.servacode.directory

import android.app.Application
import android.os.StrictMode
import androidx.lifecycle.ProcessLifecycleOwner
import com.servacode.directory.widget.DutyWidgetRefresher
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class DirectoryApplication : Application() {
    @Inject lateinit var realtimeCoordinator: RealtimeCoordinator
    @Inject lateinit var pushSetup: PushSetup
    @Inject lateinit var offlineMaps: OfflineMapCoordinator
    @Inject lateinit var dutyWidget: DutyWidgetRefresher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        // First, so a crash in anything after it is reported; a no-op without a DSN.
        CrashReporting.start(this)
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Debug builds say where a resource that is never released was acquired: CloseGuard
            // alone reports only that one leaked (INT-089). Logged, never fatal.
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build(),
            )
        }
        MapLibre.getInstance(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(realtimeCoordinator)
        pushSetup.start(this, scope)
        // The province's map, fetched once on a connection nobody pays for. It watches rather than
        // acts: nothing is downloaded until there is a province, a Wi-Fi and no refusal on file.
        offlineMaps.start(scope)
        // The home-screen widget redraws whenever the app stores a fresh home snapshot.
        dutyWidget.start(scope)
    }
}
