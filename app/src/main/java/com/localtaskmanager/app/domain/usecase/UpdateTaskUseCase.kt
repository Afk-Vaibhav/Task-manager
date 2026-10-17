package com.localtaskmanager.app.domain.usecase

import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Result<Task> {
        val trimmedTitle = task.title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Task title cannot be empty"))
        }
        if (trimmedTitle.length > 250) {
            return Result.failure(IllegalArgumentException("Task title cannot exceed 250 characters"))
        }

        val updatedTask = task.copy(
            title = trimmedTitle,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        taskRepository.updateTask(updatedTask)
        return Result.success(updatedTask)
    }
}
