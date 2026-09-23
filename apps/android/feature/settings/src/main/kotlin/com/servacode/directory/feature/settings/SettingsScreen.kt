package com.servacode.directory.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectorySettingRow
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.SectionHeader
import com.servacode.directory.core.designsystem.Space

/**
 * Settings: only what this app actually has.
 *
 * There is no theme to choose — the app has one — and no language to choose, because it is
 * written in Arabic and has no second translation to offer. What is here is what exists: the
 * account's own security, the permissions Android owns, the published pages, and the version.
 */
@Composable
fun SettingsScreen(
    onChangePassword: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit,
    appVersion: String,
    signedIn: Boolean,
) {
    val context = LocalContext.current

    DirectoryPage(
        topBar = { DirectoryTopBar(title = SettingsCopy.TITLE, onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (signedIn) {
                SectionHeader(
                    title = SettingsCopy.SECURITY,
                    modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
                )
                DirectorySettingRow(
                    title = SettingsCopy.CHANGE_PASSWORD,
                    onClick = onChangePassword,
                    icon = DirectoryIcons.verified,
                )
                DirectorySettingRow(
                    title = SettingsCopy.NOTIFICATIONS,
                    onClick = onNotifications,
                    icon = DirectoryIcons.bell,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            SectionHeader(
                title = SettingsCopy.PERMISSIONS,
                modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
            )
            // Android owns these switches; the app sends the user to them rather than keeping a
            // copy of an answer the system can change behind its back.
            DirectorySettingRow(
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
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionHeader(
                title = SettingsCopy.ABOUT,
                modifier = Modifier.padding(horizontal = Space.screen, vertical = Space.xs),
            )
            DirectorySettingRow(
                title = SettingsCopy.HELP,
                onClick = onHelp,
                icon = DirectoryIcons.info,
            )
            Text(
                text = SettingsCopy.version(appVersion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Space.screen),
            )
        }
    }
}

/** The words of the settings, provisional until product copy is approved. */
object SettingsCopy {
    const val TITLE = "الإعدادات"
    const val SECURITY = "الحساب والأمان"
    const val CHANGE_PASSWORD = "تغيير كلمة المرور"
    const val NOTIFICATIONS = "الإشعارات"
    const val PERMISSIONS = "الأذونات"
    const val SYSTEM_SETTINGS = "إعدادات التطبيق في النظام"
    const val ABOUT = "المساعدة والمعلومات"
    const val HELP = "المساعدة والمعلومات"

    fun version(name: String): String = "إصدار التطبيق $name"
}
