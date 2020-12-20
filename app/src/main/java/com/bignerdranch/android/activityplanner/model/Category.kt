package com.bignerdranch.android.activityplanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "category_table")
data class Category(
    @ColumnInfo(name = "categoryId", index = true) @PrimaryKey(autoGenerate = true) val id: Long,
    @SerializedName("title") val name: String = ""
)