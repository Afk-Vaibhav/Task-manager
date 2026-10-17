package com.localtaskmanager.app

import com.localtaskmanager.app.domain.model.RecurrenceFrequency
import com.localtaskmanager.app.domain.model.RecurrenceSeries
import com.localtaskmanager.app.domain.recurrence.RecurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceCalculatorTest {

    @Test
    fun testDailyRecurrence() {
        val series = RecurrenceSeries(
            id = "test_series_daily",
            frequency = RecurrenceFrequency.DAILY,
            interval = 1,
            startDate = LocalDate.of(2026, 10, 16),
            templateTitle = "Daily Standup"
        )

        val next = RecurrenceCalculator.calculateNextOccurrenceDate(
            series = series,
            currentDate = LocalDate.of(2026, 10, 16)
        )
        assertEquals(LocalDate.of(2026, 10, 17), next)
    }

    @Test
    fun testWeekdaysRecurrence_skipsWeekend() {
        val series = RecurrenceSeries(
            id = "test_series_weekdays",
            frequency = RecurrenceFrequency.WEEKDAYS,
            interval = 1,
            selectedDaysOfWeek = listOf(1, 2, 3, 4, 5), // Mon-Fri
            startDate = LocalDate.of(2026, 10, 16), // Friday
            templateTitle = "Work Review"
        )

        // Friday Oct 16 -> should advance to Monday Oct 19
        val next = RecurrenceCalculator.calculateNextOccurrenceDate(
            series = series,
            currentDate = LocalDate.of(2026, 10, 16)
        )
        assertEquals(LocalDate.of(2026, 10, 19), next)
    }

    @Test
    fun testWeeklyRecurrence() {
        val series = RecurrenceSeries(
            id = "test_series_weekly",
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 1,
            startDate = LocalDate.of(2026, 10, 16),
            templateTitle = "Weekly Clean"
        )

        val next = RecurrenceCalculator.calculateNextOccurrenceDate(
            series = series,
            currentDate = LocalDate.of(2026, 10, 16)
        )
        assertEquals(LocalDate.of(2026, 10, 23), next)
    }

    @Test
    fun testMonthlyRecurrenceClamping_leapYear() {
        // Test Jan 31 in leap year 2024 -> Feb 29
        val series = RecurrenceSeries(
            id = "test_series_monthly",
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 1,
            dayOfMonth = 31,
            startDate = LocalDate.of(2024, 1, 31),
            templateTitle = "Monthly Invoice"
        )

        val nextLeap = RecurrenceCalculator.calculateNextOccurrenceDate(
            series = series,
            currentDate = LocalDate.of(2024, 1, 31)
        )
        assertEquals(LocalDate.of(2024, 2, 29), nextLeap)

        // Test Jan 31 in non-leap year 2025 -> Feb 28
        val nextNonLeap = RecurrenceCalculator.calculateNextOccurrenceDate(
            series = series,
            currentDate = LocalDate.of(2025, 1, 31)
        )
        assertEquals(LocalDate.of(2025, 2, 28), nextNonLeap)
    }

    @Test
    fun testRecurrenceEndDateRespected() {
        val series = RecurrenceSeries(
            id = "test_series_ended",
            frequency = RecurrenceFrequency.DAILY,
            interval = 1,
            startDate = LocalDate.of(2026, 10, 15),
            endDate = LocalDate.of(2026, 10, 16),
            templateTitle = "Short Project"
        )

        val next = RecurrenceCalculator.calculateNextOccurrenceDate(
            series = series,
            currentDate = LocalDate.of(2026, 10, 16)
        )
        // Next would be Oct 17, which is after Oct 16 -> should return null
        assertNull(next)
    }
}
