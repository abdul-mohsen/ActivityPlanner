package com.bignerdranch.android.activityplanner.model

import androidx.room.Embedded
import androidx.room.Relation


data class BusinessWithWeathers (
    @Embedded val business: Business,
    @Relation(
        parentColumn = "businessId",
        entity = Weather::class,
        entityColumn = "businessId"
    )
    val weathers: List<Weather>
)