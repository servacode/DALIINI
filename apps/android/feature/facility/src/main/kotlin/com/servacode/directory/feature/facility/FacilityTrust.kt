package com.servacode.directory.feature.facility

/**
 * How long ago something happened, in the unit a person would say it in.
 *
 * The trust line reads "تم التحقق قبل ٣ أيام", not a date: what the reader weighs is how fresh
 * the check is, and a calendar date makes them do the subtraction.
 */
sealed interface FacilityAge {
    data object Today : FacilityAge
    data class Days(val count: Int) : FacilityAge
    data class Months(val count: Int) : FacilityAge
    data class Years(val count: Int) : FacilityAge

    companion object {
        private const val DAY_MS = 86_400_000L

        /**
         * The age of [thenMillis] at [nowMillis]. A time in the future — the device clock behind
         * the server's — reads as today rather than as a negative age.
         */
        fun of(thenMillis: Long, nowMillis: Long): FacilityAge {
            val days = ((nowMillis - thenMillis) / DAY_MS).coerceAtLeast(0)
            return when {
                days < 1 -> Today
                days < 30 -> Days(days.toInt())
                days < 365 -> Months((days / 30).toInt())
                else -> Years((days / 365).toInt())
            }
        }
    }
}

/**
 * The WhatsApp chat link for a published number, or null when it has too few digits to be one.
 *
 * wa.me wants the international number as digits alone. With WhatsApp installed the link opens
 * there; without it the same https address opens WhatsApp's page in the browser, which says so.
 */
fun whatsAppLink(number: String): String? {
    val digits = number.filter { it in '0'..'9' }
    return if (digits.length < MIN_WHATSAPP_DIGITS) null else "https://wa.me/$digits"
}

private const val MIN_WHATSAPP_DIGITS = 8
