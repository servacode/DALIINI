package com.servacode.directory.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuGroup
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySectionLabel
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space

/**
 * Settings: everything about the app, and only what this app actually has.
 *
 * There is no theme to choose — the app has one — and no language to choose, because it is
 * written in Arabic and has no second translation to offer.
 *
 * This is the one place for the password and for whether notices are allowed. The profile used
 * to offer both as well, so a reader who wanted to change a password had two rows that did the
 * same thing and no way to know they were the same.
 *
 * **Help and information are not here.** They are about the platform, not about this app's
 * behaviour, and they are read once rather than changed; they sit in the profile, one tap from
 * where a reader is rather than two.
 *
 * Whether notices are allowed is Android's answer, not ours: the app reads the system's switch
 * and opens the system's screen rather than keeping a copy of a decision the user can change
 * behind its back.
 *
 * The page wears the profile's own shape — a label, then a card of rows — because it is the
 * same kind of menu one tap deeper, and two shapes for one thing is what made the profile hard
 * to read before it was redrawn.
 */
@Composable
fun SettingsScreen(
    onChangePassword: () -> Unit,
    onChangePhone: () -> Unit,
    onBack: () -> Unit,
    signedIn: Boolean,
) {
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(NotificationSetting.allowed(context)) }
    // Read again on the way back from the system's screen, where it may have just been changed.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = NotificationSetting.allowed(context)
    }

    DirectoryPage(
        topBar = { DirectoryTopBar(title = SettingsCopy.TITLE, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            if (signedIn) {
                Section(SettingsCopy.SECURITY) {
                    DirectoryMenuRow(
                        title = SettingsCopy.CHANGE_PASSWORD,
                        onClick = onChangePassword,
                        icon = DirectoryIcons.verified,
                    )
                    DirectoryMenuDivider()
                    // The number the account signs in with, changed on the one screen that can
                    // do it. Personal information offers the same row, because it is also one
                    // of the person's own details; both open this, so there is one way to it.
                    DirectoryMenuRow(
                        title = SettingsCopy.CHANGE_PHONE,
                        onClick = onChangePhone,
                        icon = DirectoryIcons.phone,
                    )
                }
            }

            Section(SettingsCopy.NOTIFICATIONS) {
                DirectoryMenuRow(
                    title = SettingsCopy.ALLOW_NOTIFICATIONS,
                    onClick = { context.startActivity(NotificationSetting.systemScreen(context)) },
                    icon = DirectoryIcons.bell,
                    subtitle = if (notificationsAllowed) SettingsCopy.ALLOWED else SettingsCopy.NOT_ALLOWED,
                )
            }

            // Android owns these switches; the app sends the user to them rather than keeping a
            // copy of an answer the system can change behind its back.
            Section(SettingsCopy.PERMISSIONS) {
                DirectoryMenuRow(
                    title = SettingsCopy.SYSTEM_SETTINGS,
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                    icon = DirectoryIcons.pin,
                    subtitle = SettingsCopy.SYSTEM_SETTINGS_HINT,
                )
            }
        }
    }
}

/** A label and the card under it: one subject, drawn as one thing. */
@Composable
private fun Section(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        DirectorySectionLabel(label)
        DirectoryMenuGroup(content = content)
    }
}

/** Whether the system lets this app show a notice, and the screen where that is decided. */
private object NotificationSetting {
    fun allowed(context: android.content.Context): Boolean {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        return manager?.areNotificationsEnabled() ?: true
    }

    fun systemScreen(context: android.content.Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
        }
}

/** The words of the settings, provisional until product copy is approved. */
object SettingsCopy {
    const val TITLE = "الإعدادات"
    const val SECURITY = "الحساب والأمان"
    const val CHANGE_PASSWORD = "تغيير كلمة المرور"
    const val CHANGE_PHONE = "تغيير رقم الهاتف"
    const val NOTIFICATIONS = "الإشعارات"
    const val ALLOW_NOTIFICATIONS = "السماح بالإشعارات"
    const val ALLOWED = "مسموح"
    const val NOT_ALLOWED = "غير مسموح"
    const val PERMISSIONS = "الأذونات"
    const val SYSTEM_SETTINGS = "إعدادات التطبيق في النظام"
    const val SYSTEM_SETTINGS_HINT = "الموقع والإشعارات والتخزين"
}
