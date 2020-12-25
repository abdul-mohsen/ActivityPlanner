package com.bignerdranch.android.activityplanner.model

import androidx.room.*
import com.google.gson.annotations.SerializedName

@Entity(tableName = "business_table")
data class Business(
        @ColumnInfo(name = "businessId") @PrimaryKey val id: String,
        @SerializedName("is_claimed")
        var isClaimed: Boolean,
        @SerializedName("is_closed")
        var isClosed: Boolean,
        @SerializedName("review_count")
        var reviewCount: Int,
        var rating: Float
    ) {
    lateinit var name: String
    lateinit var alias: String
    lateinit var phone: String
    lateinit var price: String
    lateinit var image_url: String
    lateinit var url: String

    @Embedded
    lateinit var coordinates: Coordinates
    @Ignore
    var weatherTimeMap : MutableMap<Int, Weather> = mutableMapOf()
    @Ignore
    var categories: List<Category> = listOf()

    data class Coordinates(val latitude: Double, val longitude: Double)
}
