package com.bignerdranch.android.activityplanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "business_cross_category",
    primaryKeys = ["businessId", "categoryId"]
)
data class BusinessCategory(
     val businessId: String,
     @ColumnInfo(name = "categoryId", index = true) val categoryId: Int
)