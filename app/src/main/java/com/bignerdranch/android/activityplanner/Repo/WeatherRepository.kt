package com.bignerdranch.android.activityplanner.Repo

import android.text.format.DateFormat
import androidx.lifecycle.viewModelScope
import com.bignerdranch.android.activityplanner.APIs.WebClient
import com.bignerdranch.android.activityplanner.database.BusinessCategoriesDao
import com.bignerdranch.android.activityplanner.database.BusinessWeathersDao
import com.bignerdranch.android.activityplanner.database.WeatherDao
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.DataState
import com.bignerdranch.android.activityplanner.model.Weather
import com.bignerdranch.android.activityplanner.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception
import java.util.*

object WeatherRepository {
    private val webClient = WebClient.weatherAPI
    private val dispatcher = Dispatchers.IO
    lateinit var weatherDao:  WeatherDao
    lateinit var businessWeatherDao: BusinessWeathersDao

    fun allWeatherByBusinessIdAndDate(businesses: List<Business>, fullDate: String, date: String) = flow {
        var firstIn = true
        weatherDao.getByBusinessIdAndDate(businesses.map { it.id }, fullDate).collect { list ->
            emit(list)
            if (firstIn) {
                val businessesNeedInternet = businesses.filter { business -> business.id !in list.map { it.businessId } }

                if (businessesNeedInternet.isNotEmpty()) {
                    loadWeather(businessesNeedInternet, date)
                    firstIn = false
                }
            }
        }
    }


    @FlowPreview
    private suspend fun loadWeather(businesses: List<Business>, date: String) {
        getWeather(businesses, date).collect{ list ->
            if (list.isNotEmpty()) weatherDao.insert(*list.toTypedArray())
        }
    }
    
    @FlowPreview
    suspend fun getWeather(
        businessList :List<Business>,
        startDate: String
    ): Flow<List<Weather>> = businessList.asFlow().flatMapMerge(concurrency = 4) { business ->
        flow {
            val weatherList: List<Weather> = webClient.getWeatherAtLocation(
                query = "${business.coordinates.latitude},${business.coordinates.longitude}",
                startDate = startDate
                ).toList().also { list ->
                    list.map { weather -> weather.businessId = business.id }
                    Timber.d("This is from the internet")
                }
            Timber.d("Got a response with a list of size ${weatherList.size}")
            emit(weatherList)
        }
    }.retry(1) { e ->
        (e is Exception).also { if (it) delay(1000) }
    }.catch { e ->
        Timber.d(e.toString())
    }.flowOn(dispatcher)
}
