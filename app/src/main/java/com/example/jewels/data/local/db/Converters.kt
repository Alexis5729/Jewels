package com.example.jewels.data.local.db

import androidx.room.TypeConverter
import com.example.jewels.data.local.entity.ProductStatus
import com.example.jewels.data.local.entity.InterestStatus


class Converters {

    @TypeConverter
    fun fromStatus(status: ProductStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ProductStatus = ProductStatus.valueOf(value)

    @TypeConverter
    fun fromInterestStatus(status: InterestStatus): String = status.name

    @TypeConverter
    fun toInterestStatus(value: String): InterestStatus = InterestStatus.valueOf(value)
}
