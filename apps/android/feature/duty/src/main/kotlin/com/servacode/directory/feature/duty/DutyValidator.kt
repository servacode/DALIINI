package com.servacode.directory.feature.duty

object DutyValidator {
    fun isValid(startsAtEpochMillis: Long, endsAtEpochMillis: Long): Boolean =
        startsAtEpochMillis > 0L && endsAtEpochMillis > startsAtEpochMillis
}
