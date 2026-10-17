package com.localtaskmanager.app.domain.usecase

import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.repository.TaskRepository

class CompleteTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String): Result<Task> {
        val completed = taskRepository.completeTask(taskId)
            ?: return Result.failure(IllegalStateException("Task not found"))
        return Result.success(completed)
    }
}
