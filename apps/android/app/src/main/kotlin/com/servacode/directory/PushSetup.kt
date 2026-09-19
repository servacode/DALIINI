package com.servacode.directory

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.servacode.directory.core.auth.SessionCoordinator
import com.servacode.directory.core.auth.SessionState
import com.servacode.directory.core.network.PushRegistrationCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connects Firebase Messaging to the backend registration, when the build is configured for it.
 *
 * Without a Firebase configuration — the normal case in development, and the case in this
 * repository, which carries none — nothing is initialised and the app runs with push off.
 * Delivery through FCM needs a real Firebase project and is verified separately.
 */
@Singleton
class PushSetup @Inject constructor(
    private val coordinator: PushRegistrationCoordinator,
    private val session: SessionCoordinator,
) {
    val configured: Boolean
        get() = listOf(
            BuildConfig.FIREBASE_PROJECT_ID,
            BuildConfig.FIREBASE_APPLICATION_ID,
            BuildConfig.FIREBASE_API_KEY,
            BuildConfig.FIREBASE_SENDER_ID,
        ).all { it.isNotBlank() }

    fun start(context: Context, scope: CoroutineScope) {
        if (!configured) return
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .setApplicationId(BuildConfig.FIREBASE_APPLICATION_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                    .build(),
            )
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            scope.launch { runCatching { coordinator.onTokenAvailable(token) } }
        }
        scope.launch {
            session.state.distinctUntilChanged().collect { state ->
                if (state == SessionState.SIGNED_IN) runCatching { coordinator.onSignedIn() }
            }
        }
    }
}
