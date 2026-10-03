package com.servacode.directory.feature.settings

import com.servacode.directory.core.designsystem.DirectorySwitchRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.servacode.directory.core.datastore.ThemePreference
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.Sizes
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryMenuSection
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.maps.MapPackFailure
import com.servacode.directory.core.maps.MapPackState
import com.servacode.directory.core.maps.MapPackTarget
import com.servacode.directory.core.maps.packEstimatedBytes
import kotlin.math.roundToInt

/**
 * Settings: everything about the app, and only what this app actually has.
 *
 * The theme follows the phone unless the reader picks light or dark here. There is no language
 * to choose, because the app is written in Arabic and has no second translation to offer.
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
    onEmergencyNumbers: () -> Unit = {},
    preferencesViewModel: PreferencesViewModel = hiltViewModel(),
) {
    val preferences by preferencesViewModel.values.collectAsStateWithLifecycle()
    val sync by preferencesViewModel.sync.collectAsStateWithLifecycle()
    LaunchedEffect(signedIn) { preferencesViewModel.forAccount(signedIn) }
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
                DirectoryMenuSection(SettingsCopy.SECURITY) {
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

            DirectoryMenuSection(SettingsCopy.NOTIFICATIONS) {
                DirectoryMenuRow(
                    title = SettingsCopy.ALLOW_NOTIFICATIONS,
                    onClick = { context.startActivity(NotificationSetting.systemScreen(context)) },
                    icon = DirectoryIcons.bell,
                    subtitle = if (notificationsAllowed) SettingsCopy.ALLOWED else SettingsCopy.NOT_ALLOWED,
                )
                // Which kinds, of the ones the system lets through. Signed in they are the
                // account's, kept by the backend; signed out, this device's.
                val notices = preferences.notifications
                DirectoryMenuDivider()
                DirectorySwitchRow(
                    title = SettingsCopy.NOTIFY_DUTY,
                    checked = notices.dutyReminders,
                    onCheckedChange = { on -> preferencesViewModel.setNotifications { copy(dutyReminders = on) } },
                )
                DirectorySwitchRow(
                    title = SettingsCopy.NOTIFY_NEWS,
                    checked = notices.provinceNews,
                    onCheckedChange = { on -> preferencesViewModel.setNotifications { copy(provinceNews = on) } },
                )
                if (signedIn) {
                    DirectorySwitchRow(
                        title = SettingsCopy.NOTIFY_APPLICATIONS,
                        body = SettingsCopy.NOTIFY_APPLICATIONS_BODY,
                        checked = notices.applicationStatus,
                        onCheckedChange = { on ->
                            preferencesViewModel.setNotifications { copy(applicationStatus = on) }
                        },
                    )
                }
                sync.failure?.let {
                    Text(
                        text = SettingsCopy.NOTIFY_FAILED,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(horizontal = Space.base, vertical = Space.xs)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Text(
                    text = if (sync.account) SettingsCopy.NOTIFY_ACCOUNT else SettingsCopy.NOTIFY_LOCAL,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Space.base, vertical = Space.sm),
                )
            }

            DirectoryMenuSection(SettingsCopy.DATA) {
                DirectorySwitchRow(
                    title = SettingsCopy.DATA_SAVER,
                    body = SettingsCopy.DATA_SAVER_BODY,
                    checked = preferences.dataSaver,
                    onCheckedChange = preferencesViewModel::setDataSaver,
                )
            }

            DirectoryMenuSection(SettingsCopy.SAFETY) {
                DirectoryMenuRow(
                    title = SettingsCopy.EMERGENCY_NUMBERS,
                    onClick = onEmergencyNumbers,
                    icon = DirectoryIcons.emergency,
                )
            }

            AppearanceSection()

            // What a trip needs when the connection goes, which in this country it does.
            OfflineMapSection()

            // Android owns these switches; the app sends the user to them rather than keeping a
            // copy of an answer the system can change behind its back.
            DirectoryMenuSection(SettingsCopy.PERMISSIONS) {
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

/**
 * تلقائي / فاتح / داكن, as one radio group: the choice is applied at the app's root the moment it
 * is made, so the reader sees what they picked without leaving the page.
 */
@Composable
private fun AppearanceSection(viewModel: AppearanceViewModel = hiltViewModel()) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    DirectoryMenuSection(SettingsCopy.APPEARANCE) {
        Column(Modifier.selectableGroup()) {
            ThemePreference.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.touchTarget)
                        .selectable(
                            selected = theme == option,
                            onClick = { viewModel.choose(option) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = Space.base, vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    DirectoryIcon(
                        icon = themeIcon(option),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = SettingsCopy.theme(option),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    // The row is the control; the button only shows the choice.
                    RadioButton(selected = theme == option, onClick = null)
                }
            }
        }
    }
}

private fun themeIcon(option: ThemePreference): Int = when (option) {
    ThemePreference.SYSTEM -> DirectoryIcons.settings
    ThemePreference.LIGHT -> DirectoryIcons.sun
    ThemePreference.DARK -> DirectoryIcons.moon
}

/**
 * The province's map, kept on the device.
 *
 * One row that says where the pack stands, and one action that fits that state: fetch it, stop
 * fetching it, carry on, or give the space back. The app does this by itself on a connection
 * nobody pays by the megabyte for; this is for the reader who wants it now, or not at all.
 */
@Composable
private fun OfflineMapSection(viewModel: OfflineMapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val target by viewModel.target.collectAsStateWithLifecycle()

    DirectoryMenuSection(SettingsCopy.OFFLINE_MAP) {
        DirectoryMenuRow(
            title = packTitle(state),
            onClick = {
                when (state) {
                    is MapPackState.Downloading -> if ((state as MapPackState.Downloading).running) {
                        viewModel.pause()
                    } else {
                        viewModel.download()
                    }
                    is MapPackState.Ready, MapPackState.Unknown -> Unit
                    else -> viewModel.download()
                }
            },
            icon = DirectoryIcons.map,
            subtitle = packDetail(state, target),
            trailing = state !is MapPackState.Ready && state != MapPackState.Unknown,
        )
        if (state is MapPackState.Ready) {
            DirectoryMenuDivider()
            DirectoryMenuRow(
                title = SettingsCopy.OFFLINE_DELETE,
                onClick = viewModel::remove,
                icon = DirectoryIcons.close,
                danger = true,
                trailing = false,
            )
        }
    }
}

/** What the row says it is offering, which is different in each state. */
@Composable
@ReadOnlyComposable
private fun packTitle(state: MapPackState): String = when (state) {
    MapPackState.Unknown -> SettingsCopy.OFFLINE_MAP
    MapPackState.Absent -> SettingsCopy.OFFLINE_DOWNLOAD
    is MapPackState.Downloading ->
        if (state.running) SettingsCopy.OFFLINE_DOWNLOADING else SettingsCopy.OFFLINE_PAUSED
    is MapPackState.Ready -> SettingsCopy.OFFLINE_READY
    is MapPackState.Failed -> SettingsCopy.OFFLINE_FAILED
}

/**
 * The cost, the progress or the reason, under the title.
 *
 * Before a download there is only an estimate, computed from the box and the zooms; once one is
 * running the engine's own byte count replaces it, because an estimate shown next to a real figure
 * is the one that will be wrong.
 */
@Composable
@ReadOnlyComposable
private fun packDetail(state: MapPackState, target: MapPackTarget?): String = when (state) {
    MapPackState.Unknown -> SettingsCopy.OFFLINE_NO_PROVINCE
    MapPackState.Absent -> when (target) {
        null -> SettingsCopy.OFFLINE_HINT
        else -> stringResource(
            R.string.settings_offline_estimate,
            megabytes(packEstimatedBytes(target.box)),
        )
    }
    is MapPackState.Downloading -> when (val fraction = state.fraction) {
        null -> SettingsCopy.OFFLINE_COUNTING
        else -> when {
            state.running -> stringResource(
                R.string.settings_offline_progress,
                (fraction * 100).roundToInt(),
                megabytes(state.bytes),
            )
            else -> stringResource(R.string.settings_offline_paused_at, (fraction * 100).roundToInt())
        }
    }
    is MapPackState.Ready -> stringResource(R.string.settings_offline_size, megabytes(state.bytes))
    is MapPackState.Failed -> stringResource(
        when (state.reason) {
            MapPackFailure.CONNECTION -> R.string.settings_offline_failed_connection
            MapPackFailure.SERVER -> R.string.settings_offline_failed_server
            MapPackFailure.TILE_LIMIT -> R.string.settings_offline_failed_limit
            MapPackFailure.OTHER -> R.string.settings_offline_failed_other
        },
    )
}

/** Bytes as a reader counts them, rounded to the nearest megabyte and never below one. */
private fun megabytes(bytes: Long): Int =
    ((bytes + HALF_MEGABYTE) / MEGABYTE).toInt().coerceAtLeast(1)

private const val MEGABYTE = 1_000_000L
private const val HALF_MEGABYTE = 500_000L

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
    val TITLE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_title)
    val APPEARANCE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_appearance)
    val NOTIFY_DUTY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_duty)
    val NOTIFY_NEWS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_news)
    val NOTIFY_APPLICATIONS: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_applications)
    val NOTIFY_APPLICATIONS_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_applications_body)
    val NOTIFY_LOCAL: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_local)
    val NOTIFY_ACCOUNT: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_account)
    val NOTIFY_FAILED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notify_failed)
    val DATA: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_data)
    val DATA_SAVER: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_data_saver)
    val DATA_SAVER_BODY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_data_saver_body)
    val SAFETY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_safety)
    val EMERGENCY_NUMBERS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.emergency_title)

    @Composable
    @ReadOnlyComposable
    fun theme(option: ThemePreference): String = stringResource(
        when (option) {
            ThemePreference.SYSTEM -> R.string.settings_theme_system
            ThemePreference.LIGHT -> R.string.settings_theme_light
            ThemePreference.DARK -> R.string.settings_theme_dark
        },
    )
    val SECURITY: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_security)
    val CHANGE_PASSWORD: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_change_password)
    val CHANGE_PHONE: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_change_phone)
    val NOTIFICATIONS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_notifications)
    val ALLOW_NOTIFICATIONS: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_allow_notifications)
    val ALLOWED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_allowed)
    val NOT_ALLOWED: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_not_allowed)
    val PERMISSIONS: String @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_permissions)
    val OFFLINE_MAP: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_map)
    val OFFLINE_HINT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_map_hint)
    val OFFLINE_NO_PROVINCE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_no_province)
    val OFFLINE_DOWNLOAD: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_download)
    val OFFLINE_DOWNLOADING: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_downloading)
    val OFFLINE_COUNTING: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_counting)
    val OFFLINE_PAUSED: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_paused)
    val OFFLINE_READY: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_ready)
    val OFFLINE_DELETE: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_delete)
    val OFFLINE_FAILED: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_offline_failed)
    val SYSTEM_SETTINGS: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_system_settings)
    val SYSTEM_SETTINGS_HINT: String
        @Composable @ReadOnlyComposable get() = stringResource(R.string.settings_system_settings_hint)
}
