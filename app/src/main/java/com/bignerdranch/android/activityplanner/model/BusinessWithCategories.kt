package com.bignerdranch.android.activityplanner.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class BusinessWithCategories(
    @Embedded val business: Business,
    @Relation(
        parentColumn = "businessId",
        entityColumn = "categoryId",
        associateBy = Junction(BusinessCategory::class)
    )
    val categories: List<BusinessCategory>
)