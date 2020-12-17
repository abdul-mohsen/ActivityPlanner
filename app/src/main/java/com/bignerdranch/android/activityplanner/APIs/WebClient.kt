package com.bignerdranch.android.activityplanner.APIs

import com.bignerdranch.android.activityplanner.BuildConfig
import com.bignerdranch.android.activityplanner.model.Business
import com.bignerdranch.android.activityplanner.model.Weather
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

object WebClient {

    private const val YELP_BASE_URL = "https://api.yelp.com/v3/"
    private const val WEATHER_BASE_URL = "https://api.weatherapi.com/v1/"
    private const val YELP_API_KEY = BuildConfig.YELP_API_KEY
    private const val WEATHER_API_KEY = BuildConfig.WEATHER_API_KEY

    private val yelpDeserializer = JsonDeserializer { json, _, _ ->
        json as JsonObject
        val longList = json.getAsJsonArray("businesses")
        Gson().fromJson(longList, Array<Business>::class.java).apply {
            Timber.d("$this")
        }
    }

    private val weatherDeserializer = JsonDeserializer { json, _, _ ->
        json as JsonObject
        val forecast = json.get("forecast") as JsonObject
        val forecastday = forecast.getAsJsonArray("forecastday")
        val list: MutableList<Weather> = mutableListOf()
        for (item in forecastday) {
            print(forecastday.size())
            val hours = (item as JsonObject).getAsJsonArray("hour")
            list.addAll(Gson().fromJson(hours, Array<Weather>::class.java).toList().apply {
                println(this)
            })
        }
        list.toTypedArray()
    }

    private val yelpInterceptor = Interceptor{ chain ->
        chain.proceed(
            chain.request().newBuilder().addHeader("Authorization", "Bearer $YELP_API_KEY")
                .url(
                    chain.request().url.newBuilder()
                        .build()
                ).build()
        ).apply { Timber.d( this.request.toString() ) }
    }

    private val weatherInterceptor = Interceptor{ chain ->
        chain.proceed(
            chain.request().newBuilder().url(
                chain.request().url.newBuilder()
                    .addQueryParameter("key", WEATHER_API_KEY)
                    .build()
            ).build()
        ).apply { Timber.d( this.request.toString() ) }
    }

    val yelpAPI: YelpAPI = Retrofit.Builder().baseUrl(YELP_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder().registerTypeAdapter(Array<Business>::class.java, yelpDeserializer).create()
        ))
        .client(OkHttpClient.Builder().addInterceptor(yelpInterceptor).build())
        .build()
        .create(YelpAPI::class.java)

    val weatherAPI: WeatherAPI = Retrofit.Builder().baseUrl(WEATHER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder().registerTypeAdapter(Array<Weather>::class.java, weatherDeserializer).create()
        ))
        .client(OkHttpClient.Builder().addInterceptor(weatherInterceptor).build())
        .build()
        .create(WeatherAPI::class.java)
}
