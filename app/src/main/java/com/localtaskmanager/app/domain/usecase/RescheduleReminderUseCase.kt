package com.localtaskmanager.app.domain.usecase

import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.TaskRepository

class RescheduleReminderUseCase(
    private val taskRepository: TaskRepository
) {
    /**
     * Reschedules the task reminder by [hours] and [minutes] from current wall-clock time.
     * Enforces hours 0..23, minutes 0..59, and total duration > 0.
     */
    suspend operator fun invoke(
        taskId: String,
        hours: Int,
        minutes: Int,
        currentWallClockEpochMs: Long = System.currentTimeMillis()
    ): Result<Task> {
        if (hours !in 0..23) {
            return Result.failure(IllegalArgumentException("Hours must be between 0 and 23"))
        }
        if (minutes !in 0..59) {
            return Result.failure(IllegalArgumentException("Minutes must be between 0 and 59"))
        }
        val totalMinutes = (hours * 60) + minutes
        if (totalMinutes <= 0) {
            return Result.failure(IllegalArgumentException("Delay duration must be greater than 0 minutes"))
        }

        val targetEpochMs = currentWallClockEpochMs + (totalMinutes * 60 * 1000L)
        val updatedTask = taskRepository.rescheduleTaskReminder(taskId, targetEpochMs)
            ?: return Result.failure(IllegalStateException("Task with ID '$taskId' not found"))

        return Result.success(updatedTask)
    }

    companion object {
        fun calculateTargetEpochMs(
            hours: Int,
            minutes: Int,
            baseEpochMs: Long = System.currentTimeMillis()
        ): Long {
            val totalMinutes = (hours * 60) + minutes
            return baseEpochMs + (totalMinutes * 60 * 1000L)
        }
    }
}
