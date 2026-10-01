package com.servacode.directory.feature.duty

import com.servacode.directory.core.model.DutyShift
import com.servacode.directory.core.model.TemporaryClosure

/**
 * Why a shift cannot be scheduled, said before it is sent.
 *
 * The backend reports an overlap and an invalid range with one code on purpose (it will not say
 * whether another facility holds the slot), so the app checks what it can see itself — the
 * owner's own shifts and closures — and names the problem: which shift it overlaps, which closure
 * it falls in. The backend still decides; this only saves a round trip and a vague sentence.
 */
sealed interface DutyProblem {
    /** The end is not after the start. */
    data object InvalidRange : DutyProblem

    /** The whole shift is already over. */
    data object InPast : DutyProblem

    /** It overlaps one of the facility's own shifts. */
    data class Overlaps(val shift: DutyShift) : DutyProblem

    /** It falls inside one of the facility's temporary closures. */
    data class DuringClosure(val closure: TemporaryClosure) : DutyProblem
}

object DutyConflicts {
    fun check(
        startsAt: Long,
        endsAt: Long,
        shifts: List<DutyShift>,
        closures: List<TemporaryClosure>,
        now: Long,
    ): DutyProblem? {
        if (!DutyValidator.isValid(startsAt, endsAt)) return DutyProblem.InvalidRange
        if (endsAt <= now) return DutyProblem.InPast
        shifts.firstOrNull { overlaps(startsAt, endsAt, it.startsAtEpochMillis, it.endsAtEpochMillis) }
            ?.let { return DutyProblem.Overlaps(it) }
        closures.firstOrNull { overlaps(startsAt, endsAt, it.startsAtEpochMillis, it.endsAtEpochMillis) }
            ?.let { return DutyProblem.DuringClosure(it) }
        return null
    }

    /** Half-open windows: a shift that starts when another ends does not overlap it. */
    private fun overlaps(start: Long, end: Long, otherStart: Long, otherEnd: Long): Boolean =
        start < otherEnd && otherStart < end
}
