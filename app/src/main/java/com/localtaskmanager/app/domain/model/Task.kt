package com.localtaskmanager.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class Task(
    val id: String,
    val title: String,
    val notes: String? = null,
    val categoryId: String? = null,
    val category: Category? = null,
    val dueDate: LocalDate? = null,
    val scheduledTime: LocalTime? = null,
    val priority: Priority = Priority.NORMAL,
    val status: TaskStatus = TaskStatus.OPEN,
    val completedAtEpochMs: Long? = null,
    val reminderEnabled: Boolean = false,
    val advanceReminder: AdvanceReminder? = null,
    val snoozeUntilEpochMs: Long? = null,
    val recurrenceSeriesId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED

    val isOpen: Boolean
        get() = status == TaskStatus.OPEN

    fun isOverdue(
        today: LocalDate = LocalDate.now(),
        currentTime: LocalTime = LocalTime.now()
    ): Boolean {
        if (isCompleted || dueDate == null) return false
        if (dueDate.isBefore(today)) return true
        if (dueDate.isEqual(today)) {
            return scheduledTime != null && scheduledTime.isBefore(currentTime)
        }
        return false
    }

    fun isDueToday(today: LocalDate = LocalDate.now()): Boolean {
        return dueDate != null && dueDate.isEqual(today)
    }

    fun getEffectiveReminderEpochMs(zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        if (!isOpen) return null
        if (snoozeUntilEpochMs != null) {
            return snoozeUntilEpochMs
        }
        if (reminderEnabled && dueDate != null && scheduledTime != null) {
            val zdt = ZonedDateTime.of(dueDate, scheduledTime, zoneId)
            val baseTimeMs = zdt.toInstant().toEpochMilli()
            val advanceMinutes = advanceReminder?.minutesBefore ?: 0
            return baseTimeMs - (advanceMinutes * 60 * 1000L)
        }
        return null
    }

    fun getTaskDueEpochMs(zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        if (dueDate == null || scheduledTime == null) return null
        return ZonedDateTime.of(dueDate, scheduledTime, zoneId).toInstant().toEpochMilli()
    }
}
