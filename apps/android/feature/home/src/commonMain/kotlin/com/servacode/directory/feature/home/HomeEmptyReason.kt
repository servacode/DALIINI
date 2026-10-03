package com.servacode.directory.feature.home

/**
 * Why Home's list is empty, decided without naming a single word.
 *
 * The words moved to the module's resources so that a second language is a second file; the
 * decision stayed here so that it can still be compiled and tested without Android — and so
 * that a test asserts which question went unanswered rather than asserting Arabic prose, which
 * breaks every time the wording is improved.
 */
internal enum class HomeEmptyReason {
    /**
     * A specialty or a service was chosen, whatever else was: that choice is the one the reader
     * can undo, and the empty list offers to undo it.
     */
    CHOICE,
    DUTY_AND_OPEN,
    DUTY,
    OPEN,
    CATEGORY,
}

internal fun HomeFilters.emptyReason(): HomeEmptyReason = when {
    hasTags -> HomeEmptyReason.CHOICE
    openNow && dutyToday -> HomeEmptyReason.DUTY_AND_OPEN
    dutyToday -> HomeEmptyReason.DUTY
    openNow -> HomeEmptyReason.OPEN
    else -> HomeEmptyReason.CATEGORY
}
