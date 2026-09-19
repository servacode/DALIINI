package com.servacode.directory

import android.app.Application
import android.os.StrictMode
import androidx.lifecycle.ProcessLifecycleOwner
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
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
    }
}
