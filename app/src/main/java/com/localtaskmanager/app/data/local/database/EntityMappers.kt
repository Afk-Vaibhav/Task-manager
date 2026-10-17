package com.localtaskmanager.app.data.local.database

import com.localtaskmanager.app.data.local.entity.CategoryEntity
import com.localtaskmanager.app.data.local.entity.RecurrenceSeriesEntity
import com.localtaskmanager.app.data.local.entity.TaskEntity
import com.localtaskmanager.app.domain.model.AdvanceReminder
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.RecurrenceFrequency
import com.localtaskmanager.app.domain.model.RecurrenceSeries
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalTime

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        iconKey = iconKey,
        colorArgb = colorArgb,
        isDefault = isDefault,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        iconKey = iconKey,
        colorArgb = colorArgb,
        isDefault = isDefault,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun TaskEntity.toDomain(category: Category? = null): Task {
    val dueDate = dueDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    val scheduledTime = scheduledMinuteOfDay?.let {
        val hour = it / 60
        val minute = it % 60
        LocalTime.of(hour, minute)
    }

    return Task(
        id = id,
        title = title,
        notes = notes,
        categoryId = categoryId,
        category = category,
        dueDate = dueDate,
        scheduledTime = scheduledTime,
        priority = Priority.fromValue(priority),
        status = TaskStatus.fromValue(status),
        completedAtEpochMs = completedAtEpochMs,
        reminderEnabled = reminderEnabled,
        advanceReminder = advanceReminderMinutes?.let { AdvanceReminder.fromMinutes(it) },
        snoozeUntilEpochMs = snoozeUntilEpochMs,
        recurrenceSeriesId = recurrenceSeriesId,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun Task.toEntity(): TaskEntity {
    val dueDateEpochDay = dueDate?.toEpochDay()
    val scheduledMinuteOfDay = scheduledTime?.let { it.hour * 60 + it.minute }

    return TaskEntity(
        id = id,
        title = title,
        notes = notes,
        categoryId = categoryId,
        dueDateEpochDay = dueDateEpochDay,
        scheduledMinuteOfDay = scheduledMinuteOfDay,
        priority = priority.value,
        status = status.value,
        completedAtEpochMs = completedAtEpochMs,
        reminderEnabled = reminderEnabled,
        advanceReminderMinutes = advanceReminder?.minutesBefore,
        snoozeUntilEpochMs = snoozeUntilEpochMs,
        recurrenceSeriesId = recurrenceSeriesId,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun RecurrenceSeriesEntity.toDomain(): RecurrenceSeries {
    val daysList = selectedDaysOfWeek?.split(",")
        ?.mapNotNull { it.trim().toIntOrNull() }
        ?: emptyList()

    val time = templateScheduledMinuteOfDay?.let {
        LocalTime.of(it / 60, it % 60)
    }

    return RecurrenceSeries(
        id = id,
        frequency = RecurrenceFrequency.fromValue(frequency),
        interval = interval,
        selectedDaysOfWeek = daysList,
        dayOfMonth = dayOfMonth,
        startDate = LocalDate.ofEpochDay(startDateEpochDay),
        endDate = endDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        templateTitle = templateTitle,
        templateNotes = templateNotes,
        templateCategoryId = templateCategoryId,
        templateScheduledTime = time,
        templatePriority = Priority.fromValue(templatePriority),
        templateReminderEnabled = templateReminderEnabled,
        templateAdvanceReminder = templateAdvanceReminderMinutes?.let { AdvanceReminder.fromMinutes(it) },
        isActive = isActive,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun RecurrenceSeries.toEntity(): RecurrenceSeriesEntity {
    val daysString = if (selectedDaysOfWeek.isNotEmpty()) {
        selectedDaysOfWeek.joinToString(",")
    } else null

    val minuteOfDay = templateScheduledTime?.let { it.hour * 60 + it.minute }

    return RecurrenceSeriesEntity(
        id = id,
        frequency = frequency.value,
        interval = interval,
        selectedDaysOfWeek = daysString,
        dayOfMonth = dayOfMonth,
        startDateEpochDay = startDate.toEpochDay(),
        endDateEpochDay = endDate?.toEpochDay(),
        templateTitle = templateTitle,
        templateNotes = templateNotes,
        templateCategoryId = templateCategoryId,
        templateScheduledMinuteOfDay = minuteOfDay,
        templatePriority = templatePriority.value,
        templateReminderEnabled = templateReminderEnabled,
        templateAdvanceReminderMinutes = templateAdvanceReminder?.minutesBefore,
        isActive = isActive,
        createdAtEpochMs = createdAtEpochMs
    )
}
