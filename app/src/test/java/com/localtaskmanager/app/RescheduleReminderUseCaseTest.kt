package com.localtaskmanager.app

import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.RescheduleReminderUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RescheduleReminderUseCaseTest {

    private class FakeTaskRepository : TaskRepository {
        var lastRescheduledId: String? = null
        var lastRescheduledTargetMs: Long? = null

        override fun getAllTasksFlow(): Flow<List<Task>> = emptyFlow()
        override fun getOpenTasksFlow(): Flow<List<Task>> = emptyFlow()
        override fun getTasksForDateFlow(date: LocalDate): Flow<List<Task>> = emptyFlow()
        override fun getTasksForDateRangeFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> = emptyFlow()
        override fun getTaskByIdFlow(id: String): Flow<Task?> = emptyFlow()
        override suspend fun getTaskById(id: String): Task? = Task(id = id, title = "Test Task")
        override suspend fun getAllTasksSnapshot(): List<Task> = emptyList()
        override suspend fun getOpenTasksSnapshot(): List<Task> = emptyList()
        override suspend fun getTasksWithActiveReminders(): List<Task> = emptyList()
        override suspend fun insertTask(task: Task) {}
        override suspend fun updateTask(task: Task) {}
        override suspend fun deleteTask(id: String) {}
        override suspend fun completeTask(id: String): Task? = null
        override suspend fun reopenTask(id: String): Task? = null

        override suspend fun rescheduleTaskReminder(id: String, snoozeUntilEpochMs: Long): Task? {
            lastRescheduledId = id
            lastRescheduledTargetMs = snoozeUntilEpochMs
            return Task(
                id = id,
                title = "Test Task",
                snoozeUntilEpochMs = snoozeUntilEpochMs
            )
        }

        override suspend fun clearSnooze(id: String): Task? = null
        override suspend fun getTasksForDailyReview(today: LocalDate): List<Task> = emptyList()
    }

    @Test
    fun testZeroDurationRejected() = runTest {
        val repo = FakeTaskRepository()
        val useCase = RescheduleReminderUseCase(repo)

        val result = useCase("task-1", hours = 0, minutes = 0)
        assertTrue(result.isFailure)
    }

    @Test
    fun testInvalidHoursRejected() = runTest {
        val repo = FakeTaskRepository()
        val useCase = RescheduleReminderUseCase(repo)

        val resultNegative = useCase("task-1", hours = -1, minutes = 10)
        assertTrue(resultNegative.isFailure)

        val resultOver23 = useCase("task-1", hours = 24, minutes = 10)
        assertTrue(resultOver23.isFailure)
    }

    @Test
    fun testInvalidMinutesRejected() = runTest {
        val repo = FakeTaskRepository()
        val useCase = RescheduleReminderUseCase(repo)

        val resultOver59 = useCase("task-1", hours = 1, minutes = 60)
        assertTrue(resultOver59.isFailure)
    }

    @Test
    fun testValidOneHourThirtyMinutesDelay() = runTest {
        val repo = FakeTaskRepository()
        val useCase = RescheduleReminderUseCase(repo)

        val baseMs = 1_000_000_000L
        val result = useCase("task-1", hours = 1, minutes = 30, currentWallClockEpochMs = baseMs)
        assertTrue(result.isSuccess)

        // 1 hour and 30 minutes = 90 minutes = 5,400,000 ms
        val expectedTarget = baseMs + (90 * 60 * 1000L)
        assertEquals(expectedTarget, repo.lastRescheduledTargetMs)
    }
}
