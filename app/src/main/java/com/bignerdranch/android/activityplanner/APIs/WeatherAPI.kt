package com.bignerdranch.android.activityplanner.APIs

import com.bignerdranch.android.activityplanner.model.Weather
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPI {

    @GET("forecast.json")
    suspend fun getWeatherAtLocation(
        @Query("key") key: String = "",
        @Query("q") query: String = "",
        @Query("hour") hour: String = "",
        @Query("days") term: String = "1"
    ): Array<Weather>
}
