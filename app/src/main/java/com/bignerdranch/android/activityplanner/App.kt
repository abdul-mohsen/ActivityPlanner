package com.bignerdranch.android.activityplanner

import android.app.Application
import com.bignerdranch.android.activityplanner.Repo.BusinessRepository
import com.bignerdranch.android.activityplanner.Repo.SearchHistoryRepository
import com.bignerdranch.android.activityplanner.Repo.WeatherRepository
import com.bignerdranch.android.activityplanner.database.AppDatabase
import com.squareup.picasso.Picasso
import timber.log.Timber

class App: Application() {
    override fun onCreate() {

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        AppDatabase.invoke(this).also {
            BusinessRepository.businessDao = it.businessDao()
            BusinessRepository.categoryDao = it.categoryDao()
            BusinessRepository.businessCategoriesDao = it.businessWithCategoriesDao()
            WeatherRepository.businessWeatherDao = it.businessWeatherDao()
            WeatherRepository.weatherDao = it.weatherDao()
            SearchHistoryRepository.searchHistoryDao = it.searchHistoryDao()
        }

        Picasso.get().setIndicatorsEnabled(true)

        super.onCreate()
    }
}
