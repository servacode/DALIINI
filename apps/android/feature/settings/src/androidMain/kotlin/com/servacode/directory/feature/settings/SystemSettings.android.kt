package com.servacode.directory.feature.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSystemSettings(): SystemSettings {
    val context = LocalContext.current
    return remember(context) { AndroidSystemSettings(context) }
}

private class AndroidSystemSettings(private val context: Context) : SystemSettings {
    override suspend fun noticesAllowed(): Boolean =
        context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: true

    override fun openNotices() {
        context.startActivity(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                appDetails()
            },
        )
    }

    override fun openApp() {
        context.startActivity(appDetails())
    }

    private fun appDetails() =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
}
