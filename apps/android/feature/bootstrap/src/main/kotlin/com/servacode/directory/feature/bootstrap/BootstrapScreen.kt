package com.servacode.directory.feature.bootstrap

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryPage

@Composable
fun BootstrapScreen(
    onReady: (StartDestination) -> Unit,
    viewModel: BootstrapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The splash stays while the app starts and while it hands over, so the first screen fades
    // in from the splash rather than from an empty window. It never holds navigation back.
    if (state !is BootstrapUiState.Error) DirectorySplashScreen()
    when (val value = state) {
        BootstrapUiState.Loading -> Unit
        is BootstrapUiState.Ready -> LaunchedEffect(value.start) {
            onReady(value.start)
        }
        // The one failure the splash gives way to: the app could not read what it starts from.
        is BootstrapUiState.Error -> DirectoryPage { padding ->
            DirectoryErrorState(
                title = BootstrapCopy.ERROR,
                modifier = Modifier.padding(padding),
                body = BootstrapCopy.ERROR_BODY,
            )
        }
    }
}

/** The words of the start, provisional until product copy is approved. */
object BootstrapCopy {
    const val ERROR = "تعذر بدء التطبيق"
    const val ERROR_BODY = "أغلق التطبيق وافتحه مرة أخرى."
}
