package com.servacode.directory.core.network

/** Whether this build can receive push at all: false when it carries no Firebase configuration. */
interface PushAvailability {
    val enabled: Boolean
}

/**
 * When to ask for the notification permission, which is a runtime permission from Android 13.
 *
 * Only at a moment where a notice has an obvious purpose — an owner has just sent a facility
 * for review, and the decision arrives as a push — and only when the build can receive push:
 * asking for a permission the app cannot use would be noise. Refusing blocks nothing; the
 * decision still reaches the owner in the app. Android itself stops showing the dialog after
 * the user has refused twice.
 */
object NotificationPermissionPolicy {
    /** Android 13, `TIRAMISU`. Below it the permission is granted at install. */
    const val RUNTIME_PERMISSION_SDK = 33

    fun shouldRequest(sdkInt: Int, granted: Boolean, pushEnabled: Boolean): Boolean =
        sdkInt >= RUNTIME_PERMISSION_SDK && !granted && pushEnabled
}
