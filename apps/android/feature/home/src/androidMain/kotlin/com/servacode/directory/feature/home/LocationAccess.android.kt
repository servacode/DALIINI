package com.servacode.directory.feature.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.servacode.directory.core.location.FOREGROUND_LOCATION_PERMISSIONS

/** Android's runtime permission: either location permission counts, as before. */
@Composable
internal actual fun rememberLocationAccess(onAnswer: (allowed: Boolean) -> Unit): LocationAccess {
    val context = LocalContext.current
    val answer by rememberUpdatedState(onAnswer)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> answer(result.values.any { it }) }
    return remember(context, launcher) {
        object : LocationAccess {
            override val allowed: Boolean
                get() = FOREGROUND_LOCATION_PERMISSIONS.any {
                    context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
                }

            override fun ask() = launcher.launch(FOREGROUND_LOCATION_PERMISSIONS.toTypedArray())
        }
    }
}
