package com.bignerdranch.android.activityplanner.model

import android.location.Location

data class Business(
    val id: String,
    var is_claimed: Boolean,
    var is_closed: Boolean,
    var review_count: Int,
    var rating: Float,
    var distance: Float
    ) {
    lateinit var name: String
    lateinit var alias: String
    lateinit var url: String
    lateinit var phone: String
    lateinit var price: String
    lateinit var categories: List<Map<String, String>>
    lateinit var location: Location
    lateinit var coordinates: Map<String, Double>
    val weatherTimeMap : MutableMap<Int, Weather> = mutableMapOf()
}
