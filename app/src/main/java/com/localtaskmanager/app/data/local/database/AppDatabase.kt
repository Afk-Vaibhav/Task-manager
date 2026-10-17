package com.localtaskmanager.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.localtaskmanager.app.data.local.dao.CategoryDao
import com.localtaskmanager.app.data.local.dao.RecurrenceSeriesDao
import com.localtaskmanager.app.data.local.dao.TaskDao
import com.localtaskmanager.app.data.local.entity.CategoryEntity
import com.localtaskmanager.app.data.local.entity.RecurrenceSeriesEntity
import com.localtaskmanager.app.data.local.entity.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        RecurrenceSeriesEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurrenceSeriesDao(): RecurrenceSeriesDao

    companion object {
        const val DATABASE_NAME = "local_tasks.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(DatabaseCallback())
                .build()
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(id = "cat_work", name = "Work", iconKey = "work", colorArgb = 0xFF1E88E5.toInt(), isDefault = false),
            CategoryEntity(id = "cat_grocery", name = "Grocery", iconKey = "grocery", colorArgb = 0xFF43A047.toInt(), isDefault = false),
            CategoryEntity(id = "cat_important", name = "Important", iconKey = "important", colorArgb = 0xFFE53935.toInt(), isDefault = false),
            CategoryEntity(id = "cat_calls", name = "Calls", iconKey = "calls", colorArgb = 0xFF8E24AA.toInt(), isDefault = false),
            CategoryEntity(id = "cat_study", name = "Study", iconKey = "study", colorArgb = 0xFFFB8C00.toInt(), isDefault = false),
            CategoryEntity(id = "cat_personal", name = "Personal", iconKey = "personal", colorArgb = 0xFF00897B.toInt(), isDefault = true),
            CategoryEntity(id = "cat_payments", name = "Payments", iconKey = "payments", colorArgb = 0xFF3949AB.toInt(), isDefault = false),
            CategoryEntity(id = "cat_other", name = "Other", iconKey = "other", colorArgb = 0xFF546E7A.toInt(), isDefault = false)
        )

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        database.categoryDao().insertCategories(DEFAULT_CATEGORIES)
                    }
                }
            }
        }
    }
}
