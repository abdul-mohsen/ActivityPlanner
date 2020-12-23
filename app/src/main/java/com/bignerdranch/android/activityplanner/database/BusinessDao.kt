package com.bignerdranch.android.activityplanner.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.bignerdranch.android.activityplanner.model.Business
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {

    @Query("SELECT * FROM business_table")
    fun getAll(): Flow<List<Business>>

    @Query("""
        SELECT * FROM business_table WHERE (latitude - (:latitude)) * (latitude - (:latitude)) +
        (longitude - (:longitude)) * (longitude - (:longitude)) * (:x) < 10
        
    """)
    suspend fun getAllByDistance(latitude: Double, longitude: Double, x: Double): List<Business>

    @Update
    suspend fun update(vararg business: Business)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg business: Business)

    @Query("DELETE FROM business_table")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(vararg business: Business)
}