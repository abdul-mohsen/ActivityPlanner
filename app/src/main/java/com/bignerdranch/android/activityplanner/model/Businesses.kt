package com.bignerdranch.android.activityplanner.model

import com.google.gson.annotations.SerializedName

data class Businesses(
    @SerializedName("businesses")
    val businessList: List<Business>,
)