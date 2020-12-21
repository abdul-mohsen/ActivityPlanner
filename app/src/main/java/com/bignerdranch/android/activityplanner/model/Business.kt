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

        var url: String
    ) {
    lateinit var name: String
    lateinit var alias: String
    lateinit var phone: String
    lateinit var price: String

    @Embedded
    lateinit var coordinates: Coordinates
    @Ignore
    lateinit var weatherTimeMap : MutableMap<Int, Weather>
    @Ignore
    lateinit var categories: List<Category>

    data class Coordinates(val latitude: Double, val longitude: Double)
}
