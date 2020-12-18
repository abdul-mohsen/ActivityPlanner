package com.bignerdranch.android.activityplanner.model

import com.google.gson.annotations.SerializedName

data class Business(
    val id: String,
    @SerializedName("is_claimed")
    var isClaimed: Boolean,
    @SerializedName("is_closed")
    var isClosed: Boolean,
    @SerializedName("review_count")
    var reviewCount: Int,
    var rating: Float,
    var distance: Float
    ) {
    lateinit var name: String
    lateinit var alias: String
    lateinit var url: String
    lateinit var phone: String
    lateinit var price: String
    lateinit var categories: List<Map<String, String>>
    lateinit var coordinates: Coordinates
    lateinit var weatherTimeMap : MutableMap<Int, Weather>

    data class Coordinates(val latitude: Double, val longitude: Double,)
}
