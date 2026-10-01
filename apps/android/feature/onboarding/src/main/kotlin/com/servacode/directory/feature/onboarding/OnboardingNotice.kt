package com.servacode.directory.feature.onboarding

import com.servacode.directory.core.model.AppError

/**
 * What the owner is told after an action, named rather than worded.
 *
 * The view model decides that a draft was saved or that an upload failed; the screen decides how
 * to say it, reading the sentence from the module's `strings.xml`. That split is why the same
 * screen in a second language is a second file, and why a view model — which has no composition
 * to read a resource in, and no business knowing the reader's language — holds no prose.
 */
enum class OnboardingNotice {
    DRAFT_SAVED,
    FILE_UPLOADED,
    SUBMITTED,
    LOCATION_PERMISSION,
    LOCATION_UNAVAILABLE,
    IMAGE_UNREADABLE,
    DRAFT_SAVE_FAILED,
    LOCATION_SAVE_FAILED,
    HOURS_SAVE_FAILED,
    UPLOAD_FAILED,
    SUBMIT_FAILED,
}

/**
 * A notice, and the error behind it where there is one.
 *
 * The failures show the error's own sentence as well as their own, because "could not save" alone
 * does not tell an owner whether to try again or to change something.
 */
data class OnboardingMessage(val notice: OnboardingNotice, val error: AppError? = null)
