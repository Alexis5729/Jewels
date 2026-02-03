package com.example.jewels.data.local.db

import androidx.room.TypeConverter
import com.example.jewels.data.local.entity.ProductStatus

class Converters {

    @TypeConverter
    fun fromStatus(status: ProductStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ProductStatus = ProductStatus.valueOf(value)
}
