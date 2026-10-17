package com.localtaskmanager.app.domain.repository

import com.localtaskmanager.app.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getAllTasksFlow(): Flow<List<Task>>
    fun getOpenTasksFlow(): Flow<List<Task>>
    fun getTasksForDateFlow(date: LocalDate): Flow<List<Task>>
    fun getTasksForDateRangeFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>
    fun getTaskByIdFlow(id: String): Flow<Task?>
    suspend fun getTaskById(id: String): Task?
    suspend fun getAllTasksSnapshot(): List<Task>
    suspend fun getOpenTasksSnapshot(): List<Task>
    suspend fun getTasksWithActiveReminders(): List<Task>
    suspend fun insertTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun completeTask(id: String): Task?
    suspend fun reopenTask(id: String): Task?
    suspend fun rescheduleTaskReminder(id: String, snoozeUntilEpochMs: Long): Task?
    suspend fun clearSnooze(id: String): Task?
    suspend fun getTasksForDailyReview(today: LocalDate): List<Task>
}
