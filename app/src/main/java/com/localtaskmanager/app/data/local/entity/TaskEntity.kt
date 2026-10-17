package com.localtaskmanager.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status", "dueDateEpochDay"]),
        Index(value = ["categoryId"]),
        Index(value = ["recurrenceSeriesId"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String? = null,
    val categoryId: String? = null,
    val dueDateEpochDay: Long? = null,
    val scheduledMinuteOfDay: Int? = null,
    val priority: Int = 1, // 0: Low, 1: Normal, 2: High
    val status: String = "OPEN", // "OPEN", "COMPLETED"
    val completedAtEpochMs: Long? = null,
    val reminderEnabled: Boolean = false,
    val advanceReminderMinutes: Int? = null,
    val snoozeUntilEpochMs: Long? = null,
    val recurrenceSeriesId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
