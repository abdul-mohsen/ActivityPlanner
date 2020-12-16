package com.bignerdranch.android.activityplanner.flickrAPI

import com.bignerdranch.android.activityplanner.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WebClient {

    private const val YELP_BASE_URL = "https://api.yelp.com/v3"
    private const val WEATHER_BASE_URL = "http://api.weatherapi.com/v1"
    private const val YELP_API_KEY = BuildConfig.YELP_API_KEY

    private val yelpInterceptor = Interceptor{ chain ->
        chain.proceed(
            chain.request().newBuilder().addHeader("Authorization", "Bearer $YELP_API_KEY")
                .url(
                    chain.request().url.newBuilder().build()
                ).build()
        )
    }

    private val weatherInterceptor = Interceptor{ chain ->
        chain.proceed(
            chain.request().newBuilder().url(
                chain.request().url.newBuilder()
                    .build()
            ).build()
        )
    }

    val yelpAPI: YelpAPI = Retrofit.Builder().baseUrl(YELP_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().addInterceptor(yelpInterceptor).build())
        .build()
        .create(YelpAPI::class.java)

    val weatherAPI: WeatherAPI = Retrofit.Builder().baseUrl(WEATHER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(OkHttpClient.Builder().addInterceptor(weatherInterceptor).build())
        .build()
        .create(WeatherAPI::class.java)
}
