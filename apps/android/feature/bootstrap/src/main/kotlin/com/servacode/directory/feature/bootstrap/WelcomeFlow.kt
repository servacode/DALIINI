package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.datastore.DirectoryPreferences
import com.servacode.directory.core.datastore.LocationPreference

/** Where a start goes once the device's preferences are read. */
enum class StartDestination { WELCOME, HOME }

/**
 * The welcome and the location question are a first run only.
 *
 * A device that has been through them says so. A device that already carries a chosen province
 * was here before the welcome existed, so it is not sent back through it: a redesign does not
 * restart anyone's onboarding.
 */
fun DirectoryPreferences.startDestination(): StartDestination = when {
    welcomeCompleted -> StartDestination.HOME
    selectedProvinceId != null -> StartDestination.HOME
    else -> StartDestination.WELCOME
}

/** What the user did with the location question. */
enum class LocationAnswer { ALLOWED, REFUSED, LATER }

/**
 * What the answer is remembered as. Either way the app carries on with the chosen province; a
 * refusal costs the distances and the nearest-first order, nothing else. "Not now" stays open,
 * so the offer on Home still reads as an offer, and this screen does not ask a second time.
 */
fun LocationAnswer.preference(): LocationPreference = when (this) {
    LocationAnswer.ALLOWED -> LocationPreference.ENABLED
    LocationAnswer.REFUSED -> LocationPreference.DISABLED
    LocationAnswer.LATER -> LocationPreference.ASK
}

/** The words of the first run, in one place, provisional until product copy is approved. */
object WelcomeCopy {
    const val TITLE = "دليل الخدمات الصحية في محافظتك"
    const val BODY = "اعرف الصيدليات والعيادات القريبة منك، ومَن منها مفتوح أو مناوب الآن."
    const val CONTINUE = "ابدأ الآن"
}

/** The words of the location question, provisional until product copy is approved. */
object LocationCopy {
    const val TITLE = "الأذونات المطلوبة"
    const val NOTE = "كلاهما اختياري، ويمكنك المتابعة بدونهما ضمن محافظتك."
    val REASONS = listOf(
        "معرفة أقرب الخدمات إليك",
        "عرض المسافة التقريبية",
        "تحسين نتائج الخريطة",
        "تنبيهك بالمناوبة وبما يخص منشأتك",
    )
    const val ALLOW = "السماح"
    const val LATER = "ليس الآن"
}
