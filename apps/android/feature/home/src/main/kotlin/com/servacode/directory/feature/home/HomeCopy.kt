package com.servacode.directory.feature.home

/**
 * Everything Home says, in one place.
 *
 * Provisional until product copy is approved, like every other `…Copy` in the app: keeping the
 * words out of the layouts is what lets approved wording be dropped in without anyone opening
 * a composable.
 */
object HomeCopy {
    const val TITLE = "دليلك"
    const val YOU_ARE_IN = "أنت الآن في"
    const val NOTIFICATIONS = "الإشعارات"
    const val MANY = "99+"
    const val SEARCH = "ابحث عن صيدلية، طبيب أو خدمة صحية…"
    const val FILTERS = "تصفية"
    const val LOADING_MORE = "جارٍ تحميل المزيد…"

    const val ERROR = "تعذر تحميل الصفحة"
    const val PROVINCE_REQUIRED = "اختر محافظتك"
    const val PROVINCE_REQUIRED_BODY = "نعرض المنشآت القريبة منك داخل محافظتك."
    const val PROVINCE_CHOOSE = "اختيار المحافظة"

    const val LOCATION_TITLE = "اسمح بالوصول إلى موقعك"
    const val LOCATION_BODY = "لنعرض لك الأقرب إليك ونحسب المسافات."
    const val LOCATION_ACTION = "السماح بالموقع"

    fun chip(chip: HomeChip): String = when (chip) {
        HomeChip.NEAREST -> "الأقرب إليك"
        HomeChip.OPEN_NOW -> "مفتوح الآن"
        HomeChip.DUTY_TODAY -> "مناوبة اليوم"
    }


    /**
     * Why the list is empty, in terms of what was asked.
     *
     * "No results" would leave the reader to work out which of their choices produced none, and
     * the combinations are exactly where that is hardest to guess.
     */
    fun emptyFor(filters: HomeFilters): String = when {
        filters.openNow && filters.dutyToday -> "لا توجد منشآت مناوبة اليوم ومفتوحة الآن"
        filters.dutyToday -> "لا توجد منشآت مناوبة اليوم"
        filters.openNow -> "لا توجد منشآت مفتوحة حالياً"
        else -> "لا توجد منشآت في هذا التصنيف"
    }
}
