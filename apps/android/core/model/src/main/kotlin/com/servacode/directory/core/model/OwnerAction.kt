package com.servacode.directory.core.model

/**
 * What the backend asks of the owner, named without saying it.
 *
 * It arrives as a code the contract lists. A code this app has not met yet is [UNKNOWN] rather
 * than the code itself: an owner shown `REVERIFY_AND_SUBMIT` learns nothing, and is not told
 * that the app is out of date either.
 *
 * The words are in the design system's resources, read by `OwnerWords.requiredAction`. The
 * statuses and the roles need nothing here: they are already enumerated by the domain, so the
 * design system maps them to sentences directly.
 */
enum class OwnerAction {
    REVIEW_REJECTION,
    COMPLETE_AND_SUBMIT,
    REVERIFY_AND_SUBMIT,
    WAIT_FOR_REVIEW,
    CONTACT_SUPPORT,
    UNKNOWN,
}

fun ownerAction(code: String): OwnerAction = when (code) {
    "REVIEW_REJECTION" -> OwnerAction.REVIEW_REJECTION
    "COMPLETE_AND_SUBMIT" -> OwnerAction.COMPLETE_AND_SUBMIT
    "REVERIFY_AND_SUBMIT" -> OwnerAction.REVERIFY_AND_SUBMIT
    "WAIT_FOR_REVIEW" -> OwnerAction.WAIT_FOR_REVIEW
    "CONTACT_SUPPORT" -> OwnerAction.CONTACT_SUPPORT
    else -> OwnerAction.UNKNOWN
}
