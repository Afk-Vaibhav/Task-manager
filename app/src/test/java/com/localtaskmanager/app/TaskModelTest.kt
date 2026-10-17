package com.localtaskmanager.app

import com.localtaskmanager.app.domain.model.AdvanceReminder
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskModelTest {

    @Test
    fun testIsOverdueLogic() {
        val today = LocalDate.of(2026, 10, 16)
        val nowTime = LocalTime.of(15, 0) // 3:00 PM

        // 1. Past due date
        val pastTask = Task(
            id = "t1",
            title = "Past Task",
            dueDate = today.minusDays(1),
            status = TaskStatus.OPEN
        )
        assertTrue(pastTask.isOverdue(today, nowTime))

        // 2. Completed past task -> not overdue
        val completedPastTask = pastTask.copy(status = TaskStatus.COMPLETED)
        assertFalse(completedPastTask.isOverdue(today, nowTime))

        // 3. Today task past time -> overdue
        val todayPastTimeTask = Task(
            id = "t2",
            title = "Today Past",
            dueDate = today,
            scheduledTime = LocalTime.of(14, 0), // 2:00 PM
            status = TaskStatus.OPEN
        )
        assertTrue(todayPastTimeTask.isOverdue(today, nowTime))

        // 4. Today task future time -> not overdue
        val todayFutureTimeTask = Task(
            id = "t3",
            title = "Today Future",
            dueDate = today,
            scheduledTime = LocalTime.of(16, 0), // 4:00 PM
            status = TaskStatus.OPEN
        )
        assertFalse(todayFutureTimeTask.isOverdue(today, nowTime))

        // 5. Today task without scheduled time -> not overdue yet
        val todayAllDayTask = Task(
            id = "t4",
            title = "Today All Day",
            dueDate = today,
            scheduledTime = null,
            status = TaskStatus.OPEN
        )
        assertFalse(todayAllDayTask.isOverdue(today, nowTime))

        // 6. Undated task -> not overdue
        val undatedTask = Task(
            id = "t5",
            title = "Undated Task",
            dueDate = null,
            status = TaskStatus.OPEN
        )
        assertFalse(undatedTask.isOverdue(today, nowTime))
    }

    @Test
    fun testEffectiveReminderTriggerCalculations() {
        val zoneId = ZoneId.of("UTC")
        val date = LocalDate.of(2026, 10, 16)
        val time = LocalTime.of(18, 0) // 6:00 PM UTC
        val dueEpochMs = ZonedDateTime.of(date, time, zoneId).toInstant().toEpochMilli()

        // 1. Task with advance reminder (15 minutes before)
        val taskWithAdvance = Task(
            id = "t1",
            title = "Advance Alert Task",
            dueDate = date,
            scheduledTime = time,
            reminderEnabled = true,
            advanceReminder = AdvanceReminder.MINUTES_15,
            status = TaskStatus.OPEN
        )
        val expectedTrigger = dueEpochMs - (15 * 60 * 1000L)
        assertEquals(expectedTrigger, taskWithAdvance.getEffectiveReminderEpochMs(zoneId))

        // 2. Task with active snooze override -> snooze takes precedence
        val snoozeEpochMs = dueEpochMs + (30 * 60 * 1000L)
        val snoozedTask = taskWithAdvance.copy(snoozeUntilEpochMs = snoozeEpochMs)
        assertEquals(snoozeEpochMs, snoozedTask.getEffectiveReminderEpochMs(zoneId))

        // 3. Completed task -> no reminder trigger
        val completedTask = taskWithAdvance.copy(status = TaskStatus.COMPLETED)
        assertNull(completedTask.getEffectiveReminderEpochMs(zoneId))
    }
}
