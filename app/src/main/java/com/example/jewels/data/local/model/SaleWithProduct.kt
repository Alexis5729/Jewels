package com.example.jewels.data.local.model

import androidx.room.ColumnInfo

data class SaleWithProduct(
    val id: Long,
    val productId: Long,
    val interestId: Long?,              // ✅ NUEVO (puede ser null)
    @ColumnInfo(name = "productName") val productName: String,
    val buyerName: String,
    val phone: String,
    val note: String,
    val priceClp: Int,
    val qty: Int,                       // ✅ recomendado (para devolver stock real)
    val createdAt: Long
)
