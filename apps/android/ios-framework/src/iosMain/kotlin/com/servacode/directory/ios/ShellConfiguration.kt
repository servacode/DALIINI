package com.servacode.directory.ios

import platform.Foundation.NSBundle

/**
 * What a build of the iPhone app is pointed at, read from its Info.plist, where Xcode writes the
 * configuration's values (the xcconfig files in apps/ios/Config). A release built without a real
 * address keeps the placeholder, and the app says it is not configured rather than guessing.
 */
data class ShellConfiguration(
    val apiBaseUrl: String,
    val allowCleartext: Boolean,
) {
    companion object {
        fun fromBundle(bundle: NSBundle = NSBundle.mainBundle): ShellConfiguration = ShellConfiguration(
            apiBaseUrl = bundle.objectForInfoDictionaryKey("DaliiniApiBaseUrl") as? String ?: "",
            allowCleartext = (bundle.objectForInfoDictionaryKey("DaliiniAllowCleartext") as? String)
                ?.trim()?.equals("YES", ignoreCase = true) == true,
        )
    }
}
