package com.servacode.directory

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.servacode.directory.core.model.RoundedDistance
import com.servacode.directory.core.model.roundedDistance
import com.servacode.directory.feature.navigation.GuidanceNotice
import com.servacode.directory.feature.navigation.NavigationKeepAlive
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import com.servacode.directory.core.designsystem.R as DesignSystemR

/**
 * The trip as the shade shows it, shared by the screen that drives it and the service that
 * shows it. One trip at a time: null when there is none.
 */
@Singleton
class NavigationNotices @Inject constructor() {
    private val current = MutableStateFlow<GuidanceNotice?>(null)
    val notice: StateFlow<GuidanceNotice?> = current.asStateFlow()

    fun set(value: GuidanceNotice?) {
        current.value = value
    }
}

/**
 * [NavigationKeepAlive] on Android: a foreground service of the location type for as long as a
 * trip is under way (DECISION-078).
 *
 * Started while the trip's screen is in front, which is the only time Android lets a location
 * service start. If it refuses anyway — no location permission, or a system that will not allow
 * it — the trip goes on as before, on screen only, rather than failing.
 */
@Singleton
class AndroidNavigationKeepAlive @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notices: NavigationNotices,
) : NavigationKeepAlive {
    override fun show(notice: GuidanceNotice) {
        val starting = notices.notice.value == null
        notices.set(notice)
        if (!starting) return
        try {
            ContextCompat.startForegroundService(context, Intent(context, NavigationForegroundService::class.java))
        } catch (refused: IllegalStateException) {
            // Not allowed to start from where the app stands now; the screen still guides.
        } catch (refused: SecurityException) {
            // No location permission at the moment; nothing to keep alive without it.
        }
    }

    override fun stop() {
        if (notices.notice.value == null) return
        notices.set(null)
        context.stopService(Intent(context, NavigationForegroundService::class.java))
    }
}

/**
 * Keeps the process and its location readings alive while a trip is under way, and says so in
 * the shade: how far is left and about how long, or that a new way is being found. Tapping the
 * notice returns to the trip. It makes no sound of its own; the voice guidance is the sound.
 */
@AndroidEntryPoint
class NavigationForegroundService : Service() {
    @Inject lateinit var notices: NavigationNotices

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    getString(R.string.app_navigation_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notice = notices.notice.value
        if (notice == null) {
            // The trip ended before the service got going.
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this,
                NOTICE,
                build(notice),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                },
            )
        } catch (refused: RuntimeException) {
            // Android 14 refuses a location service without the permission, and Android 12
            // one started from the background. Either way there is nothing to keep alive.
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch {
            notices.notice.filterNotNull().collect { update(it) }
        }
        // A trip is not resumed by the system after the process is gone: the screen that ran it
        // is gone with it.
        return START_NOT_STICKY
    }

    private fun update(notice: GuidanceNotice) {
        val manager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            manager.notify(NOTICE, build(notice))
        } catch (withdrawn: SecurityException) {
            // The permission was withdrawn after the check above.
        }
    }

    private fun build(notice: GuidanceNotice) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(ContextCompat.getColor(this, DesignSystemR.color.token_colors_primary))
        .setContentTitle(getString(R.string.app_navigation_title))
        .setContentText(text(notice))
        .setContentIntent(returnToTrip())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()

    private fun text(notice: GuidanceNotice): String {
        if (notice.rerouting) return getString(R.string.app_navigation_rerouting)
        val distance = when (val rounded = roundedDistance(notice.remainingMeters.toDouble())) {
            is RoundedDistance.Metres -> getString(DesignSystemR.string.ds_distance_metres, rounded.value)
            is RoundedDistance.Kilometres ->
                getString(DesignSystemR.string.ds_distance_kilometres, rounded.whole, rounded.tenth)
        }
        val minutes = resources.getQuantityString(
            R.plurals.app_navigation_minutes,
            notice.remainingMinutes,
            notice.remainingMinutes,
        )
        return getString(R.string.app_navigation_remaining, distance, minutes)
    }

    /** Back to the trip: the app's own task brought to the front, as the launcher would. */
    private fun returnToTrip(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        } ?: return null
        return PendingIntent.getActivity(
            this,
            0,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL = "navigation"
        const val NOTICE = 2
    }
}

/** The trip screen asks for a [NavigationKeepAlive]; the app is what can run a service. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NavigationKeepAliveModule {
    @Binds
    abstract fun bindKeepAlive(impl: AndroidNavigationKeepAlive): NavigationKeepAlive
}
