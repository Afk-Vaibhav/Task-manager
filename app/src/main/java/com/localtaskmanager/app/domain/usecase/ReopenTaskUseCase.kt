package com.localtaskmanager.app.domain.usecase

import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.TaskRepository

class ReopenTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Task> {
        val reopened = taskRepository.reopenTask(taskId)
            ?: return Result.failure(IllegalStateException("Task not found"))
        return Result.success(reopened)
    }
}
