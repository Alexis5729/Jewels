package com.example.jewels.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interests",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId")]
)
data class InterestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: Long,
    val buyerName: String,
    val phone: String,
    val note: String = "",
    val status: InterestStatus = InterestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class InterestStatus { PENDING, CONTACTED, CLOSED }
