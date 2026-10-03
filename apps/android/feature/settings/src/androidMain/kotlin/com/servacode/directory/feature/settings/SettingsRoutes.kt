package com.servacode.directory.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.model.LegalPageKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltEmergencyNumbersViewModel @Inject constructor(
    repository: EmergencyNumbersRepository,
) : EmergencyNumbersViewModel(repository)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltLegalViewModel @Inject constructor(
    legal: LegalRepository,
) : LegalViewModel(legal)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltPreferencesViewModel @Inject constructor(
    preferences: DirectoryPreferencesStore,
    notifications: NotificationPreferencesSync,
) : PreferencesViewModel(preferences, notifications)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltAppearanceViewModel @Inject constructor(
    preferences: DirectoryPreferencesStore,
) : AppearanceViewModel(preferences)

/** `EmergencyNumbersScreen` as the app's navigation opens it: a number opens the dialer. */
@Composable
fun EmergencyNumbersRoute(
    onBack: () -> Unit,
    viewModel: HiltEmergencyNumbersViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    EmergencyNumbersScreen(
        viewModel = viewModel,
        onBack = onBack,
        onCall = { dial(context, it) },
    )
}

private fun dial(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    } catch (_: ActivityNotFoundException) {
        // A device with no dialer (a tablet): the number is on screen to be read.
    }
}

/** `HelpScreen` as the app's navigation opens it. */
@Composable
fun HelpRoute(
    onPage: (LegalPageKey) -> Unit,
    onBack: () -> Unit,
    appVersion: String,
    viewModel: HiltLegalViewModel = hiltViewModel(),
) {
    HelpScreen(
        viewModel = viewModel,
        onPage = onPage,
        onBack = onBack,
        appVersion = appVersion,
    )
}

/** `LegalPageScreen` as the app's navigation opens it. */
@Composable
fun LegalPageRoute(
    key: LegalPageKey,
    onBack: () -> Unit,
    viewModel: HiltLegalViewModel = hiltViewModel(),
) {
    LegalPageScreen(
        viewModel = viewModel,
        key = key,
        onBack = onBack,
    )
}

/** `SettingsScreen` as the app's navigation opens it, with the offline map MapLibre keeps. */
@Composable
fun SettingsRoute(
    onChangePassword: () -> Unit,
    onChangePhone: () -> Unit,
    onBack: () -> Unit,
    signedIn: Boolean,
    onEmergencyNumbers: () -> Unit = {},
    preferencesViewModel: HiltPreferencesViewModel = hiltViewModel(),
    appearanceViewModel: HiltAppearanceViewModel = hiltViewModel(),
) {
    SettingsScreen(
        preferencesViewModel = preferencesViewModel,
        appearanceViewModel = appearanceViewModel,
        onChangePassword = onChangePassword,
        onChangePhone = onChangePhone,
        onBack = onBack,
        signedIn = signedIn,
        onEmergencyNumbers = onEmergencyNumbers,
        offlineMap = { OfflineMapSection(hiltViewModel()) },
    )
}
