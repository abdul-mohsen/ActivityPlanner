package com.bignerdranch.android.activityplanner.Repo

import com.bignerdranch.android.activityplanner.database.SearchHistoryDao
import com.bignerdranch.android.activityplanner.model.SearchHistory

object SearchHistoryRepository {
    lateinit var searchHistoryDao: SearchHistoryDao
    val allSearchHistory by lazy { searchHistoryDao.getALL() }

    suspend fun insert(searchHistory: SearchHistory) {
        searchHistoryDao.insert(searchHistory)
    }
}