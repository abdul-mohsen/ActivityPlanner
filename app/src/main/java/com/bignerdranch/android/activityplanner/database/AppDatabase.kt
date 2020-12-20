package com.bignerdranch.android.activityplanner.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.BusinessCategory
import com.bignerdranch.android.activityplanner.model.Category

@Database(entities = [Business::class, Category::class, BusinessCategory::class], version = 1, exportSchema = false)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun categoryDao(): CategoryDao
    abstract fun businessWithCategoriesDao(): BusinessCategoriesDao

    companion object {
        private const val DATABASE_NAME = "database"
        @Volatile
        private var INSTANCE: AppDatabase? = null
        operator fun invoke(context: Context) = INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
    }
}
