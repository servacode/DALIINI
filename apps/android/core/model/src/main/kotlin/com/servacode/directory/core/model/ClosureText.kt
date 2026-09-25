package com.servacode.directory.core.model

/**
 * What a temporary closure shows besides its period (INT-095).
 *
 * A reason of spaces is not a reason. Dropping it here, rather than where the line is drawn, keeps
 * the rule testable and stops a stray separator with nothing after it appearing on the owner's
 * page. How the reason and the period are joined is a sentence, so it is in the design system's
 * resources and `closureText` reads it.
 */
fun TemporaryClosure.shownReason(): String? = reason?.trim()?.takeIf { it.isNotEmpty() }
