package com.bignerdranch.android.activityplanner.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.OnConflictStrategy
import com.bignerdranch.android.activityplanner.model.BusinessCategory
import com.bignerdranch.android.activityplanner.model.BusinessWithCategories
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessCategoriesDao {
    @Transaction
    @Query("SELECT * FROM business_table")
    fun getAll(): Flow<List<BusinessWithCategories>>

    @Transaction
    @Query("SELECT * FROM business_table Where businessId in (:ids)")
    suspend fun getByBusinessesId(ids: List<String>): List<BusinessWithCategories>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg x: BusinessCategory)

    @Query("DELETE FROM business_cross_category")
    suspend fun deleteAll()
}