package com.servacode.directory.ios

import platform.Foundation.NSBundle

/**
 * The shell's own words, by key. The sentences live in the app's strings file
 * (apps/ios/Daliini/ar.lproj/Shell.strings), as Android's live in its resources; what the design
 * system says itself (a retry, an error's sentence, the offline notice) is its own (DECISION-094).
 */
enum class ShellWord(val key: String) {
    APP_NAME("app_name"),
    CHANGE_PROVINCE("home_province_choose"),
    LOAD_FAILED("home_error"),
    STALE_HOME("shell_home_stale"),
    DUTY_NOW("widget_duty_title"),
    OPEN_NOW("shell_open_now"),
    NEARBY("home_chip_nearest"),
    CATEGORIES("shell_categories"),
}

internal const val WORDS_TABLE = "Shell"

/** The sentence for [this] in [bundle]; the key itself if the bundle has none. */
internal fun ShellWord.text(bundle: NSBundle = NSBundle.mainBundle): String =
    bundle.localizedStringForKey(key, value = key, table = WORDS_TABLE)

/** The keys [bundle] has no sentence for. The app's own tests expect none. */
fun missingShellWords(bundle: NSBundle): List<String> =
    ShellWord.entries.filter { it.text(bundle) == it.key }.map { it.key }
