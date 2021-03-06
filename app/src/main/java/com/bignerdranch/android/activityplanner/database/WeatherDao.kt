package com.bignerdranch.android.activityplanner.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.bignerdranch.android.activityplanner.model.Weather
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_table")
    suspend fun getAll(): List<Weather>

    @Query("SELECT * FROM weather_table WHERE businessId IN (:ids) AND time = (:date)")
    fun getByBusinessIdAndDate(ids: List<String>, date: String): Flow<List<Weather>>

    @Update
    suspend fun update(vararg weather: Weather)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg weather: Weather)

    @Delete
    suspend fun delete(vararg weather: Weather)

    @Query("DELETE FROM weather_table")
    suspend fun deleteAll()
}