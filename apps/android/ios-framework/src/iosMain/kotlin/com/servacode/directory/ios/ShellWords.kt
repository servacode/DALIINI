package com.servacode.directory.ios

import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.AppErrorMessage
import com.servacode.directory.core.model.AppErrorMessages
import platform.Foundation.NSBundle

/**
 * The shell's words, by key. The sentences live in the app's own strings file
 * (apps/ios/Daliini/ar.lproj/Shell.strings), as Android's live in its resources, so a second
 * language is a second file and no Kotlin changes.
 */
enum class ShellWord(val key: String) {
    APP_NAME("app_name"),
    CHOOSE_PROVINCE("province_title"),
    CHANGE_PROVINCE("home_province_choose"),
    STALE_LIST("province_stale"),
    STALE_HOME("shell_home_stale"),
    LOADING("shell_loading"),
    RETRY("ds_retry"),
    DUTY_NOW("widget_duty_title"),
    OPEN_NOW("shell_open_now"),
    NEARBY("home_chip_nearest"),
    CATEGORIES("shell_categories"),
    ERROR_OFFLINE("ds_error_offline"),
    ERROR_MAINTENANCE("ds_error_maintenance"),
    ERROR_SERVER("ds_error_server"),
    ERROR_NOT_FOUND("ds_error_not_found"),
    ERROR_THROTTLED("ds_error_throttled"),
    ERROR_UNEXPECTED("ds_error_unexpected"),
}

internal const val WORDS_TABLE = "Shell"

/** The sentence for [word] in [bundle]; the key itself if the bundle has none. */
internal fun ShellWord.text(bundle: NSBundle = NSBundle.mainBundle): String =
    bundle.localizedStringForKey(key, value = key, table = WORDS_TABLE)

/** The keys [bundle] has no sentence for. The app's own tests expect none. */
fun missingShellWords(bundle: NSBundle): List<String> =
    ShellWord.entries.filter { it.text(bundle) == it.key }.map { it.key }

/**
 * Which word a failure gets: the shared decision (AppErrorMessages), in the words Android's design
 * system uses for the same messages. Only those a public read can meet have their own.
 */
internal fun failureWord(error: AppError): ShellWord = when (AppErrorMessages.of(error)) {
    AppErrorMessage.OFFLINE -> ShellWord.ERROR_OFFLINE
    AppErrorMessage.MAINTENANCE -> ShellWord.ERROR_MAINTENANCE
    AppErrorMessage.SERVER -> ShellWord.ERROR_SERVER
    AppErrorMessage.NOT_FOUND -> ShellWord.ERROR_NOT_FOUND
    AppErrorMessage.THROTTLED -> ShellWord.ERROR_THROTTLED
    else -> ShellWord.ERROR_UNEXPECTED
}
