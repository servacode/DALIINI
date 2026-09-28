package com.servacode.directory

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.MaintenanceScreen
import com.servacode.directory.core.network.MaintenanceCoordinator
import com.servacode.directory.core.network.MaintenanceStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Shows [content] — the navigation host — and covers it with the maintenance screen while
 * the backend reports maintenance.
 *
 * The host stays composed underneath, so the back stack and every screen's state survive;
 * when maintenance ends the user is where they were and the screens retry on their own
 * terms. Auto-retry runs only while the notice is visible and in the foreground
 * (collectAsStateWithLifecycle), so a backgrounded app does not keep probing.
 */
@Composable
fun MaintenanceGate(
    status: StateFlow<MaintenanceStatus>,
    coordinator: MaintenanceCoordinator,
    content: @Composable () -> Unit,
) {
    val current by status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var retrying by remember { mutableStateOf(false) }

    Box {
        content()
        val active = current as? MaintenanceStatus.Active
        if (active != null) {
            LaunchedEffect(Unit) { coordinator.autoRetry() }
            MaintenanceScreen(
                message = active.message,
                retrying = retrying,
                onRetry = {
                    if (retrying) return@MaintenanceScreen
                    retrying = true
                    scope.launch {
                        try {
                            coordinator.retryNow()
                        } finally {
                            retrying = false
                        }
                    }
                },
            )
        }
    }
}
