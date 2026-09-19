package com.servacode.directory

import android.app.Application
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
        MapLibre.getInstance(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(realtimeCoordinator)
        pushSetup.start(this, scope)
    }
}
