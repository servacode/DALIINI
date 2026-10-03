package com.servacode.directory

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.datastore.NotificationPreferences
import com.servacode.directory.core.model.DirectoryBrand
import com.servacode.directory.core.model.NotificationCategory
import com.servacode.directory.core.network.PushMessageData
import com.servacode.directory.core.network.PushRegistrationCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.SupervisorJob

/**
 * Receives token rotations and pushes.
 *
 * A push carries identifiers only — `notificationId` and `type` — never content; the app shows
 * a neutral notice and the user sees the substance only after the app fetches it over REST.
 * Neither the token nor the payload is logged.
 *
 * The reader's choices in Settings are honoured here too, on the device: a push whose kind they
 * turned off is not shown. Signed in, the backend already stops sending those (DECISION-077);
 * checking again catches a push sent just before the change. The message stays in the inbox
 * either way.
 */
@AndroidEntryPoint
class DirectoryMessagingService : FirebaseMessagingService() {
    @Inject lateinit var coordinator: PushRegistrationCoordinator
    @Inject lateinit var preferences: DirectoryPreferencesStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { runCatching { coordinator.onTokenAvailable(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = PushMessageData.from(message.data) ?: return
        // On FCM's own worker thread, which may block briefly: one read of the stored choices.
        val choices = runCatching { runBlocking { preferences.values.first().notifications } }
            .getOrDefault(NotificationPreferences())
        if (!choices.allows(NotificationCategory.of(data.type))) return
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
                NotificationChannel(
                    CHANNEL,
                    getString(R.string.app_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        // Opens where the notice is about: a gap nudge on duty scheduling, an hours reminder on
        // the owner's facilities, anything else in the inbox, where its words are.
        val open = PendingIntent.getActivity(
            this,
            0,
            AppEntries.notice(this, data),
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
            .setContentText(getString(R.string.app_notification_body))
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
