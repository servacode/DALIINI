package com.servacode.directory.feature.auth

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.servacode.directory.core.datastore.DirectoryPreferencesStore
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.network.PublicApiBoundary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltLoginViewModel @Inject constructor(
    auth: AuthRepository,
) : LoginViewModel(auth)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltRegisterViewModel @Inject constructor(
    auth: AuthRepository,
    publicApi: PublicApiBoundary,
    preferences: DirectoryPreferencesStore,
    location: LocationProvider,
) : RegisterViewModel(auth, publicApi, preferences, location)

/** The shared view model as Hilt builds it for Android's navigation. */
@HiltViewModel
class HiltRecoveryViewModel @Inject constructor(
    auth: AuthRepository,
) : RecoveryViewModel(auth)

/** `LoginScreen` as the app's navigation opens it. */
@Composable
fun LoginRoute(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onRecovery: () -> Unit,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    viewModel: HiltLoginViewModel = hiltViewModel(),
) {
    LoginScreen(
        viewModel = viewModel,
        onSignedIn = onSignedIn,
        onRegister = onRegister,
        onRecovery = onRecovery,
        onBack = onBack,
        bottomBar = bottomBar,
    )
}

/** `RegisterScreen` as the app's navigation opens it. */
@Composable
fun RegisterRoute(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit = onBack,
    viewModel: HiltRegisterViewModel = hiltViewModel(),
) {
    RegisterScreen(
        viewModel = viewModel,
        onRegistered = onRegistered,
        onBack = onBack,
        onSignIn = onSignIn,
    )
}

/** `RecoveryScreen` as the app's navigation opens it. */
@Composable
fun RecoveryRoute(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: HiltRecoveryViewModel = hiltViewModel(),
) {
    RecoveryScreen(
        viewModel = viewModel,
        onDone = onDone,
        onBack = onBack,
    )
}
