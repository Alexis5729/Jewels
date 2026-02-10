package com.example.jewels.data.local.model

import androidx.room.ColumnInfo

data class SaleWithProduct(
    val id: Long,
    val productId: Long,
    @ColumnInfo(name = "productName") val productName: String,
    val buyerName: String,
    val phone: String,
    val note: String,
    val priceClp: Int,
    val createdAt: Long
)
