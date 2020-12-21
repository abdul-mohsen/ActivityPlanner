package com.bignerdranch.android.activityplanner.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "weather_table")
data class Weather(
    @ColumnInfo(name = "weatherId")@PrimaryKey(autoGenerate = true) val id: Long,
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
    @Ignore
    lateinit var condition: Map<String, String>
    @SerializedName("chance_of_rain")
    lateinit var rainChance: String
    @SerializedName("chance_of_snow")
    lateinit var snowChance: String
}
