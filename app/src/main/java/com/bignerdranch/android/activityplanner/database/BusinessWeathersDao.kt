package com.bignerdranch.android.activityplanner.database

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Query
import com.bignerdranch.android.activityplanner.model.BusinessWithWeathers
import com.bignerdranch.android.activityplanner.model.Weather
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessWeathersDao {
    @Transaction
    @Query("SELECT * FROM business_table")
    fun getAll(): Flow<List<BusinessWithWeathers>>

    @Transaction
    @Query("SELECT * FROM business_table Where businessId = (:id)")
    suspend fun getByBusinessId(id: String): BusinessWithWeathers

}
