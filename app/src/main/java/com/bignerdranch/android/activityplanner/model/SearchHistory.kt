package com.bignerdranch.android.activityplanner.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "search_history_table")
data class SearchHistory (
    @PrimaryKey(autoGenerate = true) val id:Long = 0,
    val query: String,
    val date: Long = Date().time
)