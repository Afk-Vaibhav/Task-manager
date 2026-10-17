package com.localtaskmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recurrence_series")
data class RecurrenceSeriesEntity(
    @PrimaryKey val id: String,
    val frequency: String, // "DAILY", "WEEKDAYS", "WEEKLY", "MONTHLY"
    val interval: Int = 1,
    val selectedDaysOfWeek: String? = null, // comma-separated "1,2,3,4,5"
    val dayOfMonth: Int? = null,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long? = null,
    val templateTitle: String,
    val templateNotes: String? = null,
    val templateCategoryId: String? = null,
    val templateScheduledMinuteOfDay: Int? = null,
    val templatePriority: Int = 1,
    val templateReminderEnabled: Boolean = false,
    val templateAdvanceReminderMinutes: Int? = null,
    val isActive: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
