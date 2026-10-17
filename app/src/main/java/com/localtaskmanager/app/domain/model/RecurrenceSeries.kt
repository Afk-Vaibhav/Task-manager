package com.localtaskmanager.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class RecurrenceSeries(
    val id: String,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val selectedDaysOfWeek: List<Int> = emptyList(), // 1 = Monday, 7 = Sunday
    val dayOfMonth: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val templateTitle: String,
    val templateNotes: String? = null,
    val templateCategoryId: String? = null,
    val templateScheduledTime: LocalTime? = null,
    val templatePriority: Priority = Priority.NORMAL,
    val templateReminderEnabled: Boolean = false,
    val templateAdvanceReminder: AdvanceReminder? = null,
    val isActive: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
