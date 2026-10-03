package com.marsa.absen.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDateTime

private val ID = Locale.forLanguageTag("id-ID")

/** Server sering mengisi kolom kosong dengan "00:00:00"; itu dianggap belum ada. */
fun String?.cleanTime(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("00:00:00") }

fun String?.cleanDate(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.startsWith("0000") }

fun String?.toLocalTimeOrNull(): LocalTime? =
    cleanTime()?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

/** "06:50:00" -> "06:50"; kosong -> "--:--" */
fun String?.toHhmm(): String = cleanTime()?.take(5) ?: "--:--"

fun lateMinutes(actual: String?, schedule: String?): Long {
    val a = actual.toLocalTimeOrNull() ?: return 0
    val s = schedule.toLocalTimeOrNull() ?: return 0
    return Duration.between(s, a).toMinutes().coerceAtLeast(0)
}

fun greeting(): String = when (LocalTime.now().hour) {
    in 0..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..17 -> "Selamat sore"
    else -> "Selamat malam"
}

fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", ID))

fun todayIso(): String = LocalDate.now().toString() // yyyy-MM-dd

fun String?.initials(): String {
    val parts = this?.trim()?.split(" ")?.filter { it.isNotBlank() }.orEmpty()
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
/** "2026-10-03 18:19:00" -> "Sab, 3 Okt 18:19" */
fun String?.toDateTimeLabel(): String {
    val s = this?.trim()?.takeIf { it.isNotEmpty() } ?: return "-"
    val t = runCatching { LocalDateTime.parse(s.replace(' ', 'T')) }.getOrNull() ?: return "-"
    return t.format(DateTimeFormatter.ofPattern("EEE, d MMM HH:mm", ID))
}
