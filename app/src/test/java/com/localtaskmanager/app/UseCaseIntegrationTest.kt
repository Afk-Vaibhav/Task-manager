package com.localtaskmanager.app

import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.CompleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.CreateTaskUseCase
import com.localtaskmanager.app.domain.usecase.DailyReviewUseCase
import com.localtaskmanager.app.domain.usecase.DeleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.ReopenTaskUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class UseCaseIntegrationTest {

    private class InMemoryTaskRepository : TaskRepository {
        private val tasksMap = mutableMapOf<String, Task>()
        private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

        private fun emit() {
            tasksFlow.value = tasksMap.values.toList()
        }

        override fun getAllTasksFlow(): Flow<List<Task>> = tasksFlow.asStateFlow()
        override fun getOpenTasksFlow(): Flow<List<Task>> = tasksFlow.map { list -> list.filter { it.isOpen } }
        override fun getTasksForDateFlow(date: LocalDate): Flow<List<Task>> =
            tasksFlow.map { list -> list.filter { it.dueDate == date } }

        override fun getTasksForDateRangeFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> =
            tasksFlow.map { list ->
                list.filter { it.dueDate != null && !it.dueDate.isBefore(startDate) && !it.dueDate.isAfter(endDate) }
            }

        override fun getTaskByIdFlow(id: String): Flow<Task?> = tasksFlow.map { it.firstOrNull { t -> t.id == id } }

        override suspend fun getTaskById(id: String): Task? = tasksMap[id]
        override suspend fun getAllTasksSnapshot(): List<Task> = tasksMap.values.toList()
        override suspend fun getOpenTasksSnapshot(): List<Task> = tasksMap.values.filter { it.isOpen }
        override suspend fun getTasksWithActiveReminders(): List<Task> =
            tasksMap.values.filter { it.isOpen && (it.reminderEnabled || it.snoozeUntilEpochMs != null) }

        override suspend fun insertTask(task: Task) {
            tasksMap[task.id] = task
            emit()
        }

        override suspend fun updateTask(task: Task) {
            tasksMap[task.id] = task
            emit()
        }

        override suspend fun deleteTask(id: String) {
            tasksMap.remove(id)
            emit()
        }

        override suspend fun completeTask(id: String): Task? {
            val task = tasksMap[id] ?: return null
            if (task.isCompleted) return task
            val updated = task.copy(
                status = TaskStatus.COMPLETED,
                completedAtEpochMs = System.currentTimeMillis(),
                snoozeUntilEpochMs = null
            )
            tasksMap[id] = updated
            emit()
            return updated
        }

        override suspend fun reopenTask(id: String): Task? {
            val task = tasksMap[id] ?: return null
            val updated = task.copy(
                status = TaskStatus.OPEN,
                completedAtEpochMs = null
            )
            tasksMap[id] = updated
            emit()
            return updated
        }

        override suspend fun rescheduleTaskReminder(id: String, snoozeUntilEpochMs: Long): Task? {
            val task = tasksMap[id] ?: return null
            val updated = task.copy(snoozeUntilEpochMs = snoozeUntilEpochMs)
            tasksMap[id] = updated
            emit()
            return updated
        }

        override suspend fun clearSnooze(id: String): Task? {
            val task = tasksMap[id] ?: return null
            val updated = task.copy(snoozeUntilEpochMs = null)
            tasksMap[id] = updated
            emit()
            return updated
        }

        override suspend fun getTasksForDailyReview(today: LocalDate): List<Task> {
            return tasksMap.values.filter {
                it.isOpen && (it.dueDate == null || !it.dueDate.isAfter(today))
            }
        }
    }

    private lateinit var repo: InMemoryTaskRepository
    private lateinit var createTaskUseCase: CreateTaskUseCase
    private lateinit var completeTaskUseCase: CompleteTaskUseCase
    private lateinit var reopenTaskUseCase: ReopenTaskUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    private lateinit var dailyReviewUseCase: DailyReviewUseCase

    @Before
    fun setUp() {
        repo = InMemoryTaskRepository()
        createTaskUseCase = CreateTaskUseCase(repo)
        completeTaskUseCase = CompleteTaskUseCase(repo)
        reopenTaskUseCase = ReopenTaskUseCase(repo)
        deleteTaskUseCase = DeleteTaskUseCase(repo)
        dailyReviewUseCase = DailyReviewUseCase(repo)
    }

    @Test
    fun testTaskCreationAndValidation() = runTest {
        // Empty title rejected
        val emptyResult = createTaskUseCase(Task(id = "", title = "   "))
        assertTrue(emptyResult.isFailure)

        // Valid task created and given UUID
        val successResult = createTaskUseCase(Task(id = "", title = "Study for exam"))
        assertTrue(successResult.isSuccess)
        val task = successResult.getOrThrow()
        assertTrue(task.id.isNotBlank())
        assertEquals("Study for exam", task.title)
    }

    @Test
    fun testTaskCompletionAndReopeningLifecycle() = runTest {
        val created = createTaskUseCase(Task(id = "test-1", title = "Buy groceries")).getOrThrow()
        assertEquals(TaskStatus.OPEN, created.status)

        // Mark completed
        val completed = completeTaskUseCase(created.id).getOrThrow()
        assertEquals(TaskStatus.COMPLETED, completed.status)
        assertNotNull(completed.completedAtEpochMs)

        // Reopen
        val reopened = reopenTaskUseCase(created.id).getOrThrow()
        assertEquals(TaskStatus.OPEN, reopened.status)
        assertEquals(null, reopened.completedAtEpochMs)
    }

    @Test
    fun testDailyReviewPostponingToTomorrow() = runTest {
        val today = LocalDate.of(2026, 10, 16)
        val created = createTaskUseCase(
            Task(id = "review-1", title = "Unfinished task", dueDate = today)
        ).getOrThrow()

        val reviewList = dailyReviewUseCase.getReviewTasks(today)
        assertEquals(1, reviewList.size)

        // Move to tomorrow
        val updated = dailyReviewUseCase.moveTaskToTomorrow(created.id, today)
        assertNotNull(updated)
        assertEquals(today.plusDays(1), updated?.dueDate)
        assertEquals("Unfinished task", updated?.title)
    }
}
