package com.localtaskmanager.app.di

import android.content.Context
import com.localtaskmanager.app.data.export.DataBackupManager
import com.localtaskmanager.app.data.local.database.AppDatabase
import com.localtaskmanager.app.data.repository.CategoryRepositoryImpl
import com.localtaskmanager.app.data.repository.PreferencesRepositoryImpl
import com.localtaskmanager.app.data.repository.TaskRepositoryImpl
import com.localtaskmanager.app.domain.repository.CategoryRepository
import com.localtaskmanager.app.domain.repository.PreferencesRepository
import com.localtaskmanager.app.domain.repository.TaskRepository
import com.localtaskmanager.app.domain.usecase.CompleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.CreateTaskUseCase
import com.localtaskmanager.app.domain.usecase.DailyReviewUseCase
import com.localtaskmanager.app.domain.usecase.DeleteTaskUseCase
import com.localtaskmanager.app.domain.usecase.ReopenTaskUseCase
import com.localtaskmanager.app.domain.usecase.RescheduleReminderUseCase
import com.localtaskmanager.app.domain.usecase.UpdateTaskUseCase

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(
            database = database,
            taskDao = database.taskDao(),
            categoryDao = database.categoryDao(),
            recurrenceSeriesDao = database.recurrenceSeriesDao()
        )
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(
            categoryDao = database.categoryDao(),
            taskDao = database.taskDao()
        )
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(context)
    }

    val dataBackupManager: DataBackupManager by lazy {
        DataBackupManager(
            database = database,
            taskDao = database.taskDao(),
            categoryDao = database.categoryDao(),
            recurrenceSeriesDao = database.recurrenceSeriesDao()
        )
    }

    // Use cases
    val createTaskUseCase by lazy { CreateTaskUseCase(taskRepository) }
    val updateTaskUseCase by lazy { UpdateTaskUseCase(taskRepository) }
    val deleteTaskUseCase by lazy { DeleteTaskUseCase(taskRepository) }
    val completeTaskUseCase by lazy { CompleteTaskUseCase(taskRepository) }
    val reopenTaskUseCase by lazy { ReopenTaskUseCase(taskRepository) }
    val rescheduleReminderUseCase by lazy { RescheduleReminderUseCase(taskRepository) }
    val dailyReviewUseCase by lazy { DailyReviewUseCase(taskRepository) }
}
