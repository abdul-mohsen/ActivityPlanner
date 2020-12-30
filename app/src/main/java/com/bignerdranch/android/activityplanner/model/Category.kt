package com.bignerdranch.android.activityplanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "category_table")
data class Category(
    @SerializedName("title") val name: String,
    @ColumnInfo(name = "categoryId", index = true)
    @PrimaryKey val id: Int = name.hashCode()
)