package com.bignerdranch.android.activityplanner.flickrAPI

import retrofit2.http.GET

interface WeatherAPI {

    @GET("")
    suspend fun getWeather(): String
}
