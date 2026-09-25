package com.servacode.directory

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.Manifest
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.servacode.directory.core.designsystem.R as DesignSystemR
import com.servacode.directory.core.model.DirectoryBrand
import com.servacode.directory.core.network.PushMessageData
import com.servacode.directory.core.network.PushRegistrationCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob

/**
 * Receives token rotations and pushes.
 *
 * A push carries identifiers only — `notificationId` and `type` — never content; the app shows
 * a neutral notice and the user sees the substance only after the app fetches it over REST.
 * Neither the token nor the payload is logged.
 */
@AndroidEntryPoint
class DirectoryMessagingService : FirebaseMessagingService() {
    @Inject lateinit var coordinator: PushRegistrationCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { runCatching { coordinator.onTokenAvailable(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        PushMessageData.from(message.data) ?: return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        // From Android 13 posting needs the runtime permission; without it the notice is dropped
        // and the update is still there the next time the app opens.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "التحديثات", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Ours, not Android's generic dot: the status bar gets the mark drawn flat, because a
        // small icon is filled with one colour and nothing else of it survives; the shade gets
        // the artwork itself beside the words, and the brand's green as the accent.
        val notice = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, DesignSystemR.color.token_colors_primary))
            .setLargeIcon(
                BitmapFactory.decodeResource(resources, DesignSystemR.drawable.brand_symbol),
            )
            .setContentTitle(DirectoryBrand.NAME)
            .setContentText("لديك تحديث جديد")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTICE, notice)
        } catch (withdrawn: SecurityException) {
            // The permission was withdrawn after the check above.
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL = "updates"
        const val NOTICE = 1
    }
}
