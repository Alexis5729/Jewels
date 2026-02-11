package com.example.jewels.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val interestId: Long? = null,
    val buyerName: String,
    val phone: String,
    val note: String = "",
    val priceClp: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val qty: Int
)
