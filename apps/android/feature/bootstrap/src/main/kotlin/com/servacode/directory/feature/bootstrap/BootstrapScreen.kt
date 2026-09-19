package com.servacode.directory.feature.bootstrap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        is BootstrapUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("تعذر بدء التطبيق")
        }
    }
}
