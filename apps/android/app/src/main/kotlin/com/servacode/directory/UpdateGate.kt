package com.servacode.directory

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.designsystem.DirectoryConfirmDialog
import com.servacode.directory.core.designsystem.UpdateRequiredScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Shows [content] and covers it when the backend says this build is too old.
 *
 * Built like [MaintenanceGate]: the host stays composed underneath, so nothing is torn down.
 * The difference is that this one has no way back. Maintenance ends by itself and the notice
 * lifts; a build below the minimum stays below it until a newer one is installed, so there is
 * no retry here — only the way out.
 *
 * A build that still works but has a newer one after it gets an offer instead (DECISION-077):
 * a dialog with «تحديث» and «لاحقاً», shown once per newer build. Either answer is remembered,
 * so the same build is never offered twice, and the next one is.
 */
@Composable
fun UpdateGate(
    verdict: StateFlow<VersionCheck.Verdict>,
    versionCode: Int,
    check: VersionCheck,
    preferences: DirectoryPreferencesStore,
    content: @Composable () -> Unit,
) {
    val current by verdict.collectAsStateWithLifecycle()
    val offered by remember(preferences) { preferences.values.map { it.updateOfferedVersionCode } }
        .collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Once per process: the answer does not change while the app is open, and a build cannot
    // become acceptable without being replaced.
    LaunchedEffect(Unit) { check.refresh(versionCode) }

    Box {
        content()
        when (val value = current) {
            is VersionCheck.Verdict.TooOld -> UpdateRequiredScreen(
                message = value.notice,
                onOpenStore = value.storeUrl?.let { url -> { openStore(context, url) } },
            )
            // Not until the stored answer is read, so a build already set aside never flashes up.
            is VersionCheck.Verdict.Newer -> if (offered != null && offered!! < value.latestVersionCode) {
                val answered = { scope.launch { preferences.setUpdateOffered(value.latestVersionCode) } }
                DirectoryConfirmDialog(
                    title = stringResource(R.string.app_update_available_title),
                    body = value.notice ?: stringResource(R.string.app_update_available_body),
                    confirm = stringResource(R.string.app_update_available_action),
                    onConfirm = {
                        answered()
                        openStore(context, value.storeUrl)
                    },
                    onDismiss = { answered() },
                    dismiss = stringResource(R.string.app_update_available_later),
                )
            }
            else -> Unit
        }
    }
}

private fun openStore(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        // Nothing on this phone can open it; the tap does nothing rather than crash the screen
        // the person is looking at.
    }
}
