package com.localtaskmanager.app.domain.recurrence

import com.localtaskmanager.app.domain.model.RecurrenceFrequency
import com.localtaskmanager.app.domain.model.RecurrenceSeries
import java.time.LocalDate

object RecurrenceCalculator {

    /**
     * Calculates the next occurrence date after [currentDate] based on the series configuration.
     * Enforces end date check (returns null if next date is after endDate).
     */
    fun calculateNextOccurrenceDate(
        series: RecurrenceSeries,
        currentDate: LocalDate
    ): LocalDate? {
        val nextDate = when (series.frequency) {
            RecurrenceFrequency.DAILY -> {
                val step = series.interval.coerceAtLeast(1).toLong()
                currentDate.plusDays(step)
            }

            RecurrenceFrequency.WEEKDAYS -> {
                val allowedDays = if (series.selectedDaysOfWeek.isNotEmpty()) {
                    series.selectedDaysOfWeek
                } else {
                    listOf(1, 2, 3, 4, 5) // Monday to Friday
                }

                var candidate = currentDate.plusDays(1)
                var count = 0
                while (candidate.dayOfWeek.value !in allowedDays && count < 14) {
                    candidate = candidate.plusDays(1)
                    count++
                }
                candidate
            }

            RecurrenceFrequency.WEEKLY -> {
                val allowedDays = series.selectedDaysOfWeek.sorted()
                if (allowedDays.isEmpty()) {
                    val stepWeeks = series.interval.coerceAtLeast(1).toLong()
                    currentDate.plusWeeks(stepWeeks)
                } else {
                    val currentDayVal = currentDate.dayOfWeek.value
                    val nextInSameWeek = allowedDays.firstOrNull { it > currentDayVal }
                    if (nextInSameWeek != null) {
                        currentDate.plusDays((nextInSameWeek - currentDayVal).toLong())
                    } else {
                        // Move to next interval cycle and pick the first allowed day
                        val firstDay = allowedDays.first()
                        val stepWeeks = series.interval.coerceAtLeast(1).toLong()
                        val nextWeekBase = currentDate.plusWeeks(stepWeeks)
                        val diff = firstDay - nextWeekBase.dayOfWeek.value
                        nextWeekBase.plusDays(diff.toLong())
                    }
                }
            }

            RecurrenceFrequency.MONTHLY -> {
                val stepMonths = series.interval.coerceAtLeast(1).toLong()
                val targetMonthYear = currentDate.plusMonths(stepMonths)
                val desiredDay = series.dayOfMonth ?: currentDate.dayOfMonth
                val maxDayInMonth = targetMonthYear.lengthOfMonth()
                val clampedDay = minOf(desiredDay, maxDayInMonth)
                targetMonthYear.withDayOfMonth(clampedDay)
            }
        }

        if (series.endDate != null && nextDate.isAfter(series.endDate)) {
            return null
        }

        return nextDate
    }
}
