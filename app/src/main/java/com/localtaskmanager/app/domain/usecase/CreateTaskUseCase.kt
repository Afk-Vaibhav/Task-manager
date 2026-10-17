package com.localtaskmanager.app.domain.usecase

import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.TaskRepository
import java.util.UUID

class CreateTaskUseCase(
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

        val id = if (task.id.isBlank()) UUID.randomUUID().toString() else task.id
        val now = System.currentTimeMillis()
        val preparedTask = task.copy(
            id = id,
            title = trimmedTitle,
            createdAtEpochMs = if (task.createdAtEpochMs == 0L) now else task.createdAtEpochMs,
            updatedAtEpochMs = now
        )

        taskRepository.insertTask(preparedTask)
        return Result.success(preparedTask)
    }
}
