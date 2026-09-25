package com.servacode.directory.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The one boundary between what an owner picks, a date and a time on Damascus clocks, and what
 * the API carries, an instant as epoch milliseconds. Nothing else converts between the two.
 *
 * Syria has kept UTC+3 all year since late 2022; before that it observed summer time. The zone
 * rules decide, not a fixed offset. A time that did not exist on the local clock (the hour
 * skipped in spring) moves forward by the gap; a time that occurred twice (the hour repeated in
 * autumn) means its first occurrence.
 */
object DamascusTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Damascus")
    private val DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
    private val CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    fun toEpochMillis(date: LocalDate, time: LocalTime): Long =
        ZonedDateTime.of(date, time, ZONE).toInstant().toEpochMilli()

    fun localDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDateTime()

    fun now(): LocalDateTime = LocalDateTime.now(ZONE)

    /** "2026-09-20 08:00", Latin digits, Damascus clock. */
    fun format(epochMillis: Long): String = localDateTime(epochMillis).format(DISPLAY)

    /** "08:00", the wall clock alone, for a line that already says which day. */
    fun clock(epochMillis: Long): String = localDateTime(epochMillis).format(CLOCK)

    /** Material's date picker reports the chosen day as UTC midnight; this is that day. */
    fun dateFromPicker(utcMidnightMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMidnightMillis).atZone(ZoneOffset.UTC).toLocalDate()

    /** The inverse, to open the date picker on a given day. */
    fun pickerMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
