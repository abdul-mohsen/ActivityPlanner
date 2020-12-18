package com.bignerdranch.android.activityplanner.APIs

import com.bignerdranch.android.activityplanner.model.AutoComplete
import com.bignerdranch.android.activityplanner.model.Business
import retrofit2.http.GET
import retrofit2.http.Query

interface YelpAPI {

    @GET("businesses/search")
    suspend fun searchBusinesses(
        @Query("term") term: String = "",
        @Query("location") location: String = "null",
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = YelpAPI.radius,
        @Query("categories") categories: String ="",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("sort_by") sortBy: String = "best_match",
        @Query("price") price: String = "1,2,3,4",
        @Query("open_at") openAt: String = "",
        @Query("attributes") attributes: String = extraData
    ): Array<Business>

    @GET("autocomplete")
    suspend fun autoComplete(
        @Query("text") text: String,
        @Query("latitude") latitude: Double = newYorkLocation.latitude,
        @Query("longitude") longitude: Double = newYorkLocation.longitude,
    ): AutoComplete

    companion object {
        private const val radius = 15000
        private val newYorkLocation: Business.Coordinates =
            Business.Coordinates(40.773326878163, -73.9113807678223)
        private const val extraData = "reservation, waitlist_reservation, open_to_all, wheelchair_accessible"
    }
}
