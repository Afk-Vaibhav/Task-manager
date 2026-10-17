package com.localtaskmanager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.localtaskmanager.app.data.local.entity.RecurrenceSeriesEntity

@Dao
interface RecurrenceSeriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: RecurrenceSeriesEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSeries(seriesList: List<RecurrenceSeriesEntity>)

    @Update
    suspend fun updateSeries(series: RecurrenceSeriesEntity)

    @Delete
    suspend fun deleteSeries(series: RecurrenceSeriesEntity)

    @Query("SELECT * FROM recurrence_series WHERE id = :id LIMIT 1")
    suspend fun getSeriesById(id: String): RecurrenceSeriesEntity?

    @Query("SELECT * FROM recurrence_series WHERE isActive = 1")
    suspend fun getAllActiveSeries(): List<RecurrenceSeriesEntity>

    @Query("SELECT * FROM recurrence_series")
    suspend fun getAllSeriesSnapshot(): List<RecurrenceSeriesEntity>

    @Query("DELETE FROM recurrence_series")
    suspend fun clearAllSeries()
}
