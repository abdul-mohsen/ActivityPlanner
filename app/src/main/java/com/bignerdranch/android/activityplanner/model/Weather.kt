package com.bignerdranch.android.activityplanner.model

import com.google.gson.annotations.SerializedName

data class Weather(
    val time_epoch: Int,
    @SerializedName("temp_c")
    var tempC: Float,
    @SerializedName("is_day")
    var isDay: Int,
    private var will_it_rain: Int,
    private var will_it_snow: Int
){
    lateinit var time: String
    lateinit var condition: Map<String, String>
    @SerializedName("chance_of_rain")
    lateinit var rainChance: String
    @SerializedName("chance_of_snow")
    lateinit var snowChance: String
    val willRain
        get() = will_it_rain == 1
    val willSnow
        get() = will_it_snow == 1
}