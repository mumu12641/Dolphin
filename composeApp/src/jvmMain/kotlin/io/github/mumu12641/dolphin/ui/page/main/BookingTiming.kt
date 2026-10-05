package io.github.mumu12641.dolphin.ui.page.main

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun targetBookingDate(now: LocalDateTime, executionTime: String): LocalDate {
    val scheduled = runCatching { LocalTime.parse(executionTime) }
        .getOrDefault(LocalTime.of(8, 0, 3))
    val executionDay = if (now.toLocalTime().isBefore(scheduled)) {
        now.toLocalDate()
    } else {
        now.toLocalDate().plusDays(1)
    }
    return executionDay.plusDays(2)
}

internal fun formatBookingDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA))

internal fun compactTimeSlot(slot: String): String {
    val parts = slot.split("-")
    if (parts.size != 2) return slot
    return "${parts[0].substringBeforeLast(':')}—${parts[1].substringBeforeLast(':')}"
}
