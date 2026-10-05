package io.github.mumu12641.dolphin.ui.page.main

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class BookingTimingTest {
    @Test
    fun beforeExecutionTimeTargetsTwoDaysFromToday() {
        assertEquals(
            LocalDate.of(2026, 10, 1),
            targetBookingDate(LocalDateTime.of(2026, 9, 29, 8, 0, 0), "08:00:01")
        )
    }

    @Test
    fun atOrAfterExecutionTimeTargetsTwoDaysFromNextExecutionDay() {
        assertEquals(
            LocalDate.of(2026, 10, 2),
            targetBookingDate(LocalDateTime.of(2026, 9, 29, 8, 0, 1), "08:00:01")
        )
        assertEquals(
            LocalDate.of(2026, 10, 2),
            targetBookingDate(LocalDateTime.of(2026, 9, 29, 15, 0), "08:00:01")
        )
    }

    @Test
    fun invalidStoredTimeFallsBackToEightOhThree() {
        assertEquals(
            LocalDate.of(2026, 10, 1),
            targetBookingDate(LocalDateTime.of(2026, 9, 29, 8, 0, 2), "invalid")
        )
        assertEquals(
            LocalDate.of(2026, 10, 2),
            targetBookingDate(LocalDateTime.of(2026, 9, 29, 8, 0, 3), "invalid")
        )
    }

    @Test
    fun formatsConcreteChineseDate() {
        assertEquals("2026年10月1日 星期四", formatBookingDate(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun formatsTimeSlotsWithOneOrTwoDigitHours() {
        assertEquals("8:00—10:00", compactTimeSlot("8:00:00-10:00:00"))
        assertEquals("18:00—20:00", compactTimeSlot("18:00:00-20:00:00"))
    }
}
