package com.bignerdranch.android.activityplanner.model

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "weather_table",
    primaryKeys = ["timeEpoch", "businessId"]
)
data class Weather(
    @SerializedName("time_epoch")
    val timeEpoch: Int,
    @SerializedName("temp_c")
    var tempC: Float,
    @SerializedName("is_day")
    var isDay: Int,
    var will_it_rain: Int,
    var will_it_snow: Int
){
    lateinit var businessId: String
    lateinit var time: String
    @Embedded
    lateinit var condition: Condition
    @SerializedName("chance_of_rain")
    lateinit var rainChance: String
    @SerializedName("chance_of_snow")
    lateinit var snowChance: String

    data class Condition(val text:String, val icon:String)
}
