package com.localtaskmanager.app

import com.localtaskmanager.app.data.export.BackupPayload
import com.localtaskmanager.app.data.local.entity.CategoryEntity
import com.localtaskmanager.app.data.local.entity.TaskEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DataBackupSerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testBackupPayloadSerializationRoundTrip() {
        val categories = listOf(
            CategoryEntity(
                id = "cat_test_1",
                name = "Work",
                iconKey = "work",
                colorArgb = 0xFF1E88E5.toInt(),
                isDefault = true
            )
        )

        val tasks = listOf(
            TaskEntity(
                id = "task_test_1",
                title = "Prepare quarterly report",
                notes = "Include revenue breakdown",
                categoryId = "cat_test_1",
                dueDateEpochDay = 20743L,
                scheduledMinuteOfDay = 600, // 10:00 AM
                priority = 2, // High
                status = "OPEN",
                reminderEnabled = true,
                advanceReminderMinutes = 15,
                snoozeUntilEpochMs = null
            )
        )

        val originalPayload = BackupPayload(
            version = 1,
            exportedAtEpochMs = 1729000000000L,
            categories = categories,
            tasks = tasks
        )

        val jsonString = json.encodeToString(originalPayload)
        assertNotNull(jsonString)

        val deserialized = json.decodeFromString<BackupPayload>(jsonString)
        assertEquals(1, deserialized.version)
        assertEquals(1, deserialized.categories.size)
        assertEquals("Work", deserialized.categories[0].name)
        assertEquals(1, deserialized.tasks.size)
        assertEquals("Prepare quarterly report", deserialized.tasks[0].title)
        assertEquals(600, deserialized.tasks[0].scheduledMinuteOfDay)
    }
}
