package com.localtaskmanager.app.data.export

import androidx.room.withTransaction
import com.localtaskmanager.app.data.local.dao.CategoryDao
import com.localtaskmanager.app.data.local.dao.RecurrenceSeriesDao
import com.localtaskmanager.app.data.local.dao.TaskDao
import com.localtaskmanager.app.data.local.database.AppDatabase
import com.localtaskmanager.app.data.local.entity.CategoryEntity
import com.localtaskmanager.app.data.local.entity.RecurrenceSeriesEntity
import com.localtaskmanager.app.data.local.entity.TaskEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val categories: List<CategoryEntity>,
    val tasks: List<TaskEntity>,
    val recurrenceSeries: List<RecurrenceSeriesEntity> = emptyList()
)

enum class ImportMode {
    MERGE,
    REPLACE
}

class DataBackupManager(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val recurrenceSeriesDao: RecurrenceSeriesDao
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportData(outputStream: OutputStream): Result<Int> {
        return runCatching {
            val categories = categoryDao.getAllCategories()
            val tasks = taskDao.getAllTasksSnapshot()
            val series = recurrenceSeriesDao.getAllSeriesSnapshot()

            val payload = BackupPayload(
                version = 1,
                exportedAtEpochMs = System.currentTimeMillis(),
                categories = categories,
                tasks = tasks,
                recurrenceSeries = series
            )

            val jsonString = json.encodeToString(payload)
            outputStream.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            tasks.size
        }
    }

    suspend fun importData(inputStream: InputStream, mode: ImportMode): Result<Int> {
        return runCatching {
            val jsonString = inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
            val payload = json.decodeFromString<BackupPayload>(jsonString)

            require(payload.version == 1) { "Unsupported backup schema version: ${payload.version}" }

            // Validate categories
            for (cat in payload.categories) {
                require(cat.name.isNotBlank()) { "Category name in backup cannot be blank" }
            }

            // Validate tasks
            for (task in payload.tasks) {
                require(task.title.isNotBlank()) { "Task title in backup cannot be blank" }
            }

            database.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    taskDao.clearAllTasks()
                    categoryDao.clearAllCategories()
                    recurrenceSeriesDao.clearAllSeries()

                    categoryDao.insertCategories(payload.categories)
                    recurrenceSeriesDao.insertAllSeries(payload.recurrenceSeries)
                    taskDao.insertTasks(payload.tasks)
                } else {
                    // MERGE: Keep existing, insert non-existent or newer
                    val existingCategories = categoryDao.getAllCategories().associateBy { it.id }
                    val categoriesToInsert = mutableListOf<CategoryEntity>()
                    for (cat in payload.categories) {
                        if (!existingCategories.containsKey(cat.id)) {
                            // ensure unique name
                            val nameMatch = categoryDao.getCategoryByName(cat.name)
                            if (nameMatch == null) {
                                categoriesToInsert.add(cat)
                            }
                        }
                    }
                    if (categoriesToInsert.isNotEmpty()) {
                        categoryDao.insertCategories(categoriesToInsert)
                    }

                    if (payload.recurrenceSeries.isNotEmpty()) {
                        recurrenceSeriesDao.insertAllSeries(payload.recurrenceSeries)
                    }

                    taskDao.insertTasks(payload.tasks)
                }
            }
            payload.tasks.size
        }
    }
}
