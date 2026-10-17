package com.localtaskmanager.app.domain.usecase

import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.TaskRepository
import java.time.LocalDate

class DailyReviewUseCase(
    private val taskRepository: TaskRepository
) {
    suspend fun getReviewTasks(today: LocalDate = LocalDate.now()): List<Task> {
        return taskRepository.getTasksForDailyReview(today)
    }

    suspend fun moveTaskToTomorrow(taskId: String, today: LocalDate = LocalDate.now()): Task? {
        val task = taskRepository.getTaskById(taskId) ?: return null
        val tomorrow = today.plusDays(1)
        val updated = task.copy(
            dueDate = tomorrow,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        taskRepository.updateTask(updated)
        return updated
    }

    suspend fun rescheduleTaskToDate(taskId: String, newDate: LocalDate): Task? {
        val task = taskRepository.getTaskById(taskId) ?: return null
        val updated = task.copy(
            dueDate = newDate,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        taskRepository.updateTask(updated)
        return updated
    }
}
