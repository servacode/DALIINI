package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.model.AppError

/** The basic-information fields a backend refusal can point at. */
enum class OnboardingField(val wire: String) {
    NAME_AR("nameAr"),
    PHONE("phone"),
    WHATSAPP("whatsapp"),
    ADDRESS_AR("addressAr"),
    ;

    companion object {
        /**
         * The fields [error] names in its `details`. The backend's own messages are English and
         * technical, so the screen shows its own sentence under each named field instead.
         */
        fun named(error: AppError?): Set<OnboardingField> {
            val keys = error?.fieldErrors?.keys ?: return emptySet()
            return entries.filterTo(mutableSetOf()) { it.wire in keys }
        }
    }
}
