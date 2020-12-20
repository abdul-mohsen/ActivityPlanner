package com.bignerdranch.android.activityplanner.database

import androidx.room.*
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {

    @Query("SELECT * FROM business_table")
    fun getAll(): Flow<List<Business>>

    @Update
    suspend fun update(vararg business: Business)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg business: Business)

    @Query("DELETE FROM business_table")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(vararg business: Business)
}