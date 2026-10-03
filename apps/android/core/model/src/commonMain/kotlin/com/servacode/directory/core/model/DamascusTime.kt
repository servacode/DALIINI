package com.servacode.directory.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The one boundary between what an owner picks, a date and a time on Damascus clocks, and what
 * the API carries, an instant as epoch milliseconds. Nothing else converts between the two.
 *
 * Syria has kept UTC+3 all year since late 2022; before that it observed summer time. The zone
 * rules decide, not a fixed offset. A time that did not exist on the local clock (the hour
 * skipped in spring) moves forward by the gap; a time that occurred twice (the hour repeated in
 * autumn) means its first occurrence.
 *
 * Shared with the iPhone app (DECISION-086), so it is written on kotlinx-datetime, which reads
 * the same zone rules from the platform on both.
 */
object DamascusTime {
    val ZONE: TimeZone = TimeZone.of("Asia/Damascus")

    fun toEpochMillis(date: LocalDate, time: LocalTime): Long =
        LocalDateTime(date, time).toInstant(ZONE).toEpochMilliseconds()

    fun localDateTime(epochMillis: Long): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(ZONE)

    fun now(): LocalDateTime = Clock.System.now().toLocalDateTime(ZONE)

    /** "2026-09-20 08:00", Latin digits, Damascus clock. */
    fun format(epochMillis: Long): String = localDateTime(epochMillis).let { "${it.date} ${clockOf(it)}" }

    /** "08:00", the wall clock alone, for a line that already says which day. */
    fun clock(epochMillis: Long): String = clockOf(localDateTime(epochMillis))

    /** Material's date picker reports the chosen day as UTC midnight; this is that day. */
    fun dateFromPicker(utcMidnightMillis: Long): LocalDate =
        Instant.fromEpochMilliseconds(utcMidnightMillis).toLocalDateTime(TimeZone.UTC).date

    /** The inverse, to open the date picker on a given day. */
    fun pickerMillis(date: LocalDate): Long = date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    private fun clockOf(time: LocalDateTime): String =
        "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
