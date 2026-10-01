package com.servacode.directory

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.UpdateRequiredScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Shows [content] and covers it when the backend says this build is too old.
 *
 * Built like [MaintenanceGate]: the host stays composed underneath, so nothing is torn down.
 * The difference is that this one has no way back. Maintenance ends by itself and the notice
 * lifts; a build below the minimum stays below it until a newer one is installed, so there is
 * no retry here — only the way out.
 */
@Composable
fun UpdateGate(
    verdict: StateFlow<VersionCheck.Verdict>,
    versionCode: Int,
    check: VersionCheck,
    content: @Composable () -> Unit,
) {
    val current by verdict.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Once per process: the answer does not change while the app is open, and a build cannot
    // become acceptable without being replaced.
    LaunchedEffect(Unit) { check.refresh(versionCode) }

    Box {
        content()
        val blocked = current as? VersionCheck.Verdict.TooOld
        if (blocked != null) {
            UpdateRequiredScreen(
                message = blocked.notice,
                onOpenStore = blocked.storeUrl?.let { url ->
                    {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // Nothing on this phone can open it; the tap does nothing rather
                            // than crash the one screen the person can still see.
                        }
                    }
                },
            )
        }
    }
}
