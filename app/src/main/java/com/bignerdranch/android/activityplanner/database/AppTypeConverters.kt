package com.bignerdranch.android.activityplanner.database

import androidx.room.TypeConverter
import java.util.*

class AppTypeConverters {

    @TypeConverter
    fun toUUID(uuid: String?): UUID? = UUID.fromString(uuid)

    @TypeConverter
    fun fromUUID(uuid: UUID?): String? = uuid?.toString()

}