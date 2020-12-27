package com.bignerdranch.android.activityplanner.Repo

import com.bignerdranch.android.activityplanner.APIs.WebClient
import com.bignerdranch.android.activityplanner.database.BusinessCategoriesDao
import com.bignerdranch.android.activityplanner.database.BusinessWeathersDao
import com.bignerdranch.android.activityplanner.database.WeatherDao
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception

object WeatherRepository {
    private val webClient = WebClient.weatherAPI
    private val dispatcher = Dispatchers.IO
    lateinit var weatherDao:  WeatherDao
    lateinit var businessWeatherDao: BusinessWeathersDao

    suspend fun allWeatherByBusinessIdAndDate(ids: List<String>, date: String) =
        weatherDao.getByBusinessIdAndDate(
            ids,
            date
        )
    
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
                    weatherDao.insert(*list.toTypedArray())
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
