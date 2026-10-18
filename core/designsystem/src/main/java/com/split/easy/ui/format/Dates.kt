package com.split.easy.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// The app's text is English; Locale.US keeps "Sep" (other English locales write "Sept").
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.US)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

private val WEEKDAY_DAY_MONTH = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)
private val WEEKDAY_DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.US)

/** Feed day headers: "Sat, 10 Oct" this year, "Sat, 10 Oct 2025" otherwise (shown in capitals). */
fun formatDayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = if (date.year ==
    today.year
) {
    WEEKDAY_DAY_MONTH.format(date)
} else {
    WEEKDAY_DAY_MONTH_YEAR.format(date)
}

/** "2 Sep" this year, "2 Sep 2025" otherwise. */
fun formatShortDate(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    if (date.year == today.year) DAY_MONTH.format(date) else DAY_MONTH_YEAR.format(date)

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

/** "9:41 pm" from epoch milliseconds, in the phone's time zone. */
fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    TIME.format(Instant.ofEpochMilli(epochMillis).atZone(zone)).lowercase(Locale.US)
