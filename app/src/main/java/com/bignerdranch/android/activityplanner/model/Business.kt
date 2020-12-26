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
        var rating: Float,
        @Ignore
        var weather: Weather? = null
    ) {
    lateinit var name: String
    lateinit var alias: String
    lateinit var phone: String
    lateinit var price: String
    @SerializedName("image_url")
    lateinit var imageUrl: String
    lateinit var url: String

    @Embedded
    lateinit var coordinates: Coordinates
    @Ignore
    var categories: List<Category> = listOf()

    data class Coordinates(val latitude: Double, val longitude: Double)
    constructor(id: String, isClaimed: Boolean, isClosed: Boolean, reviewCount: Int, rating: Float):
            this(id, isClaimed, isClosed, reviewCount, rating, null)
}
