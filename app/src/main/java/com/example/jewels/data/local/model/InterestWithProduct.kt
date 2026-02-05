package com.example.jewels.data.local.model

import com.example.jewels.data.local.entity.InterestStatus

data class InterestWithProduct(
    val id: Long,
    val productId: Long,
    val productName: String,
    val buyerName: String,
    val phone: String,
    val note: String,
    val status: InterestStatus,
    val createdAt: Long
)
