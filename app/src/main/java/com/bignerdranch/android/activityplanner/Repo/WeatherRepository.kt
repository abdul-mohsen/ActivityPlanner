package com.bignerdranch.android.activityplanner.Repo

import com.bignerdranch.android.activityplanner.APIs.WebClient
import com.bignerdranch.android.activityplanner.model.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.Exception

// make the id of the location with the List of weather
typealias weatherUnion = Pair<String, List<Weather>>

object WeatherRepository {
    private val webClient = WebClient.weatherAPI
    private val dispatcher = Dispatchers.IO

    @FlowPreview
    suspend fun getWeather(
        locationList :List<Pair<Double, Double>>,
        id: String
    ): Flow<weatherUnion> = locationList.asFlow().flatMapMerge(concurrency = 4) { location ->
        flow {
            val weatherList = webClient.getWeatherAtLocation(
                query= "${location.first},${location.second}"
            ).toList()
            Timber.d("Got a response with a list of size ${weatherList.size}")
            emit(id to weatherList)
        }
    }.retry(1) { e ->
        (e is Exception).also { if (it) delay(1000) }
    }.catch { e ->
        Timber.d(e.toString())
    }.flowOn(dispatcher)
}