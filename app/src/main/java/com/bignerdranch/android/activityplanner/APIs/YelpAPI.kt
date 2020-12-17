package com.bignerdranch.android.activityplanner.APIs

import com.bignerdranch.android.activityplanner.model.Business
import retrofit2.http.GET
import retrofit2.http.Query

interface YelpAPI {

    @GET("businesses/search")
    suspend fun searchBusinesses(
        @Query("term") term: String = "",
//        @Query("location") location: String = "",
        @Query("latitude") latitude: Double = 50.5,
        @Query("longitude") longitude: Double = 50.3,
        @Query("radius") radius: Int = 30,
        @Query("categories") categories: String ="",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("sort_by") sortBy: String = "best_match",
        @Query("price") price: String = "1,2,3,4",
        @Query("open_at") openAt: String = "",
        @Query("attributes") attributes: String = "reservation" +
                "waitlist_reservation, open_to_all, wheelchair_accessible"
    ): Array<Business>
}
