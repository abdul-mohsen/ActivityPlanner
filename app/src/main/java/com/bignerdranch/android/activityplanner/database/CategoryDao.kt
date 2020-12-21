package com.bignerdranch.android.activityplanner.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import com.bignerdranch.android.activityplanner.model.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category_table")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM category_table WHERE categoryId IN (:ids)")
    suspend fun getById( ids: List<Long>): List<Category>

    @Update
    suspend fun update(vararg category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg category: Category)

    @Delete
    suspend fun delete(vararg category: Category)

    @Query("DELETE FROM category_table")
    suspend fun deleteAll()
}