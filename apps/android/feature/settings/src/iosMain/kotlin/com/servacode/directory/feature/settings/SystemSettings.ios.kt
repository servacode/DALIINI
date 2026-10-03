package com.servacode.directory.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun rememberSystemSettings(): SystemSettings = remember { IosSystemSettings }

/**
 * The notification centre's answer, and the Settings app's pages for this app. A notice the
 * system delivers quietly (provisional) or to an App Clip (ephemeral) still counts as allowed.
 */
private object IosSystemSettings : SystemSettings {
    override suspend fun noticesAllowed(): Boolean = suspendCancellableCoroutine { answer ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            answer.resume(
                status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral,
            )
        }
    }

    // The app's own notification page in Settings (iOS 16, the app's lowest).
    override fun openNotices() = open(UIApplicationOpenNotificationSettingsURLString)

    override fun openApp() = open(UIApplicationOpenSettingsURLString)

    private fun open(address: String) {
        val url = NSURL.URLWithString(address) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
