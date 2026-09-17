package com.servacode.directory

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import javax.inject.Inject

@HiltAndroidApp
class DirectoryApplication : Application() {
    @Inject lateinit var realtimeCoordinator: RealtimeCoordinator

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(realtimeCoordinator)
    }
}
