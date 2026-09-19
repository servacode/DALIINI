package com.servacode.directory.core.model

/** How a temporary closure reads in a list (INT-095). */
object ClosureText {
    /** "<reason> • <period>", or the period alone when there is no reason to show. */
    fun of(closure: TemporaryClosure): String {
        val period = DamascusTime.period(closure.startsAtEpochMillis, closure.endsAtEpochMillis)
        val reason = closure.reason?.trim().orEmpty()
        return if (reason.isEmpty()) period else "$reason • $period"
    }
}
