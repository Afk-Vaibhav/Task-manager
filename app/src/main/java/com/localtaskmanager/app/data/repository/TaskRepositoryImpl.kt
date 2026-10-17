package com.localtaskmanager.app.data.repository

import androidx.room.withTransaction
import com.localtaskmanager.app.data.local.dao.CategoryDao
import com.localtaskmanager.app.data.local.dao.RecurrenceSeriesDao
import com.localtaskmanager.app.data.local.dao.TaskDao
import com.localtaskmanager.app.data.local.database.AppDatabase
import com.localtaskmanager.app.data.local.database.toDomain
import com.localtaskmanager.app.data.local.database.toEntity
import com.localtaskmanager.app.data.local.entity.TaskEntity
import com.localtaskmanager.app.domain.model.Category
import com.localtaskmanager.app.domain.model.Task
import com.localtaskmanager.app.domain.model.TaskStatus
import com.localtaskmanager.app.domain.recurrence.RecurrenceCalculator
import com.localtaskmanager.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

class TaskRepositoryImpl(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val recurrenceSeriesDao: RecurrenceSeriesDao
) : TaskRepository {

    private fun mapEntitiesWithCategories(
        tasks: List<TaskEntity>,
        categoriesMap: Map<String, Category>
    ): List<Task> {
        return tasks.map { entity ->
            val cat = entity.categoryId?.let { categoriesMap[it] }
            entity.toDomain(cat)
        }
    }

    override fun getAllTasksFlow(): Flow<List<Task>> {
        return combine(
            taskDao.getAllTasksFlow(),
            categoryDao.getAllCategoriesFlow()
        ) { taskEntities, categoryEntities ->
            val categoriesMap = categoryEntities.associate { it.id to it.toDomain() }
            mapEntitiesWithCategories(taskEntities, categoriesMap)
        }
    }

    override fun getOpenTasksFlow(): Flow<List<Task>> {
        return combine(
            taskDao.getOpenTasksFlow(),
            categoryDao.getAllCategoriesFlow()
        ) { taskEntities, categoryEntities ->
            val categoriesMap = categoryEntities.associate { it.id to it.toDomain() }
            mapEntitiesWithCategories(taskEntities, categoriesMap)
        }
    }

    override fun getTasksForDateFlow(date: LocalDate): Flow<List<Task>> {
        return combine(
            taskDao.getTasksForDateFlow(date.toEpochDay()),
            categoryDao.getAllCategoriesFlow()
        ) { taskEntities, categoryEntities ->
            val categoriesMap = categoryEntities.associate { it.id to it.toDomain() }
            mapEntitiesWithCategories(taskEntities, categoriesMap)
        }
    }

    override fun getTasksForDateRangeFlow(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Task>> {
        return combine(
            taskDao.getTasksForDateRangeFlow(startDate.toEpochDay(), endDate.toEpochDay()),
            categoryDao.getAllCategoriesFlow()
        ) { taskEntities, categoryEntities ->
            val categoriesMap = categoryEntities.associate { it.id to it.toDomain() }
            mapEntitiesWithCategories(taskEntities, categoriesMap)
        }
    }

    override fun getTaskByIdFlow(id: String): Flow<Task?> {
        return combine(
            taskDao.getTaskByIdFlow(id),
            categoryDao.getAllCategoriesFlow()
        ) { entity, categories ->
            if (entity == null) return@combine null
            val cat = entity.categoryId?.let { catId ->
                categories.firstOrNull { it.id == catId }?.toDomain()
            }
            entity.toDomain(cat)
        }
    }

    override suspend fun getTaskById(id: String): Task? {
        val entity = taskDao.getTaskById(id) ?: return null
        val category = entity.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
        return entity.toDomain(category)
    }

    override suspend fun getAllTasksSnapshot(): List<Task> {
        val entities = taskDao.getAllTasksSnapshot()
        val categoriesMap = categoryDao.getAllCategories().associate { it.id to it.toDomain() }
        return mapEntitiesWithCategories(entities, categoriesMap)
    }

    override suspend fun getOpenTasksSnapshot(): List<Task> {
        val entities = taskDao.getOpenTasksSnapshot()
        val categoriesMap = categoryDao.getAllCategories().associate { it.id to it.toDomain() }
        return mapEntitiesWithCategories(entities, categoriesMap)
    }

    override suspend fun getTasksWithActiveReminders(): List<Task> {
        val entities = taskDao.getTasksWithActiveReminders()
        val categoriesMap = categoryDao.getAllCategories().associate { it.id to it.toDomain() }
        return mapEntitiesWithCategories(entities, categoriesMap)
    }

    override suspend fun insertTask(task: Task) {
        val trimmedTitle = task.title.trim()
        require(trimmedTitle.isNotEmpty()) { "Task title cannot be empty" }
        require(trimmedTitle.length <= 250) { "Task title exceeds limit of 250 characters" }

        val entity = task.copy(
            title = trimmedTitle,
            updatedAtEpochMs = System.currentTimeMillis()
        ).toEntity()

        taskDao.insertTask(entity)
    }

    override suspend fun updateTask(task: Task) {
        val trimmedTitle = task.title.trim()
        require(trimmedTitle.isNotEmpty()) { "Task title cannot be empty" }
        require(trimmedTitle.length <= 250) { "Task title exceeds limit of 250 characters" }

        val entity = task.copy(
            title = trimmedTitle,
            updatedAtEpochMs = System.currentTimeMillis()
        ).toEntity()

        taskDao.updateTask(entity)
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun completeTask(id: String): Task? {
        return database.withTransaction {
            val entity = taskDao.getTaskById(id) ?: return@withTransaction null
            if (entity.status == TaskStatus.COMPLETED.value) {
                val cat = entity.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
                return@withTransaction entity.toDomain(cat)
            }

            val now = System.currentTimeMillis()
            val updated = entity.copy(
                status = TaskStatus.COMPLETED.value,
                completedAtEpochMs = now,
                snoozeUntilEpochMs = null, // Clear any active snooze on completion
                updatedAtEpochMs = now
            )
            taskDao.updateTask(updated)

            // If recurring, generate next occurrence transactionally
            if (!entity.recurrenceSeriesId.isNullOrEmpty()) {
                val seriesEntity = recurrenceSeriesDao.getSeriesById(entity.recurrenceSeriesId)
                if (seriesEntity != null && seriesEntity.isActive) {
                    val currentDueDate = entity.dueDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
                    val nextDate = RecurrenceCalculator.calculateNextOccurrenceDate(
                        series = seriesEntity.toDomain(),
                        currentDate = currentDueDate
                    )

                    if (nextDate != null) {
                        val existingOccurrence = taskDao.getTaskBySeriesAndDate(
                            seriesId = seriesEntity.id,
                            dateEpochDay = nextDate.toEpochDay()
                        )
                        if (existingOccurrence == null) {
                            val nextOccurrence = TaskEntity(
                                id = UUID.randomUUID().toString(),
                                title = seriesEntity.templateTitle,
                                notes = seriesEntity.templateNotes,
                                categoryId = seriesEntity.templateCategoryId,
                                dueDateEpochDay = nextDate.toEpochDay(),
                                scheduledMinuteOfDay = seriesEntity.templateScheduledMinuteOfDay,
                                priority = seriesEntity.templatePriority,
                                status = TaskStatus.OPEN.value,
                                completedAtEpochMs = null,
                                reminderEnabled = seriesEntity.templateReminderEnabled,
                                advanceReminderMinutes = seriesEntity.templateAdvanceReminderMinutes,
                                snoozeUntilEpochMs = null,
                                recurrenceSeriesId = seriesEntity.id,
                                createdAtEpochMs = now,
                                updatedAtEpochMs = now
                            )
                            taskDao.insertTask(nextOccurrence)
                        }
                    }
                }
            }

            val category = updated.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
            updated.toDomain(category)
        }
    }

    override suspend fun reopenTask(id: String): Task? {
        val entity = taskDao.getTaskById(id) ?: return null
        if (entity.status == TaskStatus.OPEN.value) {
            val cat = entity.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
            return entity.toDomain(cat)
        }

        val now = System.currentTimeMillis()
        val updated = entity.copy(
            status = TaskStatus.OPEN.value,
            completedAtEpochMs = null,
            updatedAtEpochMs = now
        )
        taskDao.updateTask(updated)

        val cat = updated.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
        return updated.toDomain(cat)
    }

    override suspend fun rescheduleTaskReminder(id: String, snoozeUntilEpochMs: Long): Task? {
        val entity = taskDao.getTaskById(id) ?: return null
        val now = System.currentTimeMillis()
        require(snoozeUntilEpochMs > now) { "Reschedule target time must be in the future" }

        val updated = entity.copy(
            snoozeUntilEpochMs = snoozeUntilEpochMs,
            updatedAtEpochMs = now
        )
        taskDao.updateTask(updated)

        val cat = updated.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
        return updated.toDomain(cat)
    }

    override suspend fun clearSnooze(id: String): Task? {
        val entity = taskDao.getTaskById(id) ?: return null
        if (entity.snoozeUntilEpochMs == null) {
            val cat = entity.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
            return entity.toDomain(cat)
        }

        val now = System.currentTimeMillis()
        val updated = entity.copy(
            snoozeUntilEpochMs = null,
            updatedAtEpochMs = now
        )
        taskDao.updateTask(updated)

        val cat = updated.categoryId?.let { categoryDao.getCategoryById(it)?.toDomain() }
        return updated.toDomain(cat)
    }

    override suspend fun getTasksForDailyReview(today: LocalDate): List<Task> {
        val todayEpochDay = today.toEpochDay()
        val startOfDayMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDayMs = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val entities = taskDao.getTasksForDailyReview(
            todayEpochDay = todayEpochDay,
            startOfDayEpochMs = startOfDayMs,
            endOfDayEpochMs = endOfDayMs
        )
        val categoriesMap = categoryDao.getAllCategories().associate { it.id to it.toDomain() }
        return mapEntitiesWithCategories(entities, categoriesMap)
    }
}
