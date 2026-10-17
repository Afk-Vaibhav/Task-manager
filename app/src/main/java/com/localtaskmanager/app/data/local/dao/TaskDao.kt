package com.localtaskmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.localtaskmanager.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks ORDER BY dueDateEpochDay ASC, scheduledMinuteOfDay ASC, createdAtEpochMs DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'OPEN' ORDER BY dueDateEpochDay ASC, scheduledMinuteOfDay ASC, priority DESC")
    fun getOpenTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDateEpochDay = :epochDay ORDER BY scheduledMinuteOfDay ASC, priority DESC")
    fun getTasksForDateFlow(epochDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDateEpochDay BETWEEN :startDay AND :endDay ORDER BY dueDateEpochDay ASC, scheduledMinuteOfDay ASC")
    fun getTasksForDateRangeFlow(startDay: Long, endDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtEpochMs DESC")
    suspend fun getAllTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'OPEN'")
    suspend fun getOpenTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND (reminderEnabled = 1 OR snoozeUntilEpochMs IS NOT NULL)")
    suspend fun getTasksWithActiveReminders(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE recurrenceSeriesId = :seriesId AND dueDateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getTaskBySeriesAndDate(seriesId: String, dateEpochDay: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE recurrenceSeriesId = :seriesId ORDER BY dueDateEpochDay DESC LIMIT 1")
    suspend fun findLatestOccurrenceForSeries(seriesId: String): TaskEntity?

    @Query("UPDATE tasks SET categoryId = :toCategoryId WHERE categoryId = :fromCategoryId")
    suspend fun reassignCategory(fromCategoryId: String, toCategoryId: String)

    @Query("SELECT * FROM tasks WHERE status = 'OPEN' AND (dueDateEpochDay <= :todayEpochDay OR (dueDateEpochDay IS NULL AND createdAtEpochMs BETWEEN :startOfDayEpochMs AND :endOfDayEpochMs))")
    suspend fun getTasksForDailyReview(todayEpochDay: Long, startOfDayEpochMs: Long, endOfDayEpochMs: Long): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}
