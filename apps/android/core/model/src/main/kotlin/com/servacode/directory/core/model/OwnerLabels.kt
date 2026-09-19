package com.servacode.directory.core.model

/**
 * The words a user reads for the owner domain's values. The values stay as the backend and
 * the domain define them; only what reaches the screen is translated, here and nowhere else.
 * A code this app does not know yet gets a neutral label, never the code itself.
 */
object OwnerLabels {
    fun status(value: OwnerFacilityStatus): String = when (value) {
        OwnerFacilityStatus.DRAFT -> "مسودة"
        OwnerFacilityStatus.SUBMITTED -> "قيد المراجعة"
        OwnerFacilityStatus.ACTIVE -> "فعّالة"
        OwnerFacilityStatus.REVERIFICATION_REQUIRED -> "بحاجة إلى إعادة التحقق"
        OwnerFacilityStatus.SUSPENDED -> "موقوفة"
        OwnerFacilityStatus.CLOSED -> "مغلقة"
    }

    /** The action the backend asks of the owner; it arrives as a code the contract lists. */
    fun requiredAction(code: String): String = when (code) {
        "REVIEW_REJECTION" -> "راجِع سبب الرفض وعدّل الطلب"
        "COMPLETE_AND_SUBMIT" -> "أكمِل البيانات وأرسلها للمراجعة"
        "REVERIFY_AND_SUBMIT" -> "حدّث البيانات وأرسلها لإعادة التحقق"
        "WAIT_FOR_REVIEW" -> "بانتظار المراجعة"
        "CONTACT_SUPPORT" -> "تواصل مع الدعم"
        else -> UNKNOWN_ACTION
    }

    fun role(value: FacilityMemberRole): String = when (value) {
        FacilityMemberRole.OWNER -> "مالك"
        FacilityMemberRole.MANAGER -> "مدير"
    }

    const val UNKNOWN_ACTION = "يلزم إجراء، راجِع تفاصيل المنشأة"
}
